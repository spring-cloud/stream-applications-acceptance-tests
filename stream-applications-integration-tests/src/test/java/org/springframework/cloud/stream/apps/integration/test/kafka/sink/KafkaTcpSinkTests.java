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
import org.testcontainers.junit.jupiter.Container;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.apps.integration.test.kafka.support.KafkaStreamIntegrationTestSupport;
import org.springframework.kafka.core.KafkaTemplate;

import static org.awaitility.Awaitility.await;

public class KafkaTcpSinkTests extends KafkaStreamIntegrationTestSupport {

	private static final int tcpPort = findAvailablePort();

	private static Socket socket;

	private static final AtomicBoolean socketReady = new AtomicBoolean();

	@Autowired
	private KafkaTemplate kafkaTemplate;

	@Container
	private static StreamAppContainer sink = defaultKafkaContainerFor("tcp-sink")
			.withInputDestination(KafkaTcpSinkTests.class.getSimpleName())
			.withEnv("TCP_CONSUMER_HOST", localHostAddress())
			.withEnv("TCP_PORT", String.valueOf(tcpPort))
			.withEnv("TCP_CONSUMER_ENCODER", "CRLF");

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
		// Sink will not connect until it receives a message.
		String text = "Hello, world!";
		kafkaTemplate.send(sink.getInputDestination(), text);

		await().atMost(DEFAULT_DURATION).untilTrue(socketReady);
		BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		await().atMost(Duration.ofSeconds(10)).until(() -> reader.readLine().equals(text));
	}
}
