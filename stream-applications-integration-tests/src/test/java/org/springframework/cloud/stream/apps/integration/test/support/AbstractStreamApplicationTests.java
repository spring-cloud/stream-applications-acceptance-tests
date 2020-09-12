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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.cloud.stream.apps.integration.test.support.FluentMap.fluentMap;

@Testcontainers
public abstract class AbstractStreamApplicationTests {

	private static int kafkaBrokerPort;

	private static TemplateProcessor.Builder templateProcessor;

	static {
		kafkaBrokerPort = findAvailablePort();
		templateProcessor = TemplateProcessor.withGlobalProperties(loadGlobalProperties("test.properties"))
				.withOutputDirectory(initializeTempDir());
		startKafkaContainer();
	}

	protected static TemplateProcessor templateProcessor(String path) {
		return templateProcessor.withTemplate(new ClassPathResource(path)).create();
	}

	protected static TemplateProcessor templateProcessor(String path, Map<?, ?> templateProperties) {
		return templateProcessor.withTemplate(new ClassPathResource(path)).withTemplateProperties(templateProperties)
				.create();
	}

	protected static String localHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static WebClient webClient = WebClient.builder().build();

	protected static WebClient webClient() {
		return webClient;
	}

	protected static File resourceAsFile(String path) {
		try {
			return new ClassPathResource(path).getFile();
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to access resource " + path);
		}
	}

	// Junit TempDir does not work with DockerComposeContainer unless you mount it.
	// Also doesn't work as @BeforeAll in this case.
	static Path initializeTempDir() {
		Path tempDir;
		try {
			Path tempRoot = Paths.get(new ClassPathResource("/").getFile().getAbsolutePath());

			tempDir = Files.createTempDirectory(tempRoot, UUID.randomUUID().toString());
			tempDir.toFile().deleteOnExit();
		}
		catch (Exception e) {
			throw new IllegalStateException(e.getMessage(), e);
		}

		return tempDir;
	}

	protected static int findAvailablePort() {
		return SocketUtils.findAvailableTcpPort(10000, 20000);
	}

	private static Properties loadGlobalProperties(String path) {
		Properties globalProperties = new Properties();
		try {
			globalProperties.load(new ClassPathResource(path).getInputStream());
		}
		catch (IOException exception) {
			throw new IllegalStateException(exception.getMessage(), exception);
		}
		globalProperties.put("kafkaBootStrapServers", localHostAddress() + ":" + kafkaBrokerPort);
		return globalProperties;
	}

	private static void startKafkaContainer() {
		DockerComposeContainer kafkaContainer = new DockerComposeContainer(
				templateProcessor.withTemplateProperties(fluentMap()
						.withEntry("kafkaBrokerPort", kafkaBrokerPort)
						.withEntry("hostAddress", localHostAddress()))
						.withTemplate(new ClassPathResource("compose-kafka-external.yml"))
						.create().processTemplate());
		kafkaContainer
				.waitingFor("kafka", Wait.forListeningPort())
				.start();
	}

	public static class AppLog extends Slf4jLogConsumer {
		public static AppLog appLog(String appName) {
			return new AppLog(appName);
		}

		AppLog(String appName) {
			super(LoggerFactory.getLogger(appName));
		}
	}

}
