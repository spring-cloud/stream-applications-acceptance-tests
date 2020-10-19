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

package org.springframework.cloud.stream.apps.integration.test.support;

import java.time.Duration;

import org.testcontainers.containers.wait.strategy.Wait;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.kafka.AbstractKafkaStreamApplicationIntegrationTests;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamAppContainer;

public abstract class KafkaStreamIntegrationTestSupport extends AbstractKafkaStreamApplicationIntegrationTests {

	protected static final String VERSION = "3.0.0-SNAPSHOT";

	protected static final String DOCKER_ORG = "springcloudstream";

	private static String defaultKafkaImageFor(String appName) {
		return DOCKER_ORG + "/" + appName + "-kafka:" + VERSION;
	}

	protected static StreamAppContainer defaultKafkaContainerFor(String appName) {
		return new KafkaStreamAppContainer(defaultKafkaImageFor(appName), kafka);
	}

	protected static StreamAppContainer httpSource(int serverPort) {
		return defaultKafkaContainerFor("http-source")
				.withEnv("SERVER_PORT", String.valueOf(serverPort))
				.withExposedPorts(serverPort)
				.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));
	}
}
