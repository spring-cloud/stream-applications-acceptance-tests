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

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.springframework.cloud.stream.apps.integration.test.FluentMap.fluentMap;

@Testcontainers
public abstract class AbstractStreamApplicationTests {

	private static Properties globalProperties = loadGlobalProperties("test.properties");

	private static int kafkaBrokerPort;

	static {
		kafkaBrokerPort = findAvailablePort();
		startKafkaContainer();
	}

	protected static Path tempDir;

	protected static File resourceAsFile(String path) {
		try {
			return new ClassPathResource(path).getFile();
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to access resource " + path);
		}
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

	// Junit TempDir does not work with DockerComposeContainer unless you mount it.
	// Also doesn't work as @BeforeAll in this case.
	static void initializeTempDir() throws IOException {
		Path tempRoot = Paths.get(new ClassPathResource("/").getFile().getAbsolutePath());
		if (tempDir == null) {
			tempDir = Files.createTempDirectory(tempRoot, UUID.randomUUID().toString());
			tempDir.toFile().deleteOnExit();
		}
	}

	protected static int findAvailablePort() {
		return SocketUtils.findAvailableTcpPort(10000, 20000);
	}

	protected static File resolveTemplate(String templatePath, Map<String, Object> templateProperties) {
		try {
			initializeTempDir();
			try (InputStreamReader resourcesTemplateReader = new InputStreamReader(
					Objects.requireNonNull(new ClassPathResource(templatePath).getInputStream()))) {
				Template resourceTemplate = Mustache.compiler().escapeHTML(false).compile(resourcesTemplateReader);
				Path temporaryFile = tempDir.resolve(Paths.get(templatePath).getFileName());
				if (!Files.exists(temporaryFile)) {
					Files.createFile(temporaryFile);

					Files.write(temporaryFile,
							resourceTemplate.execute(addGlobalProperties(templateProperties)).getBytes()).toFile();
					temporaryFile.toFile().deleteOnExit();
				}
				return temporaryFile.toFile();
			}
		}
		catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static Map<String, Object> addGlobalProperties(Map<String, Object> templateProperties) {
		Map<String, Object> enriched = new HashMap<>();
		globalProperties.forEach((key, value) -> enriched.put(key.toString(), value.toString()));
		enriched.putAll(templateProperties);
		enriched.put("kafkaBootStrapServers", localHostAddress() + ":" + kafkaBrokerPort);
		return enriched;
	}

	private static Properties loadGlobalProperties(String path) {
		Properties globalProperties = new Properties();
		try {
			globalProperties.load(new ClassPathResource(path).getInputStream());
		}
		catch (IOException exception) {
			throw new IllegalStateException(exception.getMessage(), exception);
		}
		return globalProperties;
	}

	private static void startKafkaContainer() {
		DockerComposeContainer kafkaContainer = new DockerComposeContainer(
				resolveTemplate("compose-kafka-external.yml",
						fluentMap().withEntry("kafkaBrokerPort", kafkaBrokerPort)
								.withEntry("hostAddress", localHostAddress())));
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
