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
import java.util.UUID;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.SocketUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Testcontainers
public abstract class AbstractStreamApplicationTests {

	protected final static String STREAM_APPS_VERSION = "3.0.0-SNAPSHOT";

	public static final String STREAM_APPS_VERSION_KEY = "stream.apps.version";

	protected static Path tempDir;

	protected static File kafka() {
		return resourceAsFile("compose-kafka.yml");
	}

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
				Path temporaryFile = Files.createFile(tempDir.resolve(Paths.get(templatePath).getFileName()));
				Files.write(temporaryFile,
						resourceTemplate.execute(addGlobalProperties(templateProperties)).getBytes()).toFile();
				temporaryFile.toFile().deleteOnExit();
				return temporaryFile.toFile();
			}
		}
		catch (IOException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static Map<String, Object> addGlobalProperties(Map<String, Object> templateProperties) {
		if (templateProperties.containsKey(STREAM_APPS_VERSION)) {
			return templateProperties;
		}
		Map<String, Object> enriched = new HashMap<>(templateProperties);
		enriched.put(STREAM_APPS_VERSION_KEY, STREAM_APPS_VERSION);
		return enriched;
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
