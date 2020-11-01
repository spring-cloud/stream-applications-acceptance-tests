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

package org.springframework.cloud.stream.apps.integration.test.common;

import java.time.Duration;

public abstract class Configuration {

	public static String VERSION;

	public static final Duration DEFAULT_DURATION = Duration.ofMinutes(1);

	private static final String SPRING_CLOUD_STREAM_APPLICATIONS_VERSION = "spring.cloud.stream.applications.version";

	static {
		VERSION = System.getProperty(SPRING_CLOUD_STREAM_APPLICATIONS_VERSION, "3.1.0-SNAPSHOT");
	}

}
