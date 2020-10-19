/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.apps.integration.test.kafka.sink;

import java.time.Duration;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApplicationIntegrationTestSupport;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.apps.integration.test.common.Configuration.VERSION;

public class KafkaMongoDBSinkTests extends KafkaStreamApplicationIntegrationTestSupport {

	private static MongoTemplate mongoTemplate;

	@Autowired
	private KafkaTemplate kafkaTemplate;

	@Container
	private static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofMinutes(2));

	private static String mongoConnectionString() {
		return String.format("mongodb://%s:%s/%s", localHostAddress(),
				mongoDBContainer.getMappedPort(27017), "test");
	}

	@Container
	private StreamAppContainer sink = prepackagedKafkaContainerFor("mongodb-sink", VERSION)
			.withEnv("MONGO_DB_CONSUMER_COLLECTION", "test")
			.withEnv("SPRING_DATA_MONGODB_URL", mongoConnectionString());

	@BeforeAll
	static void buildMongoTemplate() {
		MongoDatabaseFactory mongoDatabaseFactory = new SimpleMongoClientDatabaseFactory(
				mongoConnectionString());
		mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
	}

	@Test
	void postData() {
		String json = "{\"name\":\"My Name\",\"address\":{ \"city\": \"Big City\", \"street\":\"Narrow Alley\"}}";
		kafkaTemplate.send(sink.getInputDestination(), json);

		await().atMost(DEFAULT_DURATION).untilAsserted(() -> {
			List<Document> docs = mongoTemplate.findAll(Document.class, "test");
			assertThat(docs).allMatch(document -> document.get("name", String.class).equals("My Name"));
		});
	}

	@AfterAll
	static void cleanUp() {
		mongoDBContainer.close();
	}
}
