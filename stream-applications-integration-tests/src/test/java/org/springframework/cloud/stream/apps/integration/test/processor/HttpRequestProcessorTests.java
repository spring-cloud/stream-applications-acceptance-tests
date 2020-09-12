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

package org.springframework.cloud.stream.apps.integration.test.processor;

import java.net.InetAddress;
import java.time.Duration;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.apps.integration.test.support.AbstractStreamApplicationTests;
import org.springframework.cloud.stream.apps.integration.test.support.LogMatcher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.support.AbstractStreamApplicationTests.AppLog.appLog;
import static org.springframework.cloud.stream.apps.integration.test.support.FluentMap.fluentMap;

public class HttpRequestProcessorTests extends AbstractStreamApplicationTests {
	private static MockWebServer server = new MockWebServer();

	private static int serverPort = findAvailablePort();

	private static String url = "http://" + localHostAddress() + ":" + serverPort;

	private static int sourcePort = findAvailablePort();

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final DockerComposeContainer environment = new DockerComposeContainer(
			templateProcessor("processor/http-request-processor-tests.yml", fluentMap()
					.withEntry("port", sourcePort)
					.withEntry("url", url)).processTemplate())
							.withLogConsumer("log-sink", appLog("log-sink"))
							.withLogConsumer("log-sink", logMatcher)
							.withExposedService("http-source", sourcePort,
									Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

	@BeforeAll
	static void startServer() throws Exception {
		server.start(InetAddress.getLocalHost(), serverPort);
	}

	@Test
	void get() {
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.setBody("{\"response\":\"" + recordedRequest.getBody().readUtf8() + "\"}")
						.setResponseCode(HttpStatus.OK.value());
			}
		});
		ClientResponse response = webClient()
				.post()
				.uri("http://localhost:" + sourcePort)
				.contentType(MediaType.TEXT_PLAIN)
				.body(Mono.just("ping"), String.class)
				.exchange()
				.block();
		assertThat(response.statusCode().is2xxSuccessful()).isTrue();

		await().atMost(Duration.ofSeconds(30))
				.untilTrue(logMatcher.withRegex(".*\\{\"response\":\"ping\"\\}").matches());
	}
}
