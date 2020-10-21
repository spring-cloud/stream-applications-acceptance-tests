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

package org.springframework.cloud.stream.apps.integration.test.rabbitmq.support;

import java.time.Duration;

import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamAppContainer;

@Testcontainers
public abstract class RabbitMQStreamIntegrationTestSupport {

	protected static RabbitMQContainer rabbitmq;

	final static Network network = Network.SHARED;

	static {
		rabbitmq = new RabbitMQContainer(DockerImageName.parse("rabbitmq:3.8.9"))
				.withNetwork(network)
				.withExposedPorts(5672);
		rabbitmq.start();
	}

	protected static final String VERSION = "3.0.0-SNAPSHOT";

	protected static final String DOCKER_ORG = "springcloudstream";

	protected static Duration DEFAULT_DURATION = Duration.ofMinutes(1);

	private static String defaultRabbitMQImageFor(String appName) {
		return DOCKER_ORG + "/" + appName + "-rabbit:" + VERSION;
	}

	protected static StreamAppContainer defaultRabbitMQContainerFor(String appName) {
		return new RabbitMQStreamAppContainer(defaultRabbitMQImageFor(appName), rabbitmq);
	}

	protected static StreamAppContainer httpSource(int serverPort) {
		return defaultRabbitMQContainerFor("http-source")
				.withEnv("SERVER_PORT", String.valueOf(serverPort))
				.withExposedPorts(serverPort)
				.waitingFor(Wait.forListeningPort().withStartupTimeout(DEFAULT_DURATION));
	}
}
