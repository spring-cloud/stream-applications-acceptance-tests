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

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests;
import org.springframework.cloud.stream.apps.integration.test.LogMatcher;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.LogMatcher.contains;

public class JdbcSourceTests extends AbstractStreamApplicationTests {

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final DockerComposeContainer environment = new DockerComposeContainer(
			kafka(),
			resolveTemplate("source/jdbc-source-tests.yml",
					Collections.singletonMap("init.sql", resourceAsFile("init.sql"))))
							.withLogConsumer("log-sink", logMatcher)
							.waitingFor("jdbc-source", Wait.forLogMessage(contains("Started JdbcSource"), 1)
									.withStartupTimeout(Duration.ofMinutes(2)));

	@Test
	void test() {
		await().atMost(Duration.ofSeconds(30)).untilTrue(logMatcher.withRegex(contains("Bart Simpson")).matches());
	}
}
