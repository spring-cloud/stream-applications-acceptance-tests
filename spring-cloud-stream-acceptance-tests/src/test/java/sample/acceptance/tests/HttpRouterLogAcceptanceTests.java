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

public class HttpRouterLogAcceptanceTests extends AbstractAcceptanceTests {

	@Test
	public void testHttpSplitterLog() {

		String httpSourceUrl = System.getProperty("http.source.route");
		String routerSinkUrl = System.getProperty("router.sink.route");
		String logFooSinkUrl = System.getProperty("log.foo.sink.route");
		String logBarSinkUrl = System.getProperty("log.bar.sink.route");

		boolean foundLogs = waitForLogEntry("HTTP Source", httpSourceUrl, "Started HttpSource");
		if (!foundLogs) {
			fail("Did not find the http source started logging message.");
		}

		foundLogs = waitForLogEntry("Router Sink", routerSinkUrl, "Started RouterSink");
		if (!foundLogs) {
			fail("Did not find the router sink started logging message.");
		}

		foundLogs = waitForLogEntry("Log Sink", logFooSinkUrl, "Started LogSink");
		if (!foundLogs) {
			fail("Did not find the log sink started logging message in log-foo.");
		}

		foundLogs = waitForLogEntry("Log Sink", logBarSinkUrl, "Started LogSink");
		if (!foundLogs) {
			fail("Did not find the log sink started logging message in log-bar.");
		}

		RestTemplate restTemplate = new RestTemplate();

		verifyLogs(logFooSinkUrl, ": abcdefgh");
		verifyLogs(logBarSinkUrl, ": ijklmnop");
	}

	void verifyLogs(String appUrl, String textToLookfor) {
		boolean foundMessage = waitForLogEntry("Log Sink", appUrl, textToLookfor);
		if (!foundMessage) {
			fail("Did not find the message - " + textToLookfor + " - in the logs");
		}
	}
}
