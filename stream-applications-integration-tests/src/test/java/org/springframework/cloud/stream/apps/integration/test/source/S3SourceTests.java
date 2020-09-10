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
import java.util.function.Consumer;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.github.dockerjava.api.command.CreateContainerCmd;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests;
import org.springframework.cloud.stream.apps.integration.test.LogMatcher;
import org.springframework.cloud.stream.apps.integration.test.LogMatcher.LogListener;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.apps.integration.test.AbstractStreamApplicationTests.AppLog.appLog;
import static org.springframework.cloud.stream.apps.integration.test.FluentMap.fluentMap;
import static org.springframework.cloud.stream.apps.integration.test.LogMatcher.contains;

public class S3SourceTests extends AbstractStreamApplicationTests {

	private static AmazonS3 s3Client;

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final GenericContainer minio = new GenericContainer("minio/minio:RELEASE.2020-09-05T07-14-49Z")
			.withExposedPorts(9000)
			.withEnv("MINIO_ACCESS_KEY", "minio")
			.withEnv("MINIO_SECRET_KEY", "minio123")
			.waitingFor(Wait.forHttp("/minio/health/live"))
			.withCreateContainerCmdModifier(
					(Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd.withHostName("minio"))
			.withLogConsumer(appLog("minio"))
			.withCommand("minio", "server", "/data");

	@BeforeAll
	static void init() {
		AWSCredentials credentials = new BasicAWSCredentials("minio", "minio123");
		ClientConfiguration clientConfiguration = new ClientConfiguration();
		s3Client = AmazonS3ClientBuilder
				.standard()
				.withEndpointConfiguration(
						new AwsClientBuilder.EndpointConfiguration("http://localhost:" + minio.getMappedPort(9000),
								Regions.US_EAST_1.name()))
				.withPathStyleAccessEnabled(true)
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.build();


	}

	@Container
	private final DockerComposeContainer environment = new DockerComposeContainer(
			kafka(),
			resolveTemplate("source/s3-source-tests.yml",
					fluentMap().withEntry("s3.local.dir", resourceAsFile("minio"))
							.withEntry("s3.endpoint.url",
									"http://minio:" + minio.getMappedPort(9000))
							.withEntry("extraHosts", "minio:" + localHostAddress())))
									.withLogConsumer("log-sink", logMatcher)
									.withLogConsumer("s3-source", logMatcher)
									.withLogConsumer("log-sink", appLog("logSink"));

	@Test
	void test() {

		await().atMost(Duration.ofMinutes(2)).untilTrue(logMatcher.withRegex(contains("Started S3Source")).matches());
		LogListener logListener = logMatcher.withRegex(contains("Bart Simpson"));
		s3Client.createBucket("bucket");
		s3Client.putObject(new PutObjectRequest("bucket", "test", resourceAsFile("minio/data")));
		await().atMost(Duration.ofSeconds(30)).untilTrue(logListener.matches());
	}
}
