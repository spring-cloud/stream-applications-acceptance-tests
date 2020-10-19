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

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;

import static org.springframework.cloud.stream.app.test.integration.FluentMap.fluentMap;

@Testcontainers
public class TikTokBaselineTests extends KafkaStreamIntegrationTestSupport {

	private static Logger logger = LoggerFactory.getLogger(TikTokBaselineTests.class);

	// "MM/dd/yy HH:mm:ss";
	private final static Pattern pattern = Pattern.compile(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}");

	private final static LogMatcher logMatcher = new LogMatcher();

	@Container
	static GenericContainer timeSource = new GenericContainer(
			DockerImageName.parse("springcloudstream/time-source-kafka:3.0.0-SNAPSHOT"))
					.withNetwork(kafka.getNetwork())
					.withEnv(fluentMap().withEntry("SPRING_CLOUD_STREAM_BINDINGS_OUTPUT_DESTINATION", "test-topic")
							.withEntry("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS",
									kafka.getNetworkAliases().get(0) + ":9092"))
					.dependsOn(kafka);

	// @Container
	// static GenericContainer logSink = new GenericContainer(
	// DockerImageName.parse("springcloudstream/log-sink-kafka:3.0.0-SNAPSHOT"))
	// .withNetwork(kafka.getNetwork())
	// .withEnv(fluentMap().withEntry("SPRING_CLOUD_STREAM_BINDINGS_INPUT_DESTINATION",
	// "test-topic")
	// .withEntry("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS",
	// kafka.getNetworkAliases().get(0) + ":9092")
	// .withEntry("SPRING_CLOUD_STREAM_BINDINGS_INPUT_GROUP", "TikTok"))
	// .withLogConsumer(logMatcher)
	// // .withLogConsumer(appLog("log-sink"))
	// .dependsOn(kafka);

	@Test
	void tiktok() {
		// await().atMost(Duration.ofMinutes(2)).until(verifyOutputMessage((Message<String> m) ->
		// pattern.matcher(m.getPayload()).matches()));
	}
}
