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
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests;
import org.springframework.cloud.stream.apps.integration.test.LogMatcher;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests.AppLog.appLog;
import static org.springframework.cloud.stream.apps.integration.test.LogMatcher.endsWith;

public class HttpSourceTests extends AbstractStreamApplicationTests {

	private static int port = findAvailablePort();

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final DockerComposeContainer environment = new DockerComposeContainer(
			kafka(),
			resolveTemplate("source/http-source-tests.yml", Collections.singletonMap("port", port)))
					.withLogConsumer("log-sink", appLog("log-sink"))
					.withLogConsumer("log-sink", logMatcher)
					.withExposedService("http-source", port,
							Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

	@Test
	void plaintext() {
		ClientResponse response = webClient()
				.post()
				.uri("http://localhost:" + port)
				.contentType(MediaType.TEXT_PLAIN)
				.body(Mono.just("Hello"), String.class)
				.exchange()
				.block();
		assertThat(response.statusCode().is2xxSuccessful()).isTrue();

		await().atMost(Duration.ofSeconds(30))
				.untilTrue(logMatcher.withRegex(endsWith("Hello")).matches());
	}

	@Test
	void json() {
		ClientResponse response = webClient()
				.post()
				.uri("http://localhost:" + port)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just("{\"Hello\":\"world\"}"), String.class)
				.exchange()
				.block();
		assertThat(response.statusCode().is2xxSuccessful()).isTrue();
		await().atMost(Duration.ofSeconds(30))
				.untilTrue(logMatcher.withRegex(".*\\{\"Hello\":\"world\"\\}").matches());
	}
}
