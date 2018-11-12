
#!/bin/bash

# First argument is GKE Project
# Second argument is GKE Cluster
# Third argument is GKE Zone
# Fourth argument is GKE Cluster version

pushd () {
    command pushd "$@" > /dev/null
}

popd () {
    command popd "$@" > /dev/null
}

function netcat_port() {
    local READY_FOR_TESTS=1
    for i in $( seq 1 "${RETRIES}" ); do
        nc -w1 ${1} $2 </dev/null && READY_FOR_TESTS=0 && break
        echo "Failed to connect to ${1}:$2. Attempt  #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds" >&2
        sleep "${WAIT_TIME}"
    done
    return ${READY_FOR_TESTS}
}

function prepare_ticktock_latest_with_kafka_binder() {

    grep $CLUSTER_NAME ~/.kube/config
    grep_result=$?
    if [ $grep_result = 0 ]; then
        kubectl config delete-context $CLUSTER_NAME
    fi

    gcloud container --project ${PROJECT_NAME} clusters create ${CLUSTER_NAME} --zone ${GKE_ZONE} --machine-type "custom-2-4096" \
    --cluster-version ${CLUSTER_VERSION} --num-nodes "2" --image-type "COS" --disk-size "25" --network "default" \
    --enable-cloud-logging --enable-cloud-monitoring \
    --scopes "https://www.googleapis.com/auth/compute","https://www.googleapis.com/auth/devstorage.read_only","https://www.googleapis.com/auth/logging.write","https://www.googleapis.com/auth/monitoring","https://www.googleapis.com/auth/servicecontrol","https://www.googleapis.com/auth/service.management.readonly","https://www.googleapis.com/auth/trace.append"

    gcloud container clusters get-credentials ${CLUSTER_NAME} --zone ${GKE_ZONE} --project ${PROJECT_NAME}

    kubectl create -f k8s-templates/kafka-zk-deployment.yaml
    kubectl create -f k8s-templates/kafka-zk-svc.yaml

    kubectl create -f k8s-templates/kafka-deployment.yaml
    kubectl create -f k8s-templates/kafka-svc.yaml

    kubectl create -f k8s-templates/time.yaml
    kubectl create -f k8s-templates/time-svc-lb.yaml

    kubectl create -f k8s-templates/log.yaml
    kubectl create -f k8s-templates/log-svc-lb.yaml

    READY_FOR_TESTS=1
    for i in $( seq 1 "${RETRIES}" ); do
        TIME_SOURCE_SERVER_URI=$(kubectl get service time | awk '{print $4}' | grep -v EXTERNAL-IP)
        LOG_SINK_SERVER_URI=$(kubectl get service log | awk '{print $4}' | grep -v EXTERNAL-IP)
        [ '<pending>' != $TIME_SOURCE_SERVER_URI ] && [ '<pending>' != $LOG_SINK_SERVER_URI ] && READY_FOR_TESTS=0 && break
        echo "Waiting for server external ip for time source and log sinks. Attempt  #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds" >&2
        sleep "${WAIT_TIME}"
    done
    TIME_SOURCE_SERVER_URI=$(kubectl get service time | awk '{print $4}' | grep -v EXTERNAL-IP)
    LOG_SINK_SERVER_URI=$(kubectl get service log | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(netcat_port ${TIME_SOURCE_SERVER_URI} 80)
    $(netcat_port ${LOG_SINK_SERVER_URI} 80)

    FULL_TICKTOCK_TIME_SOURCE_ROUTE=http://$TIME_SOURCE_SERVER_URI
    FULL_TICKTOCK_LOG_SINK_ROUTE=http://$LOG_SINK_SERVER_URI
}

function prepare_uppercase_transformer_with_kafka_binder() {

   kubectl create -f k8s-templates/uppercase-transformer-kafka.yaml
   kubectl create -f k8s-templates/uppercase-transformer-kafka-svc-lb.yaml

    READY_FOR_TESTS=1
    for i in $( seq 1 "${RETRIES}" ); do
        UPPERCASE_TRANSFORMER_KAFKA_SERVER_URI=$(kubectl get service uppercase-transformer-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
        [ '<pending>' != $UPPERCASE_TRANSFORMER_KAFKA_SERVER_URI ] && READY_FOR_TESTS=0 && break
        echo "Waiting for server external ip for time source and log sinks. Attempt  #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds" >&2
        sleep "${WAIT_TIME}"
    done
    UPPERCASE_TRANSFORMER_KAFKA_SERVER_URI=$(kubectl get service uppercase-transformer-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(netcat_port ${UPPERCASE_TRANSFORMER_KAFKA_SERVER_URI} 80)

    FULL_UPPERCASE_ROUTE=http://$UPPERCASE_TRANSFORMER_KAFKA_SERVER_URI
}

function prepare_partitioning_test_with_kafka_binder() {

    kubectl create -f k8s-templates/partitioning-consumer1-sample-kafka.yaml
    kubectl create -f k8s-templates/partitioning-consumer1-sample-kafka-svc-lb.yaml

    kubectl create -f k8s-templates/partitioning-consumer2-sample-kafka.yaml
    kubectl create -f k8s-templates/partitioning-consumer2-sample-kafka-svc-lb.yaml

    kubectl create -f k8s-templates/partitioning-consumer3-sample-kafka.yaml
    kubectl create -f k8s-templates/partitioning-consumer3-sample-kafka-svc-lb.yaml

     kubectl create -f k8s-templates/partitioning-producer-sample-kafka.yaml
    kubectl create -f k8s-templates/partitioning-producer-sample-kafka-svc-lb.yaml


     READY_FOR_TESTS=1
        for i in $( seq 1 "${RETRIES}" ); do
            PARTITIONING_PRODUCER_SERVER_URI=$(kubectl get service partitioning-producer-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
            PARTITIONING_CONSUMER1_SERVER_URI=$(kubectl get service partitioning-consumer1-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
            PARTITIONING_CONSUMER2_SERVER_URI=$(kubectl get service partitioning-consumer2-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
            PARTITIONING_CONSUMER3_SERVER_URI=$(kubectl get service partitioning-consumer3-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
          [ '<pending>' != $PARTITIONING_PRODUCER_SERVER_URI ] && [ '<pending>' != $PARTITIONING_CONSUMER1_SERVER_URI ]  && [ '<pending>' != $PARTITIONING_CONSUMER2_SERVER_URI ] && [ '<pending>' != $PARTITIONING_CONSUMER3_SERVER_URI ] && READY_FOR_TESTS=0 && break
           echo "Waiting for server external ip for partitioning producer/consumer apps. Attempt  #$i/${RETRIES}... will try again in [${WAIT_TIME}] seconds" >&2
             sleep "${WAIT_TIME}"
        done

    PARTITIONING_PRODUCER_SERVER_URI=$(kubectl get service partitioning-producer-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
        PARTITIONING_CONSUMER1_SERVER_URI=$(kubectl get service partitioning-consumer1-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
        PARTITIONING_CONSUMER2_SERVER_URI=$(kubectl get service partitioning-consumer2-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)
        PARTITIONING_CONSUMER3_SERVER_URI=$(kubectl get service partitioning-consumer3-sample-kafka | awk '{print $4}' | grep -v EXTERNAL-IP)

    $(netcat_port ${PARTITIONING_PRODUCER_SERVER_URI} 80)
    $(netcat_port ${PARTITIONING_CONSUMER1_SERVER_URI} 80)
    $(netcat_port ${PARTITIONING_CONSUMER2_SERVER_URI} 80)
    $(netcat_port ${PARTITIONING_CONSUMER3_SERVER_URI} 80)

    FULL_PARTITIONING_PRODUCER_ROUTE=http://$PARTITIONING_PRODUCER_SERVER_URI
    FULL_PARTITIONING_CONSUMER1_ROUTE=http://$PARTITIONING_CONSUMER1_SERVER_URI
    FULL_PARTITIONING_CONSUMER2_ROUTE=http://$PARTITIONING_CONSUMER2_SERVER_URI
    FULL_PARTITIONING_CONSUMER3_ROUTE=http://$PARTITIONING_CONSUMER3_SERVER_URI
}


function delete_acceptance_test_components() {

    kubectl delete pod,deployment,rc,service -l type="acceptance-tests"
}

function delete_kafka_components() {

    kubectl delete pod,deployment,rc,service -l app="kafka"
}

function delete_test_cluster()  {

    gcloud container clusters delete ${CLUSTER_NAME} --zone ${GKE_ZONE} --project ${PROJECT_NAME} --quiet

}

#Main script starting

SECONDS=0

WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-60}"

PROJECT_NAME=$1
CLUSTER_NAME=$2
GKE_ZONE=$3
CLUSTER_VERSION=$4

#prepare_ticktock_latest_with_kafka_binder ${PROJECT_NAME} ${CLUSTER_NAME} ${GKE_ZONE} ${CLUSTER_VERSION}
#
#pushd ../spring-cloud-stream-acceptance-tests
#
#../mvnw clean package -Dtest=TickTockLatestAcceptanceTests -Dmaven.test.skip=false -Dtime.source.route=$FULL_TICKTOCK_TIME_SOURCE_ROUTE -Dlog.sink.route=$FULL_TICKTOCK_LOG_SINK_ROUTE
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
#
#prepare_uppercase_transformer_with_kafka_binder
#
#pushd ../spring-cloud-stream-acceptance-tests
#
#../mvnw clean package -Dtest=UppercaseTransformerAcceptanceTests -Dmaven.test.skip=false -Duppercase.processor.route=$FULL_UPPERCASE_ROUTE
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

prepare_partitioning_test_with_kafka_binder ${PROJECT_NAME} ${CLUSTER_NAME} ${GKE_ZONE} ${CLUSTER_VERSION}

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=PartitioningKafkaAcceptanceTests -Dmaven.test.skip=false -Dpartitioning.producer.route=$FULL_PARTITIONING_PRODUCER_ROUTE  -Dpartitioning.consumer1.route=$FULL_PARTITIONING_CONSUMER1_ROUTE -Dpartitioning.consumer2.route=$FULL_PARTITIONING_CONSUMER2_ROUTE -Dpartitioning.consumer3.route=$FULL_PARTITIONING_CONSUMER3_ROUTE
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

delete_kafka_components
sleep 60
delete_test_cluster ${CLUSTER_NAME} ${GKE_ZONE} ${PROJECT_NAME}

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE
