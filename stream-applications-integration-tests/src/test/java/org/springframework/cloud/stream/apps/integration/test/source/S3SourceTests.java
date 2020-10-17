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
import java.util.Map;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.apps.integration.test.support.KafkaStreamIntegrationTestSupport;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;
import static org.springframework.cloud.stream.app.test.integration.FluentMap.fluentMap;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

public class S3SourceTests extends KafkaStreamIntegrationTestSupport {

	private static AmazonS3 s3Client;

	private static LogMatcher logMatcher = new LogMatcher();

	@Container
	private static final GenericContainer minio = new GenericContainer(
			DockerImageName.parse("minio/minio:RELEASE.2020-09-05T07-14-49Z"))
					.withExposedPorts(9000)
					.withEnv("MINIO_ACCESS_KEY", "minio")
					.withEnv("MINIO_SECRET_KEY", "minio123")
					.waitingFor(Wait.forHttp("/minio/health/live"))
					.withCreateContainerCmdModifier(
							(Consumer<CreateContainerCmd>) createContainerCmd -> createContainerCmd
									.withHostName("minio"))
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

	private StreamApps streamApps = kafkaStreamApps(S3SourceTests.class.getSimpleName(), kafka)
			.withSourceContainer(defaultKafkaContainerFor("s3-source")
					.withEnv("S3_SUPPLIER_REMOTE_DIR", "bucket")
					.withEnv("S3_COMMON_ENDPOINT_URL", "http://" + localHostAddress() + ":" + minio.getMappedPort(9000))
					.withEnv("S3_COMMON_PATH_STYLE_ACCESS", "true")
					.withEnv("CLOUD_AWS_STACK_AUTO", "false")
					.withEnv("CLOUD_AWS_CREDENTIALS_ACCESS_KEY", "minio")
					.withEnv("CLOUD_AWS_CREDENTIALS_SECRET_KEY", "minio123")
					.withEnv("CLOUD_AWS_REGION_STATIC", "us-east-1")
					.withLogConsumer(logMatcher))
			.withSinkContainer(defaultKafkaContainerFor("log-sink").withLogConsumer(logMatcher))
			.build();

	@Test
	void testLines() {
		startContainer(
				fluentMap().withEntry("FILE_CONSUMER_MODE", "lines"));

		await().atMost(Duration.ofMinutes(2)).until(logMatcher.verifies(log -> log.contains("Started S3Source")));
		await().atMost(Duration.ofSeconds(30)).until(logMatcher.verifies(log -> log.when(() -> {
			s3Client.createBucket("bucket");
			s3Client.putObject(new PutObjectRequest("bucket", "test",
					resourceAsFile("minio/data")));
		}).contains("Bart Simpson")));
	}

	@Test
	void testTaskLaunchRequest() {

		startContainer(fluentMap()
				.withEntry("SPRING_CLOUD_FUNCTION_DEFINITION", "s3Supplier|taskLaunchRequestFunction")
				.withEntry("TASK_LAUNCH_REQUEST_ARG_EXPRESSIONS", "filename=payload")
				.withEntry("TASK_LAUNCH_REQUEST_TASK_NAME", "myTask")
				.withEntry("FILE_CONSUMER_MODE", "ref"));

		await().atMost(Duration.ofMinutes(2)).until(logMatcher.verifies(log -> log.contains("Started S3Source")));
		await().atMost(Duration.ofSeconds(30)).until(logMatcher.verifies(log -> log.when(() -> {
			s3Client.createBucket("bucket");
			s3Client.putObject(new PutObjectRequest("bucket", "test",
					resourceAsFile("minio/data")));
		}).endsWith(
				"\\{\"args\":\\[\"filename=/tmp/s3-supplier/test\"\\],\"deploymentProps\":\\{\\},\"name\":\"myTask\"\\}")));
	}

	@Test
	void testListOnly() {

		startContainer(
				fluentMap()
						.withEntry("FILE_CONSUMER_MODE", "ref")
						.withEntry("S3_SUPPLIER_LIST_ONLY", "true"));

		await().atMost(Duration.ofMinutes(2)).until(logMatcher.verifies(log -> log.contains("Started S3Source")));
		await().atMost(Duration.ofSeconds(30)).until(logMatcher.verifies(log -> log.when(() -> {
			s3Client.createBucket("bucket");
			s3Client.putObject(new PutObjectRequest("bucket", "test",
					resourceAsFile("minio/data")));
		}).contains("\"bucketName\":\"bucket\",\"key\":\"test\"")));
	}

	private void startContainer(Map<String, String> environment) {
		streamApps.sourceContainer().withEnv(environment);
		streamApps.start();
	}

	@AfterEach
	void stop() {
		if (s3Client.doesBucketExistV2("bucket")) {
			s3Client.deleteObject("bucket", "test");
			s3Client.deleteBucket("bucket");
		}
		streamApps.stop();
	}
}
