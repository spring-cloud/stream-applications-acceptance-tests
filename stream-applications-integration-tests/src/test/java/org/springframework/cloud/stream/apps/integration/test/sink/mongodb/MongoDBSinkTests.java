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

package org.springframework.cloud.stream.apps.integration.test.sink.mongodb;

import java.time.Duration;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.TestTopicSender;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.common.Configuration.DEFAULT_DURATION;

abstract class MongoDBSinkTests {

	private static MongoTemplate mongoTemplate;

	@Autowired
	private TestTopicSender testTopicSender;

	private static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofMinutes(2));

	private static String mongoConnectionString() {
		return String.format("mongodb://%s:%s/%s", StreamAppContainerTestUtils.localHostAddress(),
				mongoDBContainer.getMappedPort(27017), "test");
	}

	private static StreamAppContainer sink;

	protected static void configureSink(StreamAppContainer baseContainer) {
		mongoDBContainer.start();
		sink = baseContainer
				.withEnv("MONGODB_CONSUMER_COLLECTION", "test")
				.withEnv("SPRING_DATA_MONGODB_URL", mongoConnectionString());
		sink.start();
		buildMongoTemplate();
	}

	static void buildMongoTemplate() {
		mongoDBContainer.start();
		MongoDatabaseFactory mongoDatabaseFactory = new SimpleMongoClientDatabaseFactory(
				mongoConnectionString());
		mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
	}

	@Test
	void postData() {
		String json = "{\"name\":\"My Name\",\"address\":{ \"city\": \"Big City\", \"street\":\"Narrow Alley\"}}";
		testTopicSender.send(sink.getInputDestination(), json);

		await().atMost(DEFAULT_DURATION).untilAsserted(() -> {
			List<Document> docs = mongoTemplate.findAll(Document.class, "test");
			assertThat(docs).allMatch(document -> document.get("name", String.class).equals("My Name"));
		});
	}

	@AfterAll
	static void cleanUp() {
		mongoDBContainer.close();
		sink.stop();
	}

}
