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
    kubectl create -f k8s-templates/time.yaml
    kubectl create -f k8s-templates/time-svc-lb.yaml

    kubectl create -f k8s-templates/log.yaml
    kubectl create -f k8s-templates/log-svc-lb.yaml

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

#    curl -X POST -H "Content-Type: text/plain" --data "foobar" $HTTP_SOURCE_SERVER_URI
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

../mvnw clean package -Dtest=HttpTransformerLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$HTTP_SOURCE_SERVER_URI -Dtransformer.processor.route=$TRANSFORMER_PROCESSOR_SERVER_URI -Dlog.sink.route=$LOG_SINK_SERVER_URI
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


delete_acceptance_test_infra

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE
