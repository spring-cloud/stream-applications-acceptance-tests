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
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests.AppLog.appLog;
import static org.springframework.cloud.stream.apps.integration.test.FluentMap.fluentMap;

public class TcpSinkTests extends AbstractStreamApplicationTests {

	private static final int port = findAvailablePort();

	private static final int tcpPort = findAvailablePort();

	private static Socket socket;

	private static final AtomicBoolean socketReady = new AtomicBoolean();

	@Container
	private static final DockerComposeContainer environment = new DockerComposeContainer(
			kafka(),
			resolveTemplate("sink/tcp-sink-tests.yml", fluentMap()
					.withEntry("port", port)
					.withEntry("tcp.port", tcpPort)
					.withEntry("tcp.host", localHostAddress())))
							.withLogConsumer("tcp-sink", appLog("tcp-sink"))
							.withExposedService("http-source", port,
									Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

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
		ClientResponse response = webClient()
				.post()
				.uri("http://localhost:" + port)
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
