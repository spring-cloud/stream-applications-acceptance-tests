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

package org.springframework.cloud.stream.apps.integration.test.kafka.source;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.apps.integration.test.kafka.support.KafkaStreamIntegrationTestSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class KafkaHttpSourceTests extends KafkaStreamIntegrationTestSupport {

	private static int serverPort = findAvailablePort();

	private static WebClient webClient = WebClient.builder().build();

	@Container
	private static final StreamAppContainer source = httpSource(serverPort)
			.withOutputDestination(KafkaHttpSourceTests.class.getSimpleName());

	@Test
	void plaintext() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<HttpStatus> httpStatus = new AtomicReference<>();
		webClient
				.post()
				.uri("http://localhost:" + source.getMappedPort(serverPort))
				.contentType(MediaType.TEXT_PLAIN)
				.body(Mono.just("Hello"), String.class)
				.exchange()
				.subscribe(r -> {
					httpStatus.set(r.statusCode());
					countDownLatch.countDown();
				});
		countDownLatch.await(30, TimeUnit.SECONDS);
		assertThat(httpStatus.get().is2xxSuccessful()).isTrue();
		await().atMost(DEFAULT_DURATION)
				.untilTrue(verifyOutputPayload(source.getOutputDestination(), s -> s.equals("Hello")));
	}

	@Test
	void json() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		webClient
				.post()
				.uri("http://localhost:" + source.getMappedPort(serverPort))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just("{\"Hello\":\"world\"}"), String.class)
				.exchange()
				.subscribe(r -> {
					countDownLatch.countDown();
					assertThat(r.statusCode().is2xxSuccessful()).isTrue();
				});
		countDownLatch.await(30, TimeUnit.SECONDS);
		await().atMost(DEFAULT_DURATION)
				.untilTrue(verifyOutputPayload(source.getOutputDestination(),
						s -> s.equals("{\"Hello\":\"world\"}")));
	}

}
