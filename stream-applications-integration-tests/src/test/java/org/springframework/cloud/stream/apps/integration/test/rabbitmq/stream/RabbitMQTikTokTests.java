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

package org.springframework.cloud.stream.apps.integration.test.rabbitmq.stream;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamApplicationIntegrationTestSupport;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.rabbitmq.RabbitMQStreamApps.rabbitMQStreamApps;
import static org.springframework.cloud.stream.apps.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.apps.integration.test.common.Configuration.VERSION;

public class RabbitMQTikTokTests extends RabbitMQStreamApplicationIntegrationTestSupport {

	private static LogMatcher logMatcher = LogMatcher.matchesRegex(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}")
			.times(3);

	@Container
	private static final StreamApps streamApp = rabbitMQStreamApps(RabbitMQTikTokTests.class.getSimpleName(), rabbitmq)
			.withSourceContainer(prepackagedRabbitMQContainerFor("time-source", VERSION))
			.withSinkContainer(prepackagedRabbitMQContainerFor("log-sink", VERSION)
					.withLogConsumer(logMatcher))
			.build();

	@Test
	void test() {
		await().atMost(DEFAULT_DURATION).until(logMatcher.matches());
	}
}
