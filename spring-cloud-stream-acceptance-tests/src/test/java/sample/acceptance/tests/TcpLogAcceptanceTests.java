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

import java.io.IOException;
import java.net.Socket;

import javax.net.SocketFactory;

import org.junit.Test;

import static org.junit.Assert.fail;

public class TcpLogAcceptanceTests extends AbstractAcceptanceTests {

	@Test
	public void testTcpSourceToLogSink() throws IOException {
		String tcpSourceUrl = System.getProperty("tcp.source.route");
		String logSinkUrl = System.getProperty("log.sink.route");
		String tcpSourceIp = System.getProperty("tcp.source.ip");

		boolean foundLogs = waitForLogEntry("TCP Source", tcpSourceUrl, "Started TcpSource");
		if (!foundLogs) {
			fail("Did not find the time source started logging message.");
		}

		foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "Started LogSink");
		if (!foundLogs) {
			fail("Did not find the log sink started logging message.");
		}

		logger.info("Trying to open socket connection to: [" + tcpSourceIp + ":1234]");

		Socket socket = SocketFactory.getDefault().createSocket(tcpSourceIp, 1234);
		socket.getOutputStream().write(("Hello World from TCP Source!!" + "\r\n").getBytes());
		socket.close();

		foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "From TCP Source: Hello World from TCP Source!!");
		if (!foundLogs) {
			fail("Did not find the tcp source messages in log sink");
		}
	}
}
