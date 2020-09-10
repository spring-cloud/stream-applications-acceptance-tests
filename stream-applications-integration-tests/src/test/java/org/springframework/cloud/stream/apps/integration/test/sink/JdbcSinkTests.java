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
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import reactor.core.publisher.Mono;

import org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.ClientResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests.AppLog.appLog;
import static org.springframework.cloud.stream.apps.integration.test.FluentMap.fluentMap;

public class JdbcSinkTests extends AbstractStreamApplicationTests {

	private static int port = findAvailablePort();

	private static JdbcTemplate jdbcTemplate;

	@Container
	private static MariaDBContainer mariadbContainer = (MariaDBContainer) new MariaDBContainer()
			.withDatabaseName("test")
			.withPassword("password")
			.withUsername("user")
			.withInitScript("init.sql")
			.withExposedPorts(3306)
			.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

	@Container
	private DockerComposeContainer environment = new DockerComposeContainer(
			kafka(),
			resolveTemplate("sink/jdbc-sink-tests.yml", fluentMap()
					.withEntry("jdbc.url",
							mariadbContainer.getJdbcUrl().replace("localhost",
									localHostAddress()))
					.withEntry("user", mariadbContainer.getUsername())
					.withEntry("password", mariadbContainer.getPassword())
					.withEntry("port", port)))
							.withLogConsumer("jdbc-sink", appLog("jdbc-sink"))
							.withExposedService("http-source", port,
									Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(2)));

	@BeforeAll
	static void buildJdbcTemplate() {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName(mariadbContainer.getDriverClassName());
		dataSource.setUsername(mariadbContainer.getUsername());
		dataSource.setPassword(mariadbContainer.getPassword());
		dataSource.setJdbcUrl(mariadbContainer.getJdbcUrl());
		jdbcTemplate = new JdbcTemplate(dataSource);
		jdbcTemplate.execute("DELETE FROM People");
	}

	@Test
	void postData() {
		String json = "{\"name\":\"My Name\",\"address\":{ \"city\": \"Big City\", \"street\": \"Narrow Alley\"}}";
		ClientResponse response = webClient()
				.post()
				.uri("http://localhost:" + port)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just(json), String.class)
				.exchange()
				.block();
		assertThat(response.statusCode().is2xxSuccessful()).isTrue();

		await().atMost(Duration.ofSeconds(30))
				.untilAsserted(
						() -> assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) from People", Integer.class))
								.isOne());
		assertThat(jdbcTemplate.queryForObject("SELECT name from People", String.class)).isEqualTo("My Name");
	}
}
