package io.github.sullis.flow.server;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.openapitools.model.FlowLog;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static io.github.sullis.flow.server.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**

 This class uses the [dropwizard-testing] library
 https://www.dropwizard.io/en/latest/manual/testing.html

 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class AppConcurrencyTest extends AbstractDropwizardTest {
    private static final Random RANDOM = new SecureRandom();

    private static final DropwizardAppExtension<AppConfig> APP = setupAppExtension();

    @CartesianTest
    public void concurrency(
            @CartesianTest.Values(ints = { 1, 2, 10 }) final int numReaders,
            @CartesianTest.Values(ints = { 1, 2, 10 }) final int numWriters) throws Exception {

        final int defaultHour = 3;
        final int numThreads = numReaders + numWriters;
        final var executor = Executors.newFixedThreadPool(numThreads);
        final var readers = makeReaders(numReaders, defaultHour);
        final var writers = makeWriters(numWriters, defaultHour);
        final var callables = new LinkedList<Callable<Boolean>>();
        callables.addAll(readers);
        callables.addAll(writers);

        Collections.shuffle(callables, RANDOM);
        final var futures = executor.invokeAll(callables);
        waitForSuccess(futures, Duration.ofSeconds(5));

        final var flows = getFlows(defaultHour);
        assertThat(flows).hasSizeGreaterThan(0);
        for (FlowLog flow: flows) {
            assertThat(flow.getHour()).isEqualTo(3);
            assertThat(flow.getBytesRx()).isGreaterThan(0);
            assertThat(flow.getBytesTx()).isGreaterThan(0);
            assertThat(flow.getBytesRx()).isNotEqualTo(flow.getBytesTx());
            assertThat(Utils.isValid(flow));
        }
        executor.shutdown();
    }

    @Override
    protected DropwizardAppExtension getExtension() {
        return APP;
    }

    private List<Callable<Boolean>> makeWriters(final int numWriters, final int hour) {
        List<Callable<Boolean>> result = new LinkedList<>();
        for (int i = 0; i < numWriters; i++) {
            Callable<Boolean> writer = () -> {
                final var response = postFlows(List.of(
                        makeFlowLog(hour, "vpc-0"),
                        makeFlowLog(hour + 1, "vpc-0"),
                        makeFlowLog(hour, "vpc-0")));

                return response.getStatus() == 204;
            };
            result.add(writer);
        }
        return result;
    }

    private List<Callable<Boolean>> makeReaders(final int numReaders, final int hour) {
        List<Callable<Boolean>> result = new LinkedList<>();
        for (int i = 0; i < numReaders; i++) {
            Callable<Boolean> reader = () -> (getFlows(hour) != null);
            result.add(reader);
        }
        return result;
    }

}
