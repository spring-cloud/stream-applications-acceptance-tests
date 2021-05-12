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

import static org.junit.Assert.fail;

/**
 * @author Soby Chacko
 */
public class JdbcLogAcceptanceTests extends AbstractAcceptanceTests {

	@Test
	public void testHttpTransformLog() {

		String jdbcSourceUrl = System.getProperty("jdbc.source.route");
		String logSinkUrl = System.getProperty("log.sink.route");

		boolean foundLogs = waitForLogEntry("JDBC Source", jdbcSourceUrl, "Started JdbcSource");
		if (!foundLogs) {
			fail("Did not find the jdbc source started logging message.");
		}

		foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "Started LogSink");
		if (!foundLogs) {
			fail("Did not find the log sink started logging message.");
		}
		// Convert the expected output text to all lowercase due to various DB platforms may write it out in various cases.
		verifyLogs(logSinkUrl, "{\"id\":1,\"name\":\"bob\",\"tag\":null}");
		verifyLogs(logSinkUrl, "{\"id\":2,\"name\":\"jane\",\"tag\":null}");
		verifyLogs(logSinkUrl, "{\"id\":3,\"name\":\"john\",\"tag\":null}");

	}

	void verifyLogs(String appUrl, String textToLookfor) {
		boolean foundMessage = waitForLogEntry("Log Sink", appUrl, true, textToLookfor);
		if (!foundMessage) {
			fail("Did not find the message - " + textToLookfor + " - in the logs");
		}
	}
}
