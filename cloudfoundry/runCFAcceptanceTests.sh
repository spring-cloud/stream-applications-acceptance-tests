
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

function prepare_jdbc_log_with_rabbit_binder() {

    wget -O /tmp/jdbc-source-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/jdbc-source-rabbit/2.1.5.BUILD-SNAPSHOT/jdbc-source-rabbit-2.1.5.BUILD-SNAPSHOT.jar

    wget -O /tmp/log-sink-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.4.BUILD-SNAPSHOT/log-sink-rabbit-2.1.4.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/jdbc-source-manifest.yml

    cf app jdbc-source-rabbit > /tmp/jdbc-source-route.txt

    JDBC_SOURCE_ROUTE=`grep routes /tmp/jdbc-source-route.txt | awk '{ print $2 }'`

    FULL_JDBC_SOURCE_ROUTE=https://$JDBC_SOURCE_ROUTE

    cf push -f ./cf-manifests/jdbc-log-sink-manifest.yml

    cf app jdbc-log-sink-rabbit > /tmp/jdbc-log-sink-route.txt

    JDBC_LOG_SINK_ROUTE=`grep routes /tmp/jdbc-log-sink-route.txt | awk '{ print $2 }'`

    FULL_JDBC_LOG_SINK_ROUTE=https://$JDBC_LOG_SINK_ROUTE
}

function prepare_http_transform_log_with_rabbit_binder() {

    wget -O /tmp/http-source-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/http-source-rabbit/2.1.3.BUILD-SNAPSHOT/http-source-rabbit-2.1.3.BUILD-SNAPSHOT.jar

    wget -O /tmp/transform-processor-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/transform-processor-rabbit/2.1.3.BUILD-SNAPSHOT/transform-processor-rabbit-2.1.3.BUILD-SNAPSHOT.jar

    wget -O /tmp/log-sink-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.4.BUILD-SNAPSHOT/log-sink-rabbit-2.1.4.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/http-source-manifest.yml

    cf app http-source-rabbit > /tmp/http-source-route.txt

    HTTP_SOURCE_ROUTE=`grep routes /tmp/http-source-route.txt | awk '{ print $2 }'`

    FULL_HTTP_SOURCE_ROUTE=https://$HTTP_SOURCE_ROUTE

    cf push -f ./cf-manifests/transform-processor-manifest.yml

    cf app transform-processor-rabbit > /tmp/transform-processor-route.txt

    TRANSFORM_PROCESSOR_ROUTE=`grep routes /tmp/transform-processor-route.txt | awk '{ print $2 }'`

    FULL_TRANSFORM_PROCESSOR_ROUTE=https://$TRANSFORM_PROCESSOR_ROUTE

    cf push -f ./cf-manifests/httptransform-log-sink-manifest.yml

    cf app log-sink-rabbit > /tmp/httptransform-log-sink-route.txt

    HTTPTRANSFORM_LOG_SINK_ROUTE=`grep routes /tmp/httptransform-log-sink-route.txt | awk '{ print $2 }'`

    FULL_HTTPTRANSFORM_LOG_SINK_ROUTE=https://$HTTPTRANSFORM_LOG_SINK_ROUTE

    curl -X POST -H "Content-Type: text/plain" --data "foobar" $FULL_HTTP_SOURCE_ROUTE
}

function prepare_http_splitter_log_with_rabbit_binder() {

    wget -O /tmp/http-source-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/http-source-rabbit/2.1.3.BUILD-SNAPSHOT/http-source-rabbit-2.1.3.BUILD-SNAPSHOT.jar

    wget -O /tmp/splitter-processor-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/splitter-processor-rabbit/2.1.3.BUILD-SNAPSHOT/splitter-processor-rabbit-2.1.3.BUILD-SNAPSHOT.jar

    wget -O /tmp/log-sink-rabbit.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.4.BUILD-SNAPSHOT/log-sink-rabbit-2.1.4.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/http-source-manifest.yml

    cf app http-source-rabbit > /tmp/http-source-route.txt

    HTTP_SOURCE_ROUTE=`grep routes /tmp/http-source-route.txt | awk '{ print $2 }'`

    FULL_HTTP_SOURCE_ROUTE=https://$HTTP_SOURCE_ROUTE

    cf push -f ./cf-manifests/splitter-processor-manifest.yml

    cf app splitter-processor-rabbit > /tmp/splitter-processor-route.txt

    SPLITTER_PROCESSOR_ROUTE=`grep routes /tmp/splitter-processor-route.txt | awk '{ print $2 }'`

    FULL_SPLITTER_PROCESSOR_ROUTE=https://$SPLITTER_PROCESSOR_ROUTE

    cf push -f ./cf-manifests/httpsplitter-log-sink-manifest.yml

    cf app log-sink-rabbit > /tmp/httpsplitter-log-sink-route.txt

    HTTPSPLITTER_LOG_SINK_ROUTE=`grep routes /tmp/httpsplitter-log-sink-route.txt | awk '{ print $2 }'`

    FULL_HTTPSPLITTER_LOG_SINK_ROUTE=https://$HTTPSPLITTER_LOG_SINK_ROUTE

    curl -X POST -H "Content-Type: text/plain" --data "how much wood would a woodchuck chuck if that woodchuck could chuck wood" $FULL_HTTP_SOURCE_ROUTE
}

function prepare_ticktock_latest_with_rabbit_binder() {

    wget -O /tmp/ticktock-time-source.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/time-source-rabbit/2.1.3.BUILD-SNAPSHOT/time-source-rabbit-2.1.3.BUILD-SNAPSHOT.jar

    wget -O /tmp/ticktock-log-sink.jar https://repo.spring.io/snapshot/org/springframework/cloud/stream/app/log-sink-rabbit/2.1.4.BUILD-SNAPSHOT/log-sink-rabbit-2.1.4.BUILD-SNAPSHOT.jar

    if [ $6 == "skip-ssl-validation" ]
    then
        cf login -a $1 --skip-ssl-validation -u $2 -p $3 -o $4 -s $5
    else
        cf login -a $1 -u $2 -p $3 -o $4 -s $5
    fi

    cf push -f ./cf-manifests/time-source-manifest.yml

    cf app ticktock-time-source > /tmp/ticktock-time-source-route.txt

    TICKTOCK_TIME_SOURCE_ROUTE=`grep routes /tmp/ticktock-time-source-route.txt | awk '{ print $2 }'`

    FULL_TICKTOCK_TIME_SOURCE_ROUTE=https://$TICKTOCK_TIME_SOURCE_ROUTE

    cf push -f ./cf-manifests/log-sink-manifest.yml

    cf app ticktock-log-sink > /tmp/ticktock-log-sink-route.txt

    TICKTOCK_LOG_SINK_ROUTE=`grep routes /tmp/ticktock-log-sink-route.txt | awk '{ print $2 }'`

    FULL_TICKTOCK_LOG_SINK_ROUTE=https://$TICKTOCK_LOG_SINK_ROUTE
}

#Main script starting

SECONDS=0

echo "Prepare artifacts for jdbc | log testing"

prepare_jdbc_log_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../stream-applications-acceptance-tests

../mvnw clean package -Dtest=JdbcLogAcceptanceTests -Dmaven.test.skip=false -Djdbc.source.route=$FULL_JDBC_SOURCE_ROUTE -Dlog.sink.route=$FULL_JDBC_LOG_SINK_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop jdbc-source-rabbit && cf delete jdbc-source-rabbit -f
cf stop jdbc-log-sink-rabbit && cf delete jdbc-log-sink-rabbit -f

cf logout

rm /tmp/jdbc-source-route.txt
rm /tmp/jdbc-log-sink-route.txt

rm /tmp/jdbc-source-rabbit.jar
rm /tmp/log-sink-rabbit.jar

if [ "$BUILD_RETURN_VALUE" != 0 ]
then
    echo "Early exit due to test failure in ticktock tests"
    duration=$SECONDS

    echo "Total time: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

    exit $BUILD_RETURN_VALUE
fi

echo "Prepare artifacts for http | transform | log testing"

prepare_http_transform_log_with_rabbit_binder $1 $2 $3 $4 $5 $6

pushd ../stream-applications-acceptance-tests

../mvnw clean package -Dtest=HttpTransformLogAcceptanceTests -Dmaven.test.skip=false -Dhttp.source.route=$FULL_HTTP_SOURCE_ROUTE -Dtransform.processor.route=$FULL_TRANSFORM_PROCESSOR_ROUTE -Dlog.sink.route=$FULL_HTTPTRANSFORM_LOG_SINK_ROUTE
BUILD_RETURN_VALUE=$?

popd

cf stop http-source-rabbit && cf delete http-source-rabbit -f
cf stop transform-processor-rabbit && cf delete transform-processor-rabbit -f
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

pushd ../stream-applications-acceptance-tests

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

pushd ../stream-applications-acceptance-tests

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

duration=$SECONDS

echo "Cumulative Build Time Across All Tests: Build took $(($duration / 60)) minutes and $(($duration % 60)) seconds to complete."

exit $BUILD_RETURN_VALUE