
#!/bin/bash

# First argument is CF URL ($1)
# Second argument is CF User ($2)
# Third argument is CF Passwrod ($3)
# Fourth argument is CF Org ($4)
# Fifth argument is CF Space ($5)
# Optional sixth argument to skip ssl validation: skip-ssl-validation (No double hiphens (--) in the front)

pushd () {
    command pushd "$@" > /dev/null
}

popd () {
    command popd "$@" > /dev/null
}

function prepare_http_transform_log_with_rabbit_binder() {

    wget -O /tmp/http-source-rabbit.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/http-source-rabbit/2.1.0.BUILD-SNAPSHOT/http-source-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    wget -O /tmp/transform-processor-rabbit.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/transform-processor-rabbit/2.1.0.BUILD-SNAPSHOT/transform-processor-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    wget -O /tmp/log-sink-rabbit.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.0.BUILD-SNAPSHOT/log-sink-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/http-source-manifest.yml

    cf app http-source-rabbit > /tmp/http-source-route.txt

    HTTP_SOURCE_ROUTE=`grep routes /tmp/http-source-route.txt | awk '{ print $2 }'`

    FULL_HTTP_SOURCE_ROUTE=http://$HTTP_SOURCE_ROUTE

    cf push -f ./cf-manifests/transform-processor-manifest.yml

    cf app transform-processor-rabbit > /tmp/transform-processor-route.txt

    TRANSFORM_PROCESSOR_ROUTE=`grep routes /tmp/transform-processor-route.txt | awk '{ print $2 }'`

    FULL_TRANSFORM_PROCESSOR_ROUTE=http://$TRANSFORM_PROCESSOR_ROUTE

    cf push -f ./cf-manifests/httptransform-log-sink-manifest.yml

    cf app log-sink-rabbit > /tmp/httptransform-log-sink-route.txt

    HTTPTRANSFORM_LOG_SINK_ROUTE=`grep routes /tmp/httptransform-log-sink-route.txt | awk '{ print $2 }'`

    FULL_HTTPTRANSFORM_LOG_SINK_ROUTE=http://$HTTPTRANSFORM_LOG_SINK_ROUTE
}


function prepare_http_splitter_log_with_rabbit_binder() {

    wget -O /tmp/http-source-rabbit.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/http-source-rabbit/2.1.0.BUILD-SNAPSHOT/http-source-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    wget -O /tmp/splitter-processor-rabbit.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/splitter-processor-rabbit/2.1.0.BUILD-SNAPSHOT/splitter-processor-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    wget -O /tmp/log-sink-rabbit.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.0.BUILD-SNAPSHOT/log-sink-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/http-source-manifest.yml

    cf app http-source-rabbit > /tmp/http-source-route.txt

    HTTP_SOURCE_ROUTE=`grep routes /tmp/http-source-route.txt | awk '{ print $2 }'`

    FULL_HTTP_SOURCE_ROUTE=http://$HTTP_SOURCE_ROUTE

    cf push -f ./cf-manifests/splitter-processor-manifest.yml

    cf app splitter-processor-rabbit > /tmp/splitter-processor-route.txt

    SPLITTER_PROCESSOR_ROUTE=`grep routes /tmp/splitter-processor-route.txt | awk '{ print $2 }'`

    FULL_SPLITTER_PROCESSOR_ROUTE=http://$SPLITTER_PROCESSOR_ROUTE

    cf push -f ./cf-manifests/httpsplitter-log-sink-manifest.yml

    cf app log-sink-rabbit > /tmp/httpsplitter-log-sink-route.txt

    HTTPSPLITTER_LOG_SINK_ROUTE=`grep routes /tmp/httpsplitter-log-sink-route.txt | awk '{ print $2 }'`

    FULL_HTTPSPLITTER_LOG_SINK_ROUTE=http://$HTTPSPLITTER_LOG_SINK_ROUTE
}

function prepare_ticktock_latest_with_rabbit_binder() {

    wget -O /tmp/ticktock-time-source.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/time-source-rabbit/2.1.0.BUILD-SNAPSHOT/time-source-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    wget -O /tmp/ticktock-log-sink.jar http://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.0.BUILD-SNAPSHOT/log-sink-rabbit-2.1.0.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/time-source-manifest.yml

    cf app ticktock-time-source > /tmp/ticktock-time-source-route.txt

    TICKTOCK_TIME_SOURCE_ROUTE=`grep routes /tmp/ticktock-time-source-route.txt | awk '{ print $2 }'`

    FULL_TICKTOCK_TIME_SOURCE_ROUTE=http://$TICKTOCK_TIME_SOURCE_ROUTE

    cf push -f ./cf-manifests/log-sink-manifest.yml

    cf app ticktock-log-sink > /tmp/ticktock-log-sink-route.txt

    TICKTOCK_LOG_SINK_ROUTE=`grep routes /tmp/ticktock-log-sink-route.txt | awk '{ print $2 }'`

    FULL_TICKTOCK_LOG_SINK_ROUTE=http://$TICKTOCK_LOG_SINK_ROUTE
}

function prepare_ticktock_13_with_rabbit_binder() {

    wget -O /tmp/ticktock-time-source131.jar http://repo.spring.io/release/org/springframework/cloud/stream/app/time-source-rabbit/1.3.1.RELEASE/time-source-rabbit-1.3.1.RELEASE.jar

    wget -O /tmp/ticktock-log-sink131.jar http://repo.spring.io/release/org/springframework/cloud/stream/app/log-sink-rabbit/1.3.1.RELEASE/log-sink-rabbit-1.3.1.RELEASE.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/time-source-manifest131.yml

    cf app ticktock-time-source131 > /tmp/ticktock-time-source-route131.txt

    TICKTOCK_TIME_SOURCE_ROUTE_131=`grep routes /tmp/ticktock-time-source-route131.txt | awk '{ print $2 }'`

    FULL_TICKTOCK_TIME_SOURCE_ROUTE_131=http://$TICKTOCK_TIME_SOURCE_ROUTE_131

    cf push -f ./cf-manifests/log-sink-manifest131.yml

    cf app ticktock-log-sink131 > /tmp/ticktock-log-sink-route131.txt

    TICKTOCK_LOG_SINK_ROUTE_131=`grep routes /tmp/ticktock-log-sink-route131.txt | awk '{ print $2 }'`

    FULL_TICKTOCK_LOG_SINK_ROUTE_131=http://$TICKTOCK_LOG_SINK_ROUTE_131
}

function prepare_uppercase_transformer_with_rabbit_binder() {

    wget -O /tmp/uppercase-transformer-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/acceptance/uppercase-transformer-rabbit/0.0.1-SNAPSHOT/uppercase-transformer-rabbit-0.0.1-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/uppercase-processor-manifest.yml

    cf app uppercase-transformer > /tmp/uppercase-route.txt

    UPPERCASE_PROCESSOR_ROUTE=`grep routes /tmp/uppercase-route.txt | awk '{ print $2 }'`

    FULL_UPPERCASE_ROUTE=http://$UPPERCASE_PROCESSOR_ROUTE
}

function prepare_partitioning_test_with_rabbit_binder() {

    wget -O /tmp/partitioning-producer-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/acceptance/partitioning-producer-sample-rabbit/0.0.1-SNAPSHOT/partitioning-producer-sample-rabbit-0.0.1-SNAPSHOT.jar
    wget -O /tmp/partitioning-consumer-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/acceptance/partitioning-consumer-sample-rabbit/0.0.1-SNAPSHOT/partitioning-consumer-sample-rabbit-0.0.1-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/partitioning-producer-manifest.yml

    cf app partitioning-producer > /tmp/part-producer-route.txt

    PARTITIONING_PRODUCER_ROUTE=`grep routes /tmp/part-producer-route.txt | awk '{ print $2 }'`

    FULL_PARTITIONING_PRODUCER_ROUTE=http://$PARTITIONING_PRODUCER_ROUTE

    # consumer 1

    cf push -f ./cf-manifests/partitioning-consumer1-manifest.yml

    cf app partitioning-consumer1 > /tmp/part-consumer1-route.txt

    PARTITIONING_CONSUMER1_ROUTE=`grep routes /tmp/part-consumer1-route.txt | awk '{ print $2 }'`

    FULL_PARTITIONING_CONSUMER1_ROUTE=http://$PARTITIONING_CONSUMER1_ROUTE

    #consumer 2

    cf push -f ./cf-manifests/partitioning-consumer2-manifest.yml

    cf app partitioning-consumer2 > /tmp/part-consumer2-route.txt

    PARTITIONING_CONSUMER2_ROUTE=`grep routes /tmp/part-consumer2-route.txt | awk '{ print $2 }'`

    FULL_PARTITIONING_CONSUMER2_ROUTE=http://$PARTITIONING_CONSUMER2_ROUTE

    #consumer 3

    cf push -f ./cf-manifests/partitioning-consumer3-manifest.yml

    cf app partitioning-consumer3 > /tmp/part-consumer3-route.txt

    PARTITIONING_CONSUMER3_ROUTE=`grep routes /tmp/part-consumer3-route.txt | awk '{ print $2 }'`

    FULL_PARTITIONING_CONSUMER3_ROUTE=http://$PARTITIONING_CONSUMER3_ROUTE

}

#Main script starting

SECONDS=0

echo "Prepare artifacts for http | transform | log testing"

prepare_http_transform_log_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=HttpTransformLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$FULL_HTTP_SOURCE_ROUTE -Dtransform.processor.route=$FULL_TRANSFORM_PROCESSOR_ROUTE -Dlog.sink.route=$FULL_HTTPTRANSFORM_LOG_SINK_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop http-source-rabbit && cf delete http-source-rabbit -f
cf stop transform-processor-rabbit && cf delete splitter-processor-rabbit -f
cf stop log-sink-rabbit && cf delete log-sink-rabbit -f

cf logout

rm /tmp/http-source-route.txt
rm /tmp/transform-processor-route.txt
rm /tmp/httptransform-log-sink-route.txt

rm /tmp/http-source-rabbit.jar
rm /tmp/transform-processor-rabbit.jar
rm /tmp/log-sink-rabbit.jar

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi

echo "Prepare artifacts for http | splitter | log testing"

prepare_http_splitter_log_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=HttpSplitterLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$FULL_HTTP_SOURCE_ROUTE -Dsplitter.processor.route=$FULL_SPLITTER_PROCESSOR_ROUTE -Dlog.sink.route=$FULL_HTTPSPLITTER_LOG_SINK_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop http-source-rabbit
cf stop splitter-processor-rabbit
cf stop log-sink-rabbit

cf delete http-source-rabbit -f
cf delete splitter-processor-rabbit -f
cf delete log-sink-rabbit -f

cf logout

rm /tmp/http-source-route.txt
rm /tmp/splitter-processor-route.txt
rm /tmp/httpsplitter-log-sink-route.txt

rm /tmp/http-source-rabbit.jar
rm /tmp/splitter-processor-rabbit.jar
rm /tmp/log-sink-rabbit.jar

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi

echo "Prepare artifacts for ticktock testing"

prepare_ticktock_latest_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=TickTockLatestAcceptanceTests -Dmaven.test.skip=false -Dtime.source.route=$FULL_TICKTOCK_TIME_SOURCE_ROUTE -Dlog.sink.route=$FULL_TICKTOCK_LOG_SINK_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop ticktock-time-source
cf stop ticktock-log-sink

cf delete ticktock-time-source -f
cf delete ticktock-log-sink -f

cf logout

rm /tmp/ticktock-time-source-route.txt
rm /tmp/ticktock-log-sink-route.txt

rm /tmp/ticktock-time-source.jar
rm /tmp/ticktock-log-sink.jar

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi


echo "Prepare artifacts for ticktock1.3.1 testing"

prepare_ticktock_13_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=TickTock13AcceptanceTests -Dmaven.test.skip=false -Dtime.source.route=$FULL_TICKTOCK_TIME_SOURCE_ROUTE_131 -Dlog.sink.route=$FULL_TICKTOCK_LOG_SINK_ROUTE_131
BUILD_RETURN_VALUE=$?

popd

cf stop ticktock-time-source131
cf stop ticktock-log-sink131

cf delete ticktock-time-source131 -f
cf delete ticktock-log-sink131 -f

cf logout

rm /tmp/ticktock-time-source-route131.txt
rm /tmp/ticktock-log-sink-route131.txt

rm /tmp/ticktock-time-source131.jar
rm /tmp/ticktock-log-sink131.jar

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi

echo "Prepare artifacts for uppercase transformer testing"

prepare_uppercase_transformer_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=UppercaseTransformerAcceptanceTests -Dmaven.test.skip=false -Duppercase.processor.route=$FULL_UPPERCASE_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop uppercase-transformer

cf delete uppercase-transformer -f

cf logout

rm /tmp/uppercase-route.txt

rm /tmp/uppercase-transformer-rabbit.jar

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in uppercase transformer"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi

echo "Prepare artifacts for partitions testing"

prepare_partitioning_test_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../spring-cloud-stream-acceptance-tests

../mvnw clean package -Dtest=PartitioningAcceptanceTests -Dmaven.test.skip=false -Duppercase.processor.route=$FULL_UPPERCASE_ROUTE -Dpartitioning.producer.route=$FULL_PARTITIONING_PRODUCER_ROUTE  -Dpartitioning.consumer1.route=$FULL_PARTITIONING_CONSUMER1_ROUTE -Dpartitioning.consumer2.route=$FULL_PARTITIONING_CONSUMER2_ROUTE -Dpartitioning.consumer3.route=$FULL_PARTITIONING_CONSUMER3_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop partitioning-producer
cf stop partitioning-consumer1
cf stop partitioning-consumer2
cf stop partitioning-consumer3

cf delete partitioning-producer -f
cf delete partitioning-consumer1 -f
cf delete partitioning-consumer2 -f
cf delete partitioning-consumer3 -f

cf logout

rm /tmp/part-producer-route.txt
rm /tmp/part-consumer1-route.txt
rm /tmp/part-consumer2-route.txt
rm /tmp/part-consumer3-route.txt

rm /tmp/partitioning-producer-rabbit.jar
rm /tmp/partitioning-consumer-rabbit.jar

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE