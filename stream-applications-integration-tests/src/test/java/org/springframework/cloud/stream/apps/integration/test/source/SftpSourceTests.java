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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.apps.integration.test.support.AbstractStreamApplicationTests;
import org.springframework.cloud.stream.apps.integration.test.support.LogMatcher;
import org.springframework.cloud.stream.apps.integration.test.support.TemplateProcessor;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.support.AbstractStreamApplicationTests.AppLog.appLog;
import static org.springframework.cloud.stream.apps.integration.test.support.FluentMap.fluentMap;

public class SftpSourceTests extends AbstractStreamApplicationTests {

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final GenericContainer sftp = (GenericContainer) new GenericContainer("atmoz/sftp")
			.withExposedPorts(22)
			.withCommand("user:pass:::remote")
			.withClasspathResourceMapping("sftp", "/home/user/remote", BindMode.READ_ONLY)
			.withStartupTimeout(Duration.ofMinutes(1));

	private DockerComposeContainer environment;

	@Test
	void test() {
		startContainer(templateProcessor("source/sftp-source-tests.yml",
				fluentMap().withEntry("sftpPort", sftp.getMappedPort(22))
						.withEntry("functionDefinition", "sftpSupplier")
						.withEntry("consumerMode", "ref")
						.withEntry("listOnly", false)
						.withEntry("sftpHost", localHostAddress())));
		await().atMost(Duration.ofSeconds(30))
				.until(logMatcher.verifies(logListener -> logListener.endsWith("\"/tmp/sftp-supplier/data.txt\"")));
	}

	private void startContainer(TemplateProcessor templateProcessor) {
		environment = new DockerComposeContainer(
				templateProcessor.processTemplate())
						.withLogConsumer("log-sink", logMatcher)
						.withLogConsumer("sftp-source", logMatcher)
						.withLogConsumer("log-sink", appLog("log-sink"));
		environment.start();
	}

	@AfterEach
	void stop() {
		environment.stop();
	}
}
