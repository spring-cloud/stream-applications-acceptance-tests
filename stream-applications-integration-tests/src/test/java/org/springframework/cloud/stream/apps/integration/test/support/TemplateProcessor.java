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
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import org.apache.shiro.util.Assert;

import org.springframework.core.io.Resource;

public final class TemplateProcessor {

	private final Path outputDirectory;

	private final Resource template;

	private final Map<?, ?> templateProperties;

	private TemplateProcessor(Builder builder) {
		outputDirectory = builder.outputDirectory;
		template = builder.template;
		templateProperties = builder.templateProperties;
	}

	private static Map<?, ?> globalProperties;

	public File processTemplate() {
		try {
			try (InputStreamReader resourcesTemplateReader = new InputStreamReader(
					Objects.requireNonNull(template.getInputStream()))) {
				Template resourceTemplate = Mustache.compiler().escapeHTML(false).compile(resourcesTemplateReader);
				Path temporaryFile = outputDirectory.resolve(Paths.get(template.getFilename()));
				if (Files.exists(temporaryFile)) {
					Files.delete(temporaryFile);
				}
				Files.createFile(temporaryFile);
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

	private Map<?, ?> addGlobalProperties(Map<?, ?> templateProperties) {
		Map<Object, Object> enriched = new HashMap<>();
		globalProperties.forEach((key, value) -> enriched.put(key.toString(), value.toString()));
		enriched.putAll(templateProperties);
		return enriched;
	}

	public static TemplateProcessor.Builder builder() {
		return TemplateProcessor.withGlobalProperties(Collections.EMPTY_MAP);
	}

	public static TemplateProcessor.Builder withGlobalProperties(Map<?, ?> globalProperties) {
		TemplateProcessor.globalProperties = globalProperties;
		return new TemplateProcessor.Builder();
	}

	public static class Builder {
		private Path outputDirectory;

		private Resource template;

		private Map<?, ?> templateProperties;

		public Builder withTemplateProperties(Map<?, ?> templateProperties) {
			this.templateProperties = templateProperties;
			return this;
		}

		public Builder withTemplate(Resource template) {
			this.template = template;
			return this;
		}

		public Builder withOutputDirectory(Path outputDirectory) {
			this.outputDirectory = outputDirectory;
			return this;
		}

		public TemplateProcessor create() {
			Assert.notNull(this.outputDirectory, "outputDirectory is required");
			Assert.notNull(this.template, "template is required");
			return new TemplateProcessor(this);
		}
	}

}
