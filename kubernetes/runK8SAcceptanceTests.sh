#!/bin/bash

# Following env vars needs to be defined
#KUBECONFIG=/path/to/cluster/kubeconfig
#CLUSTER_NAME=the-cluster-name

pushd () {
    command pushd "$@" > /dev/null
}

popd () {
    command popd "$@" > /dev/null
}

function wait_for_200 {
  local READY_FOR_TESTS=1
  for i in $( seq 1 "${RETRIES}" ); do
    STATUS=$(curl -s -o /dev/null -w '%{http_code}' ${1})
    if [ $STATUS -eq 200 ]; then
      READY_FOR_TESTS=0
      break
    else
      echo "Failed to connect to ${1} with status code: $STATUS. Attempt  #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds" >&2
      sleep "${WAIT_TIME}"
    fi
  done
  return ${READY_FOR_TESTS}
}

function prepare_ticktock_latest_with_kafka_binder() {
    kubectl create -f k8s-templates/ticktock/time.yaml
    kubectl create -f k8s-templates/ticktock/time-svc-lb.yaml

    kubectl create -f k8s-templates/ticktock/log.yaml
    kubectl create -f k8s-templates/ticktock/log-svc-lb.yaml

    TIME_SOURCE_SERVER_URI=http://$(kubectl get service time | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG_SINK_SERVER_URI=http://$(kubectl get service log | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(wait_for_200 ${TIME_SOURCE_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG_SINK_SERVER_URI}/actuator/logfile)
}

function prepare_http_transform_log_with_kafka_binder() {

    kubectl create -f k8s-templates/http-transfomer-log/http.yaml
    kubectl create -f k8s-templates/http-transfomer-log/http-svc.yaml
    kubectl create -f k8s-templates/http-transfomer-log/transform-processor-kafka.yaml
    kubectl create -f k8s-templates/http-transfomer-log/transform-processor-kafka-svc-lb.yaml
    kubectl create -f k8s-templates/http-transfomer-log/log.yaml
    kubectl create -f k8s-templates/http-transfomer-log/log-svc-lb.yaml

    HTTP_SOURCE_SERVER_URI=http://$(kubectl get service http-source | awk '{print $4}' | grep -v EXTERNAL-IP)
    TRANSFORMER_PROCESSOR_SERVER_URI=http://$(kubectl get service transform-processor-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG_SINK_SERVER_URI=http://$(kubectl get service log | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(wait_for_200 ${HTTP_SOURCE_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${TRANSFORMER_PROCESSOR_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG_SINK_SERVER_URI}/actuator/logfile)

    curl -X POST -H "Content-Type: text/plain" --data "foobar" $HTTP_SOURCE_SERVER_URI
}

function prepare_http_splitter_log_with_kafka_binder() {

    kubectl create -f k8s-templates/http-splitter-log/http.yaml
    kubectl create -f k8s-templates/http-splitter-log/http-svc.yaml
    kubectl create -f k8s-templates/http-splitter-log/splitter-processor-kafka.yaml
    kubectl create -f k8s-templates/http-splitter-log/splitter-processor-kafka-svc-lb.yaml
    kubectl create -f k8s-templates/http-splitter-log/log.yaml
    kubectl create -f k8s-templates/http-splitter-log/log-svc-lb.yaml

    HTTP_SOURCE_SERVER_URI=http://$(kubectl get service http-source | awk '{print $4}' | grep -v EXTERNAL-IP)
    SPLITTER_PROCESSOR_SERVER_URI=http://$(kubectl get service splitter-processor-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG_SINK_SERVER_URI=http://$(kubectl get service log | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(wait_for_200 ${HTTP_SOURCE_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${SPLITTER_PROCESSOR_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG_SINK_SERVER_URI}/actuator/logfile)

    curl -X POST -H "Content-Type: text/plain" --data "how much wood would a woodchuck chuck if that woodchuck could chuck wood" $HTTP_SOURCE_SERVER_URI
}

function prepare_http_splitter_partitioned_log_with_kafka_binder() {

    kubectl create -f k8s-templates/http-splitter-partitioned-log/http.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/http-svc.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/splitter-processor-kafka.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/splitter-processor-kafka-svc-lb.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/log-0.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/log-0-svc-lb.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/log-1.yaml
    kubectl create -f k8s-templates/http-splitter-partitioned-log/log-1-svc-lb.yaml

    HTTP_SOURCE_SERVER_URI=http://$(kubectl get service http-source | awk '{print $4}' | grep -v EXTERNAL-IP)
    SPLITTER_PROCESSOR_SERVER_URI=http://$(kubectl get service splitter-processor-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG0_SINK_SERVER_URI=http://$(kubectl get service log-0 | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG1_SINK_SERVER_URI=http://$(kubectl get service log-1 | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(wait_for_200 ${HTTP_SOURCE_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${SPLITTER_PROCESSOR_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG0_SINK_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG1_SINK_SERVER_URI}/actuator/logfile)

    curl -X POST -H "Content-Type: text/plain" --data "How much wood would a woodchuck chuck if that woodchuck could chuck wood" $HTTP_SOURCE_SERVER_URI
}

function prepare_http_router_log_with_kafka_binder() {

    kubectl create -f k8s-templates/http-router-log/http.yaml
    kubectl create -f k8s-templates/http-router-log/http-svc.yaml
    kubectl create -f k8s-templates/http-router-log/router-sink-kafka.yaml
    kubectl create -f k8s-templates/http-router-log/router-sink-kafka-svc-lb.yaml
    kubectl create -f k8s-templates/http-router-log/log-foo.yaml
    kubectl create -f k8s-templates/http-router-log/log-foo-svc-lb.yaml
    kubectl create -f k8s-templates/http-router-log/log-bar.yaml
    kubectl create -f k8s-templates/http-router-log/log-bar-svc-lb.yaml

    HTTP_SOURCE_SERVER_URI=http://$(kubectl get service http-source | awk '{print $4}' | grep -v EXTERNAL-IP)
    ROUTER_SINK_SERVER_URI=http://$(kubectl get service router-sink-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG_FOO_SINK_SERVER_URI=http://$(kubectl get service log-foo | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG_BAR_SINK_SERVER_URI=http://$(kubectl get service log-bar | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(wait_for_200 ${HTTP_SOURCE_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${ROUTER_SINK_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG_FOO_SINK_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG_BAR_SINK_SERVER_URI}/actuator/logfile)

    curl -X POST -H "Content-Type: text/plain" --data "abcdefgh" $HTTP_SOURCE_SERVER_URI
    curl -X POST -H "Content-Type: text/plain" --data "ijklmnop" $HTTP_SOURCE_SERVER_URI
}

function delete_acceptance_test_components() {

    kubectl delete pod,deployment,rc,service -l type="stream-ats"
}

function delete_acceptance_test_infra() {

    kubectl delete pod,deployment,rc,service -l type="stream-ats-kafka"
}

#Main script starting

SECONDS=0

WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-60}"

if [[ -z "${KUBERNETES_NAMESPACE}" ]]; then
  export KUBERNETES_NAMESPACE='default'
fi

kubectl config use-context ${CLUSTER_NAME}
kubectl config set-context ${CLUSTER_NAME} --namespace ${KUBERNETES_NAMESPACE}

kubectl create -f k8s-templates/kafka-zk-deployment.yaml
kubectl create -f k8s-templates/kafka-zk-svc.yaml

kubectl create -f k8s-templates/kafka-deployment.yaml
kubectl create -f k8s-templates/kafka-svc.yaml

prepare_ticktock_latest_with_kafka_binder

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=TickTockLatestAcceptanceTests -Dmaven.test.skip=false -Dtime.source.route=$TIME_SOURCE_SERVER_URI -Dlog.sink.route=$LOG_SINK_SERVER_URI
BUILD_RETURN_VALUE=$?

popd

delete_acceptance_test_components

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."
    delete_acceptance_test_infra
    exit $BUILD_RETURN_VALUE
fi

prepare_http_transform_log_with_kafka_binder

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=HttpTransformLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$HTTP_SOURCE_SERVER_URI -Dtransform.processor.route=$TRANSFORMER_PROCESSOR_SERVER_URI -Dlog.sink.route=$LOG_SINK_SERVER_URI
BUILD_RETURN_VALUE=$?

popd

delete_acceptance_test_components

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in http/transform/log tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."
    delete_acceptance_test_infra
    exit $BUILD_RETURN_VALUE
fi

prepare_http_splitter_log_with_kafka_binder

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=HttpSplitterLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$HTTP_SOURCE_SERVER_URI -Dsplitter.processor.route=$SPLITTER_PROCESSOR_SERVER_URI -Dlog.sink.route=$LOG_SINK_SERVER_URI
BUILD_RETURN_VALUE=$?

popd

delete_acceptance_test_components

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in http/splitter/log tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."
    delete_acceptance_test_infra
    exit $BUILD_RETURN_VALUE
fi

prepare_http_splitter_partitioned_log_with_kafka_binder

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=PartitioningAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$HTTP_SOURCE_SERVER_URI -Dsplitter.processor.route=$SPLITTER_PROCESSOR_SERVER_URI -Dlog0.sink.route=$LOG0_SINK_SERVER_URI -Dlog1.sink.route=$LOG1_SINK_SERVER_URI
BUILD_RETURN_VALUE=$?

popd

delete_acceptance_test_components

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in partitioning tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."
    delete_acceptance_test_infra
    exit $BUILD_RETURN_VALUE
fi

prepare_http_router_log_with_kafka_binder

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=HttpRouterLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$HTTP_SOURCE_SERVER_URI -Drouter.sink.route=$ROUTER_SINK_SERVER_URI -Dlog.foo.sink.route=$LOG_FOO_SINK_SERVER_URI -Dlog.bar.sink.route=$LOG_BAR_SINK_SERVER_URI
BUILD_RETURN_VALUE=$?

popd

delete_acceptance_test_components

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in http-router tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."
    delete_acceptance_test_infra
    exit $BUILD_RETURN_VALUE
fi

delete_acceptance_test_infra

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE
