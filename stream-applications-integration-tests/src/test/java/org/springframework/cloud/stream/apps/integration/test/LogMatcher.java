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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;

public class LogMatcher implements Consumer<OutputFrame> {
	private static Logger logger = LoggerFactory.getLogger(LogMatcher.class);

	private List<Consumer<String>> listeners = new LinkedList<>();

	public static String contains(String string) {
		return ".*" + string + ".*";
	}

	public static String endsWith(String string) {
		return ".*" + string;
	}

	@Override
	public void accept(OutputFrame outputFrame) {
		listeners.forEach(m -> m.accept(outputFrame.getUtf8String()));
	}

	public LogListener withRegex(String regex) {
		LogListener logListener = new LogListener(regex);
		listeners.add(logListener);
		return logListener;
	}

	public class LogListener implements Consumer<String> {
		private AtomicBoolean matched = new AtomicBoolean();

		private final Pattern pattern;

		LogListener(String regex) {
			pattern = Pattern.compile(regex);
		}

		@Override
		public void accept(String s) {
			logger.trace(this + "matching " + s.trim() + " using pattern " + pattern.pattern());
			if (pattern.matcher(s.trim()).matches()) {
				logger.debug(" MATCHED " + s.trim());
				matched.set(true);
			}
		}

		public AtomicBoolean matches() {
			return matched;
		}
	}
}
