
#!/bin/bash

pushd () {
    command pushd "$@" > /dev/null
}

popd () {
    command popd "$@" > /dev/null
}

function prepare_jdbc_source_with_kafka_and_rabbit_binders() {
    wget -O /tmp/jdbc-source-kafka-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/sample-jdbc-source/0.0.1-SNAPSHOT/sample-jdbc-source-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/jdbc-source-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/sample-jdbc-source/0.0.1-SNAPSHOT/sample-jdbc-source-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_jdbc_sink_with_kafka_and_rabbit_binders() {
    wget -O /tmp/jdbc-sink-kafka-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/sample-jdbc-sink/0.0.1-SNAPSHOT/sample-jdbc-sink-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/jdbc-sink-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/sample-jdbc-sink/0.0.1-SNAPSHOT/sample-jdbc-sink-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_dynamic_source_with_kafka_and_rabbit_binders() {
    wget -O /tmp/dynamic-destination-source-kafka-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/dynamic-destination-source/0.0.1-SNAPSHOT/dynamic-destination-source-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/dynamic-destination-source-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/dynamic-destination-source/0.0.1-SNAPSHOT/dynamic-destination-source-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_multi_binder_with_kafka_rabbit() {
    wget -O /tmp/multibinder-kafka-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/multibinder-kafka-rabbit/0.0.1-SNAPSHOT/multibinder-kafka-rabbit-0.0.1-SNAPSHOT.jar
}

function prepare_multi_binder_with_two_kafka_clusters() {
    wget -O /tmp/multibinder-two-kafka-clusters-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/multibinder-two-kafka-clusters/0.0.1-SNAPSHOT/multibinder-two-kafka-clusters-0.0.1-SNAPSHOT.jar
}

function prepare_kafka_streams_word_count() {
    wget -O /tmp/kafka-streams-word-count-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/kafka-streams-word-count/0.0.1-SNAPSHOT/kafka-streams-word-count-0.0.1-SNAPSHOT.jar
}

function prepare_streamlistener_basic_with_kafka_rabbit_binders() {
    wget -O /tmp/streamlistener-basic-kafka-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/streamlistener-basic/0.0.1-SNAPSHOT/streamlistener-basic-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/streamlistener-basic-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/streamlistener-basic/0.0.1-SNAPSHOT/streamlistener-basic-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_reactive_processor_with_kafka_rabbit_binders() {
    wget -O /tmp/reactive-processor-kafka-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/reactive-processor/0.0.1-SNAPSHOT/reactive-processor-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/reactive-processor-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/reactive-processor/0.0.1-SNAPSHOT/reactive-processor-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_sensor_average_reactive_with_kafka_rabbit_binders() {
    wget -O /tmp/sensor-average-reactive-kafka-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/sensor-average-reactive/0.0.1-SNAPSHOT/sensor-average-reactive-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/sensor-average-reactive-rabbit-sample.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/sensor-average-reactive/0.0.1-SNAPSHOT/sensor-average-reactive-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_schema_registry_vanilla_with_kafka_rabbit_binders() {
    wget -O /tmp/schema-registry-vanilla-registry-kafka.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-server/0.0.1-SNAPSHOT/schema-registry-vanilla-server-0.0.1-SNAPSHOT.jar
    wget -O /tmp/schema-registry-vanilla-consumer-kafka.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-consumer/0.0.1-SNAPSHOT/schema-registry-vanilla-consumer-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/schema-registry-vanilla-producer1-kafka.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-producer1/0.0.1-SNAPSHOT/schema-registry-vanilla-producer1-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/schema-registry-vanilla-producer2-kafka.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-producer2/0.0.1-SNAPSHOT/schema-registry-vanilla-producer2-0.0.1-SNAPSHOT-kafka.jar

    wget -O /tmp/schema-registry-vanilla-registry-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-server/0.0.1-SNAPSHOT/schema-registry-vanilla-server-0.0.1-SNAPSHOT.jar
    wget -O /tmp/schema-registry-vanilla-consumer-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-consumer/0.0.1-SNAPSHOT/schema-registry-vanilla-consumer-0.0.1-SNAPSHOT-rabbit.jar
    wget -O /tmp/schema-registry-vanilla-producer1-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-producer1/0.0.1-SNAPSHOT/schema-registry-vanilla-producer1-0.0.1-SNAPSHOT-rabbit.jar
    wget -O /tmp/schema-registry-vanilla-producer2-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/schema-registry-vanilla-producer2/0.0.1-SNAPSHOT/schema-registry-vanilla-producer2-0.0.1-SNAPSHOT-rabbit.jar
}

function prepare_partitioning_with_kafka_rabbit_binders() {
    wget -O /tmp/partitioning-producer-kafka.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/partitioning-producer/0.0.1-SNAPSHOT/partitioning-producer-0.0.1-SNAPSHOT-kafka.jar
    wget -O /tmp/partitioning-consumer-kafka.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/partitioning-consumer-kafka/0.0.1-SNAPSHOT/partitioning-consumer-kafka-0.0.1-SNAPSHOT.jar

    wget -O /tmp/partitioning-producer-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/partitioning-producer/0.0.1-SNAPSHOT/partitioning-producer-0.0.1-SNAPSHOT-rabbit.jar
    wget -O /tmp/partitioning-consumer-rabbit.jar http://repo.spring.io/libs-snapshot-local/io/spring/cloud/stream/sample/partitioning-consumer-rabbit/0.0.1-SNAPSHOT/partitioning-consumer-rabbit-0.0.1-SNAPSHOT.jar
}

#Main script starting

echo "Prepare artifacts for testing"

prepare_jdbc_source_with_kafka_and_rabbit_binders
prepare_jdbc_sink_with_kafka_and_rabbit_binders
prepare_dynamic_source_with_kafka_and_rabbit_binders
prepare_multi_binder_with_kafka_rabbit
prepare_multi_binder_with_two_kafka_clusters
prepare_streamlistener_basic_with_kafka_rabbit_binders
prepare_reactive_processor_with_kafka_rabbit_binders
prepare_sensor_average_reactive_with_kafka_rabbit_binders
prepare_kafka_streams_word_count

prepare_schema_registry_vanilla_with_kafka_rabbit_binders

prepare_partitioning_with_kafka_rabbit_binders

echo "Starting components in docker containers..."

docker-compose up -d

echo "Running tests"

./mvnw clean package -Dmaven.test.skip=false
BUILD_RETURN_VALUE=$?

docker-compose down

# Post cleanup

rm /tmp/jdbc-source-kafka-sample.jar
rm /tmp/jdbc-source-rabbit-sample.jar
rm /tmp/jdbc-sink-kafka-sample.jar
rm /tmp/jdbc-sink-rabbit-sample.jar
rm /tmp/dynamic-destination-source-kafka-sample.jar
rm /tmp/dynamic-destination-source-rabbit-sample.jar
rm /tmp/multibinder-kafka-rabbit-sample.jar
rm /tmp/multibinder-two-kafka-clusters-sample.jar
rm /tmp/kafka-streams-word-count-sample.jar
rm /tmp/streamlistener-basic-kafka-sample.jar
rm /tmp/streamlistener-basic-rabbit-sample.jar
rm /tmp/reactive-processor-kafka-sample.jar
rm /tmp/reactive-processor-rabbit-sample.jar
rm /tmp/sensor-average-reactive-kafka-sample.jar
rm /tmp/sensor-average-reactive-rabbit-sample.jar

rm /tmp/schema-registry-vanilla-registry-kafka.jar
rm /tmp/schema-registry-vanilla-consumer-kafka.jar
rm /tmp/schema-registry-vanilla-producer1-kafka.jar
rm /tmp/schema-registry-vanilla-producer2-kafka.jar
rm /tmp/schema-registry-vanilla-registry-rabbit.jar
rm /tmp/schema-registry-vanilla-consumer-rabbit.jar
rm /tmp/schema-registry-vanilla-producer1-rabbit.jar
rm /tmp/schema-registry-vanilla-producer2-rabbit.jar

rm /tmp/partitioning-producer-kafka.jar
rm /tmp/partitioning-consumer-kafka.jar

rm /tmp/partitioning-producer-rabbit.jar
rm /tmp/partitioning-consumer-rabbit.jar

exit $BUILD_RETURN_VALUE