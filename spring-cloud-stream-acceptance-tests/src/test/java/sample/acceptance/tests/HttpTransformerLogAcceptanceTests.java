package sample.acceptance.tests;

import org.junit.Test;

import static org.junit.Assert.fail;

public class HttpTransformerLogAcceptanceTests extends AbstractAcceptanceTests {

	@Test
	public void testUppercaseTransformerRabbit() {

		String httpSourceUrl = System.getProperty("http.source.route");
		String transformerProcessorUrl = System.getProperty("transformer.processor.route");
		String logSinkUrl = System.getProperty("log.sink.route");

		boolean foundLogs = waitForLogEntry("HTTP Source", httpSourceUrl, "Started HttpSourceKafkaApplication");
		if(!foundLogs) {
			fail("Did not find the HttpSourceKafkaApplication started logging messages.");
		}

		foundLogs = waitForLogEntry("Transform Processor", transformerProcessorUrl, "Started TransformProcessorKafkaApplication");
		if(!foundLogs) {
			fail("Did not find the TransformProcessorKafkaApplication started logging messages.");
		}

		foundLogs = waitForLogEntry("Log Sink", logSinkUrl, "Started LogSinkKafkaApplication");
		if(!foundLogs) {
			fail("Did not find the log sink started logging message.");
		}

		foundLogs = waitForLogEntry("Uppercase Transformer", logSinkUrl, "From Transformer: FOOBAR");
		if(!foundLogs) {
			fail("Did not find the logging messages.");
		}

	}
}
