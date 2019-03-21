/*
 * Copyright 2018 the original author or authors.
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

public class TickTockLatestAcceptanceTests extends AbstractAcceptanceTests {

    @Test
    public void testTickTockLatestRabbit() {

        String timeSourceUrl = System.getProperty("time.source.route");
        String logSinkUrl = System.getProperty("log.sink.route");

        boolean foundLogs = waitForLogEntry("Time Source", timeSourceUrl, "Started TimeSource");
        if(!foundLogs) {
            fail("Did not find the time source started logging message.");
        }

        foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "Started LogSink");
        if(!foundLogs) {
            fail("Did not find the log sink started logging message.");
        }

        foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "TICKTOCK - TIMESTAMP:");
        if(!foundLogs) {
            fail("Did not find the ticktock messages in log sink");
        }
    }
}
