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

package org.springframework.cloud.stream.apps.integration.test.sink;

import java.time.Duration;
import java.util.List;

import org.bson.Document;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

public class MongoDBSinkTests extends KafkaStreamIntegrationTestSupport {

	private static int serverPort = findAvailablePort();

	private static MongoTemplate mongoTemplate;

	private static WebClient webClient = WebClient.builder().build();

	@Container
	private static MongoDBContainer mongoDBContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"))
			.withExposedPorts(27017)
			.withStartupTimeout(Duration.ofMinutes(2));

	private static String mongoConnectionString() {
		return String.format("mongodb://%s:%s/%s", localHostAddress(), mongoDBContainer.getMappedPort(27017), "test");
	}

	@Container
	private StreamApps streamApps = kafkaStreamApps(MongoDBSinkTests.class.getSimpleName(), kafka)
			.withSourceContainer(httpSource(serverPort))
			.withSinkContainer(defaultKafkaContainerFor("mongodb-sink")
					.withEnv("MONGO_DB_CONSUMER_COLLECTION", "test")
					.withEnv("SPRING_DATA_MONGODB_URL", mongoConnectionString()))
			.build();

	@BeforeAll
	static void buildMongoTemplate() {
		MongoDatabaseFactory mongoDatabaseFactory = new SimpleMongoClientDatabaseFactory(
				mongoConnectionString());
		mongoTemplate = new MongoTemplate(mongoDatabaseFactory);
	}

	@Test
	void postData() {
		String json = "{\"name\":\"My Name\",\"address\":{ \"city\": \"Big City\", \"street\": \"Narrow Alley\"}}";
		ClientResponse response = webClient
				.post()
				.uri("http://localhost:" + streamApps.sourceContainer().getMappedPort(serverPort))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(json), String.class)
				.exchange()
				.block(Duration.ofSeconds(30));
		assertThat(response.statusCode().is2xxSuccessful()).isTrue();
		List<Document> docs = mongoTemplate.findAll(Document.class, "test");
		assertThat(docs).allMatch(document -> document.get("name", String.class).equals("My Name"));
	}
}
