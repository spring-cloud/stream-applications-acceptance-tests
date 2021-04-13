/*
 * Copyright 2021-2021 the original author or authors.
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

package sample.acceptance.tests;

import org.junit.Test;

import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.fail;

public class PartitioningAcceptanceTests extends AbstractAcceptanceTests {

	@Test
	public void testHttpSplitterLog() {

		String httpSourceUrl = System.getProperty("http.source.route");
		String splitterProcessorUrl = System.getProperty("splitter.processor.route");
		String log0SinkUrl = System.getProperty("log0.sink.route");
		String log1SinkUrl = System.getProperty("log1.sink.route");

		boolean foundLogs = waitForLogEntry("HTTP Source", httpSourceUrl, "Started HttpSource");
		if (!foundLogs) {
			fail("Did not find the http source started logging message.");
		}

		foundLogs = waitForLogEntry("Splitter Processor", splitterProcessorUrl, "Started SplitterProcessor");
		if (!foundLogs) {
			fail("Did not find the splitter processor started logging message.");
		}

		foundLogs = waitForLogEntry("Log Sink", log0SinkUrl, "Started LogSink");
		if (!foundLogs) {
			fail("Did not find the log sink started logging message in log-0.");
		}

		foundLogs = waitForLogEntry("Log Sink", log1SinkUrl, "Started LogSink");
		if (!foundLogs) {
			fail("Did not find the log sink started logging message in log-1.");
		}

		RestTemplate restTemplate = new RestTemplate();

		try {
			restTemplate.postForObject(
					httpSourceUrl,
					"how much wood would a woodchuck chuck if that woodchuck could chuck wood", String.class);
		}
		catch (Exception e) {
			//pass through
		}

		verifyLogs(log0SinkUrl, ": How");
		verifyLogs(log0SinkUrl, ": chuck");

		verifyLogs(log1SinkUrl, ": much");
		verifyLogs(log1SinkUrl, ": wood");
		verifyLogs(log1SinkUrl, ": a");
		verifyLogs(log1SinkUrl, ": woodchuck");
		verifyLogs(log1SinkUrl, ": if");
		verifyLogs(log1SinkUrl, ": that");
		verifyLogs(log1SinkUrl, ": woodchuck");
		verifyLogs(log1SinkUrl, ": could");
		verifyLogs(log1SinkUrl, ": wood");
	}

	void verifyLogs(String appUrl, String textToLookfor) {
		boolean foundMessage = waitForLogEntry("Log Sink", appUrl, textToLookfor);
		if (!foundMessage) {
			fail("Did not find the message - " + textToLookfor + " - in the logs");
		}
	}
}