package sample.acceptance.tests;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PartitioningKafkaAcceptanceTests extends AbstractAcceptanceTests {


    Set<String> partitions = new HashSet<>();


    @Test
    public void testPartitioningWith3ConsumersKafka() throws Exception {

        Thread.sleep(10_000);

        String prodUrl = System.getProperty("partitioning.producer.route");

        boolean foundLogs = waitForLogEntry("Partitioning producer", prodUrl, "Started PartProducerApplication in");
        if(!foundLogs) {
            fail("Did not find the logging messages.");
        }

        String consumer1Url = System.getProperty("partitioning.consumer1.route");
        String consumer2Url = System.getProperty("partitioning.consumer2.route");
        String consumer3Url = System.getProperty("partitioning.consumer3.route");
        //String consumer4Url = System.getProperty("partitioning.consumer4.route");

        Future<?> future1 = verifyPartitions("Partitioning Consumer-1", consumer1Url, consumer2Url, consumer3Url,
                "f received from partition 0",
                "g received from partition 0",
                "h received from partition 0");
        Future<?> future2 = verifyPartitions("Partitioning Consumer-2", consumer1Url, consumer2Url, consumer3Url,
                "fo received from 1",
                "go received from partition 1",
                "ho received from partition 1");
        Future<?> future3 = verifyPartitions("Partitioning Consumer-3", consumer1Url, consumer2Url, consumer3Url,
                "foo received from 2",
                "goo received from 2",
                "hoo received from partition 2");
//		Future<?> future4 = verifyPartitions("Partitioning Consumer-4",consumer4Url,
//				"fooz received from partition partitioned.destination.myGroup-3",
//				"gooz received from partition partitioned.destination.myGroup-3",
//				"hooz received from partition partitioned.destination.myGroup-3");

        verifyResults(future1, future2, future3);
    }

    private Future<?> verifyPartitions(String consumerMsg, String consumer1Route, String consumer2Route, String consumer3Route,
                                       String... entries) {

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        Future<?> submit = executorService.submit(() -> {
            boolean found = waitForLogEntry(consumerMsg, consumer1Route, entries);
            if (found) {
                partitions.add(consumer1Route);
            }
            if (!found) {
                found = waitForLogEntry(consumerMsg, consumer2Route, entries);
                if (found) {
                    partitions.add(consumer2Route);
                }
            }
            if (!found) {
                found = waitForLogEntry(consumerMsg, consumer3Route, entries);
                if (found) {
                    partitions.add(consumer3Route);
                }
            }

            if (!found) {
                fail("Could not find the test data in the logs");
            }
        });

        executorService.shutdown();
        return submit;
    }

    private void verifyResults(Future<?>... futures) throws Exception {
        for (Future<?> future : futures) {
            try {
                future.get();
            }
            catch (Exception e) {
                throw e;
            }
        }
        assertEquals("Not all app instances received data equally", 3, partitions.size());
    }
}
