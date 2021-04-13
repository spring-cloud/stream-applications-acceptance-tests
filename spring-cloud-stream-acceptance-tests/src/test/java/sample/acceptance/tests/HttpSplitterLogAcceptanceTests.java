/*
 * Copyright 2019 the original author or authors.
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

/**
 * @author Soby Chacko
 */
public class HttpSplitterLogAcceptanceTests extends AbstractAcceptanceTests {

	@Test
	public void testHttpSplitterLog() {

		String httpSourceUrl = System.getProperty("http.source.route");
		String splitterProcessorUrl = System.getProperty("splitter.processor.route");
		String logSinkUrl = System.getProperty("log.sink.route");


		boolean foundLogs = waitForLogEntry("HTTP Source", httpSourceUrl, "Started HttpSource");
		if(!foundLogs) {
			fail("Did not find the http source started logging message.");
		}

		foundLogs = waitForLogEntry("Splitter Processor", splitterProcessorUrl, "Started SplitterProcessor");
		if(!foundLogs) {
			fail("Did not find the splitter processor started logging message.");
		}

		foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "Started LogSink");
		if(!foundLogs) {
			fail("Did not find the log sink started logging message.");
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
		verifyLogs(logSinkUrl, ": how");
		verifyLogs(logSinkUrl, ": much");
		verifyLogs(logSinkUrl, ": wood");
		verifyLogs(logSinkUrl, ": a");
		verifyLogs(logSinkUrl, ": woodchuck");
		verifyLogs(logSinkUrl, ": chuck");
		verifyLogs(logSinkUrl, ": if");
		verifyLogs(logSinkUrl, ": that");
		verifyLogs(logSinkUrl, ": woodchuck");
		verifyLogs(logSinkUrl, ": could");
		verifyLogs(logSinkUrl, ": chuck");
		verifyLogs(logSinkUrl, ": wood");
	}

	void verifyLogs(String appUrl, String textToLookfor) {
		boolean foundMessage = waitForLogEntry("Log Sink", appUrl, textToLookfor);
		if (!foundMessage) {
			fail("Did not find the message - " + textToLookfor + " - in the logs");
		}
	}
}
