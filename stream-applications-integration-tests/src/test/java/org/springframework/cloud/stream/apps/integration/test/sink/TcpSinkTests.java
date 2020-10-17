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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

public class TcpSinkTests extends KafkaStreamIntegrationTestSupport {

	private static final int port = findAvailablePort();

	private static final int tcpPort = findAvailablePort();

	private static Socket socket;

	private static final AtomicBoolean socketReady = new AtomicBoolean();

	private static WebClient webClient = WebClient.builder().build();

	@Container
	private static StreamApps streamApps = kafkaStreamApps(TcpSinkTests.class.getSimpleName(), kafka)
			.withSourceContainer(httpSource(port))
			.withSinkContainer(new GenericContainer(defaultKafkaImageFor("tcp-sink"))
					.withEnv("TCP_CONSUMER_HOST", localHostAddress())
					.withEnv("TCP_PORT", String.valueOf(tcpPort))
					.withEnv("TCP_CONSUMER_ENCODER", "CRLF"))
			.build();

	@BeforeAll
	static void startTcpServer() {
		new Thread(() -> {
			try {
				socket = new ServerSocket(tcpPort, 50, InetAddress.getLocalHost()).accept();
				socketReady.set(true);
			}
			catch (IOException exception) {
				exception.printStackTrace();
			}
		}).start();
	}

	@Test
	void postData() throws IOException {
		String text = "Hello, world!";
		ClientResponse response = webClient
				.post()
				.uri("http://localhost:" + streamApps.sourceContainer().getMappedPort(port))
				.contentType(MediaType.TEXT_PLAIN)
				.body(Mono.just(text), String.class)
				.exchange()
				.block();
		assertThat(response.statusCode().is2xxSuccessful()).isTrue();
		await().atMost(Duration.ofSeconds(10)).untilTrue(socketReady);
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		await().atMost(Duration.ofSeconds(10)).until(() -> reader.readLine().equals("Hello, world!"));
	}
}
