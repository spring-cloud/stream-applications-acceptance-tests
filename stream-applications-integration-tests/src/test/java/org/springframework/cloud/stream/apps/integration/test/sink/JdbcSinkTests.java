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

package org.springframework.cloud.stream.apps.integration.test.sink;

import java.time.Duration;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

public class JdbcSinkTests extends KafkaStreamIntegrationTestSupport {

	private static int serverPort = findAvailablePort();

	private static JdbcTemplate jdbcTemplate;

	private static WebClient webClient = WebClient.builder().build();

//	@Container
//	private static MySQLContainer mySQL = new MySQLContainer<>(DockerImageName.parse("mysql:5.7"))
//			.withUsername("test")
//			.withPassword("secret")
//			.withExposedPorts(3306)
//			.withNetwork(kafka.getNetwork())
//			.withClasspathResourceMapping("init.sql", "/init.sql", BindMode.READ_ONLY)
//			.withCommand("--init-file", "/init.sql");
//
//	@Container
//	private static StreamApps streamApps = kafkaStreamApps(JdbcSinkTests.class.getSimpleName(), kafka)
//			.withSourceContainer(defaultKafkaContainerFor("http-source")
//					.withEnv("SERVER_PORT", String.valueOf(serverPort))
//					.withExposedPorts(serverPort)
//					.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2))))
//			.withSinkContainer(defaultKafkaContainerFor("jdbc-sink")
//					.withEnv("JDBC_CONSUMER_COLUMNS", "name,city:address.city,street:address.street")
//					.withEnv("JDBC_CONSUMER_TABLE_NAME", "People")
//					.withEnv("SPRING_DATASOURCE_USERNAME", "test")
//					.withEnv("SPRING_DATASOURCE_PASSWORD", "secret")
//					.withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.mariadb.jdbc.Driver")
//					.withEnv("SPRING_DATASOURCE_URL",
//							"jdbc:mysql://" + mySQL.getNetworkAliases().get(0) + ":3306/test"))
//			.build();
//
//	@BeforeAll
//	static void startStreamApps() {
//		HikariDataSource dataSource = new HikariDataSource();
//		dataSource.setDriverClassName("org.mariadb.jdbc.Driver");
//		dataSource.setUsername(mySQL.getUsername());
//		dataSource.setPassword(mySQL.getPassword());
//		dataSource.setJdbcUrl("jdbc:mysql://localhost:" + mySQL.getMappedPort(3306) + "/test");
//		jdbcTemplate = new JdbcTemplate(dataSource);
//		jdbcTemplate.execute("DELETE FROM People");
//	}
//
//	@Test
//	void postData() {
//		String json = "{\"name\":\"My Name\",\"address\":{ \"city\": \"Big City\", \"street\":\"Narrow Alley\"}}";
//		ClientResponse response = webClient
//				.post()
//				.uri("http://localhost:" + streamApps.sourceContainer().getMappedPort(serverPort))
//				.contentType(MediaType.APPLICATION_JSON)
//				.body(Mono.just(json), String.class)
//				.exchange()
//				.block();
//		assertThat(response.statusCode().is2xxSuccessful()).isTrue();
//
//		await().atMost(Duration.ofSeconds(30))
//				.untilAsserted(
//						() -> assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) from People",
//								Integer.class))
//										.isOne());
//		assertThat(jdbcTemplate.queryForObject("SELECT name from People",
//				String.class)).isEqualTo("My Name");
//	}
}
