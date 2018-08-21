
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

function delete_ticktock_components() {

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

echo "Prepare artifacts for ticktock testing"

WAIT_TIME="${WAIT_TIME:-5}"
RETRIES="${RETRIES:-60}"

PROJECT_NAME=$1
CLUSTER_NAME=$2
GKE_ZONE=$3
CLUSTER_VERSION=$4

prepare_ticktock_latest_with_kafka_binder ${PROJECT_NAME} ${CLUSTER_NAME} ${GKE_ZONE} ${CLUSTER_VERSION}

./mvnw  -P acceptance-tests clean package -Dtest=TickTockAcceptanceTests -Dmaven.test.skip=false -Dtime.source.route=$FULL_TICKTOCK_TIME_SOURCE_ROUTE -Dlog.sink.route=$FULL_TICKTOCK_LOG_SINK_ROUTE
BUILD_RETURN_VALUE=$?

delete_ticktock_components

delete_kafka_components

delete_test_cluster ${CLUSTER_NAME} ${GKE_ZONE} ${PROJECT_NAME}

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE