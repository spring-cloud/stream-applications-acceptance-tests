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
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.FluentMap.fluentMap;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

public class SftpSourceTests extends KafkaStreamIntegrationTestSupport {

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final GenericContainer sftp = new GenericContainer(DockerImageName.parse("atmoz/sftp"))
			.withExposedPorts(22)
			.withCommand("user:pass:::remote")
			.withClasspathResourceMapping("sftp", "/home/user/remote", BindMode.READ_ONLY)
			.withStartupTimeout(Duration.ofMinutes(1));

	private StreamApps streamApps = kafkaStreamApps(SftpSourceTests.class.getSimpleName(), kafka)
			.withSourceContainer(new GenericContainer(defaultKafkaImageFor("sftp-source"))
					.withEnv("SFTP_SUPPLIER_FACTORY_ALLOW_UNKNOWN_KEYS", "true")
					.withEnv("SFTP_SUPPLIER_REMOTE_DIR", "/remote")
					.withEnv("SFTP_SUPPLIER_FACTORY_USERNAME", "user")
					.withEnv("SFTP_SUPPLIER_FACTORY_PASSWORD", "pass")
					.withEnv("SFTP_SUPPLIER_FACTORY_PORT", String.valueOf(sftp.getMappedPort(22)))
					.withEnv("SFTP_SUPPLIER_FACTORY_HOST", localHostAddress()))
			.withSinkContainer(new GenericContainer(defaultKafkaImageFor("log-sink")).withLogConsumer(logMatcher))
			.build();

	// TODO: This fixture supports additional tests with different modes, etc.
	@Test
	void test() {
		startContainer(
				fluentMap().withEntry("FILE_CONSUMER_MODE", "ref"));

		await().atMost(Duration.ofSeconds(30))
				.until(logMatcher.verifies(logListener -> logListener.endsWith("\"/tmp/sftp-supplier/data.txt\"")));
	}

	private void startContainer(Map<String, String> environment) {
		streamApps.sourceContainer().withEnv(environment);
		streamApps.start();
	}

	@AfterEach
	void stop() {
		streamApps.stop();
	}
}
