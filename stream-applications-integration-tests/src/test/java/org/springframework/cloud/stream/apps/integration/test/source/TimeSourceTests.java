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
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;

import static org.awaitility.Awaitility.await;

@Testcontainers
public class TimeSourceTests extends KafkaStreamIntegrationTestSupport {

	// "MM/dd/yy HH:mm:ss";
	private final static Pattern pattern = Pattern.compile(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}");

	static LogMatcher logMatcher = new LogMatcher();

	@Container
	static StreamAppContainer timeSource = defaultKafkaContainerFor("time-source")
			.withLogConsumer(logMatcher)
			.withOutputDestination(TimeSourceTests.class.getSimpleName());

	@Test
	void test() {
		await().atMost(Duration.ofMinutes(1)).until(logMatcher.verifies(log -> log.contains("Started TimeSource")));
		await().atMost(Duration.ofMinutes(2)).untilTrue(verifyOutputMessage(timeSource.getOutputDestination(),
				message -> pattern.matcher((String) message.getPayload()).matches()));
	}
}
