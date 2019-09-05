
#!/bin/bash

# First argument is GKE Project
# Second argument is GKE Cluster
# Third argument is GKE Zone
# Fourth arg namespace
# Fifth arg is base domain

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

    TIME_SOURCE_SERVER_URI=https://time.${CLUSTER_NAME}.${BASE_DOMAIN}
    LOG_SINK_SERVER_URI=https://log.${CLUSTER_NAME}.${BASE_DOMAIN}

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

    HTTP_SOURCE_SERVER_URI=https://http-source.${CLUSTER_NAME}.${BASE_DOMAIN}
    TRANSFORMER_PROCESSOR_SERVER_URI=https://transform-processor-kafka.${CLUSTER_NAME}.${BASE_DOMAIN}
    LOG_SINK_SERVER_URI=https://log.${CLUSTER_NAME}.${BASE_DOMAIN}

    $(wait_for_200 ${HTTP_SOURCE_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${TRANSFORMER_PROCESSOR_SERVER_URI}/actuator/logfile)
    $(wait_for_200 ${LOG_SINK_SERVER_URI}/actuator/logfile)

    curl -X POST -H "Content-Type: text/plain" --data "foobar" $HTTP_SOURCE_SERVER_URI
}
#
#function prepare_partitioning_test_with_kafka_binder() {
#
#    kubectl create -f k8s-templates/partitioning-consumer1-sample-kafka.yaml
#    kubectl create -f k8s-templates/partitioning-consumer1-sample-kafka-svc-lb.yaml
#
#    kubectl create -f k8s-templates/partitioning-consumer2-sample-kafka.yaml
#    kubectl create -f k8s-templates/partitioning-consumer2-sample-kafka-svc-lb.yaml
#
#    kubectl create -f k8s-templates/partitioning-consumer3-sample-kafka.yaml
#    kubectl create -f k8s-templates/partitioning-consumer3-sample-kafka-svc-lb.yaml
#
#     kubectl create -f k8s-templates/partitioning-producer-sample-kafka.yaml
#    kubectl create -f k8s-templates/partitioning-producer-sample-kafka-svc-lb.yaml
#
#
#     READY_FOR_TESTS=1
#        for i in $( seq 1 "${RETRIES}" ); do
#            PARTITIONING_PRODUCER_SERVER_URI=$(kubectl get service partitioning-producer-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#            PARTITIONING_CONSUMER1_SERVER_URI=$(kubectl get service partitioning-consumer1-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#            PARTITIONING_CONSUMER2_SERVER_URI=$(kubectl get service partitioning-consumer2-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#            PARTITIONING_CONSUMER3_SERVER_URI=$(kubectl get service partitioning-consumer3-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#          [ '<pending>' != $PARTITIONING_PRODUCER_SERVER_URI ] && [ '<pending>' != $PARTITIONING_CONSUMER1_SERVER_URI ]  && [ '<pending>' != $PARTITIONING_CONSUMER2_SERVER_URI ] && [ '<pending>' != $PARTITIONING_CONSUMER3_SERVER_URI ] && READY_FOR_TESTS=0 && break
#           echo "Waiting for server external ip for partitioning producer/consumer apps. Attempt  #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds" >&2
#             sleep "${WAIT_TIME}"
#        done
#
#    PARTITIONING_PRODUCER_SERVER_URI=$(kubectl get service partitioning-producer-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#        PARTITIONING_CONSUMER1_SERVER_URI=$(kubectl get service partitioning-consumer1-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#        PARTITIONING_CONSUMER2_SERVER_URI=$(kubectl get service partitioning-consumer2-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#        PARTITIONING_CONSUMER3_SERVER_URI=$(kubectl get service partitioning-consumer3-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
#
#    $(netcat_port ${PARTITIONING_PRODUCER_SERVER_URI} 80)
#    $(netcat_port ${PARTITIONING_CONSUMER1_SERVER_URI} 80)
#    $(netcat_port ${PARTITIONING_CONSUMER2_SERVER_URI} 80)
#    $(netcat_port ${PARTITIONING_CONSUMER3_SERVER_URI} 80)
#
#    FULL_PARTITIONING_PRODUCER_ROUTE=http://$PARTITIONING_PRODUCER_SERVER_URI
#    FULL_PARTITIONING_CONSUMER1_ROUTE=http://$PARTITIONING_CONSUMER1_SERVER_URI
#    FULL_PARTITIONING_CONSUMER2_ROUTE=http://$PARTITIONING_CONSUMER2_SERVER_URI
#    FULL_PARTITIONING_CONSUMER3_ROUTE=http://$PARTITIONING_CONSUMER3_SERVER_URI
#}


function delete_acceptance_test_components() {

    kubectl delete pod,deployment,rc,service -l type="stream-ats"
}

#Main script starting

SECONDS=0

WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-60}"

PROJECT_NAME=$1
CLUSTER_NAME=$2
GKE_ZONE=$3
NAMESPACE=$4
BASE_DOMAIN=$5

gcloud container clusters get-credentials ${CLUSTER_NAME} --zone ${GKE_ZONE} --project ${PROJECT_NAME}

C_TMP=$(kubectl config get-contexts | grep ${CLUSTER_NAME} | awk '{print $2}')
kubectl config use-context $C_TMP
kubectl config set-context $C_TMP --namespace ${NAMESPACE}

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
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    delete_kafka_components
    sleep 60
    delete_test_cluster ${CLUSTER_NAME} ${GKE_ZONE} ${PROJECT_NAME}

    exit $BUILD_RETURN_VALUE
fi

#prepare_partitioning_test_with_kafka_binder ${PROJECT_NAME} ${CLUSTER_NAME} ${GKE_ZONE} ${CLUSTER_VERSION}
#
#pushd ../spring-cloud-stream-acceptance-tests
#
#../mvnw clean package -Dtest=PartitioningKafkaAcceptanceTests -Dmaven.test.skip=false -Dpartitioning.producer.route=$FULL_PARTITIONING_PRODUCER_ROUTE  -Dpartitioning.consumer1.route=$FULL_PARTITIONING_CONSUMER1_ROUTE -Dpartitioning.consumer2.route=$FULL_PARTITIONING_CONSUMER2_ROUTE -Dpartitioning.consumer3.route=$FULL_PARTITIONING_CONSUMER3_ROUTE
#BUILD_RETURN_VALUE=$?
#
#popd
#
#delete_acceptance_test_components
#
#if [ "$BUILD_RETURN_VALUE" != 0 ]
#then
#    echo "Early exit due to test failure in ticktock tests"
#    duration=$SECONDS
#
#    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."
#
#    delete_kafka_components
#    sleep 60
#    delete_test_cluster ${CLUSTER_NAME} ${GKE_ZONE} ${PROJECT_NAME}
#
#    exit $BUILD_RETURN_VALUE
#fi

#delete_kafka_components
#delete_test_cluster ${CLUSTER_NAME} ${GKE_ZONE} ${PROJECT_NAME}

kubectl delete pod,deployment,rc,service -l type=stream-ats-kafka

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE
