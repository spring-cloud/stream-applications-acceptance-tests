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

package org.springframework.cloud.stream.apps.integration.test.kafka.source;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.apps.integration.test.kafka.support.KafkaStreamIntegrationTestSupport;

import static org.awaitility.Awaitility.await;

public class KafkaJdbcSourceTests extends KafkaStreamIntegrationTestSupport {

	@Container
	public static MySQLContainer mySQL = new MySQLContainer<>(DockerImageName.parse("mysql:5.7"))
			.withUsername("test")
			.withPassword("secret")
			.withExposedPorts(3306)
			.withNetwork(kafka.getNetwork())
			.withClasspathResourceMapping("init.sql", "/init.sql", BindMode.READ_ONLY)
			.withCommand("--init-file", "/init.sql");

	private static final StreamAppContainer source = defaultKafkaContainerFor("jdbc-source")
			.withEnv("JDBC_SUPPLIER_QUERY", "SELECT * FROM People WHERE deleted='N'")
			.withEnv("JDBC_SUPPLIER_UPDATE", "UPDATE People SET deleted='Y' WHERE id=:id")
			.withEnv("SPRING_DATASOURCE_USERNAME", "test")
			.withEnv("SPRING_DATASOURCE_PASSWORD", "secret")
			.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver")
			.withEnv("SPRING_DATASOURCE_URL",
					"jdbc:mysql://" + mySQL.getNetworkAliases().get(0) + ":3306/test")
			.withOutputDestination(KafkaJdbcSourceTests.class.getSimpleName());

	@BeforeAll
	static void startSource() {
		await().atMost(DEFAULT_DURATION).until(() -> mySQL.isRunning());
		source.start();
	}

	@Test
	void test() {
		await().atMost(DEFAULT_DURATION).untilTrue(verifyOutputPayload(source.getOutputDestination(),
				(String s) -> s.contains("Bart Simpson")));
	}
}
