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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;

import static org.awaitility.Awaitility.await;

public class SftpSourceTests extends KafkaStreamIntegrationTestSupport {

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final GenericContainer sftp = new GenericContainer(DockerImageName.parse("atmoz/sftp"))
			.withExposedPorts(22)
			.withCommand("user:pass:::remote")
			.withClasspathResourceMapping("sftp", "/home/user/remote", BindMode.READ_ONLY)
			.withStartupTimeout(Duration.ofMinutes(1));

	private StreamAppContainer source = defaultKafkaContainerFor("sftp-source")
			.withEnv("SFTP_SUPPLIER_FACTORY_ALLOW_UNKNOWN_KEYS", "true")
			.withEnv("SFTP_SUPPLIER_REMOTE_DIR", "/remote")
			.withEnv("SFTP_SUPPLIER_FACTORY_USERNAME", "user")
			.withEnv("SFTP_SUPPLIER_FACTORY_PASSWORD", "pass")
			.withEnv("SFTP_SUPPLIER_FACTORY_PORT", String.valueOf(sftp.getMappedPort(22)))
			.withEnv("SFTP_SUPPLIER_FACTORY_HOST", localHostAddress())
			.withOutputDestination(SftpSourceTests.class.getSimpleName());

	// // TODO: This fixture supports additional tests with different modes, etc.
	@Test
	void test() {
		startContainer(Collections.singletonMap("FILE_CONSUMER_MODE", "ref"));

		await().atMost(Duration.ofSeconds(30))
				.untilTrue(verifyOutputMessage(source.getOutputDestination(),
						message -> message.getPayload().equals("\"/tmp/sftp-supplier/data.txt\"")));
	}

	private void startContainer(Map<String, String> environment) {
		source.withEnv(environment);
		source.start();
	}

}
