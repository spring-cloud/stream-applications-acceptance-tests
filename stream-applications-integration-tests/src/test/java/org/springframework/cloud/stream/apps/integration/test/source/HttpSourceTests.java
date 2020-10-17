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

package org.springframework.cloud.stream.apps.integration.test.source;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

public class HttpSourceTests extends KafkaStreamIntegrationTestSupport {

	private static int serverPort = findAvailablePort();

	private static WebClient webClient = WebClient.builder().build();

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final StreamApps streamApps = kafkaStreamApps(HttpSourceTests.class.getSimpleName(), kafka)
			.withSourceContainer(httpSource(serverPort))
			.withSinkContainer(new GenericContainer(defaultKafkaImageFor("log-sink")).withLogConsumer(logMatcher))
			.build();

	@Test
	void plaintext() {
		await().atMost(Duration.ofSeconds(30))
				.until(logMatcher.verifies(log -> log.when(() -> {
					ClientResponse response = webClient
							.post()
							.uri("http://localhost:" + streamApps.sourceContainer().getMappedPort(serverPort))
							.contentType(MediaType.TEXT_PLAIN)
							.body(Mono.just("Hello"), String.class)
							.exchange()
							.block();
					assertThat(response.statusCode().is2xxSuccessful()).isTrue();
				}).endsWith("Hello")));
	}

	@Test
	void json() {
		await().atMost(Duration.ofSeconds(30))
				.until(logMatcher.verifies(log -> log.when(() -> {
					ClientResponse response = webClient
							.post()
							.uri("http://localhost:" + streamApps.sourceContainer().getMappedPort(serverPort))
							.contentType(MediaType.APPLICATION_JSON)
							.body(Mono.just("{\"Hello\":\"world\"}"), String.class)
							.exchange()
							.block();
					assertThat(response.statusCode().is2xxSuccessful()).isTrue();
				}).matchesRegex(".*\\{\"Hello\":\"world\"\\}")));
	}

}
