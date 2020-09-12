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

package org.springframework.cloud.stream.apps.integration.test;

import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.apps.integration.test.support.AbstractStreamApplicationTests;
import org.springframework.cloud.stream.apps.integration.test.support.LogMatcher;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.support.AbstractStreamApplicationTests.AppLog.appLog;

public class TickTockTests extends AbstractStreamApplicationTests {
	// "MM/dd/yy HH:mm:ss";
	private final Pattern pattern = Pattern.compile(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}");

	private final LogMatcher logMatcher = new LogMatcher();

	@Container
	private final DockerComposeContainer environment = new DockerComposeContainer(
			templateProcessor("tick-tock-tests.yml").processTemplate())
					.withLogConsumer("log-sink", logMatcher)
					.withLogConsumer("log-sink", appLog("log-sink"));

	@Test
	void ticktock() {
		await().atMost(Duration.ofMinutes(2)).until(logMatcher.verifies(log -> log.contains("Started LogSink")));
		await().atMost(Duration.ofSeconds(30)).until(logMatcher.verifies(log -> log.matchesRegex(pattern.pattern())));
	}
}
