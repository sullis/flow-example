package io.github.sullis.flow.server;

import org.junit.jupiter.api.Test;

import static io.github.sullis.flow.server.TestUtils.makeFlowLog;
import static io.github.sullis.flow.server.TestUtils.waitForSuccess;
import static io.github.sullis.flow.server.TestUtils.GET_RESPONSE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.extension.ExtendWith;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.openapitools.model.FlowLog;

import javax.ws.rs.client.Entity;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/**

 This class uses the [dropwizard-testing] library
 https://www.dropwizard.io/en/latest/manual/testing.html

 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class AppTest extends AbstractDropwizardTest {
    private static final String CONTENT_TYPE = "application/json";
    private static final Random RANDOM = new SecureRandom();

    private static final DropwizardAppExtension<AppConfig> APP = setupAppExtension();

    @Test
    public void happyPath() {
        final int hour = 3;
        final var vpc0 = "vpc-0";
        final var vpc1 = "vpc-1";

        postFlows(List.of(makeFlowLog(hour, vpc0), makeFlowLog(hour, vpc1)));
        postFlows(List.of(makeFlowLog(hour, vpc0)));

        final var response = getFlows(hour);
        assertThat(response).hasSize(2);

        final var expected0 = new FlowLog();
        expected0.setHour(3);
        expected0.setBytesRx(2000);
        expected0.setBytesTx(1402);
        expected0.setSrcApp("srcApp1");
        expected0.setDestApp("destApp1");
        expected0.setVpcId(vpc0);

        final var expected1 = new FlowLog();
        expected1.setHour(3);
        expected1.setBytesRx(1000);
        expected1.setBytesTx(701);
        expected1.setSrcApp("srcApp1");
        expected1.setDestApp("destApp1");
        expected1.setVpcId(vpc1);

        assertThat(response)
            .containsExactlyInAnyOrder(expected0, expected1);
    }

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
        }
        executor.shutdown();
    }

    @Test
    public void serverRejectsInvalidHour() {
        final var response = getClient()
                .target(flowsUrl())
                .queryParam("hour", -1)
                .request()
                .accept(CONTENT_TYPE)
                .get();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    public void serverAcceptsAllValidHours() {
        Hours.stream().forEach(hour -> {
            final var response = getClient()
                    .target(flowsUrl())
                    .queryParam("hour", hour)
                    .request()
                    .accept(CONTENT_TYPE)
                    .get();
            assertThat(response.getStatus()).isEqualTo(200);
            final var responseList = response.readEntity(GET_RESPONSE_TYPE);
            assertThat(responseList).isNotNull();
        });
    }

    @Test
    public void serverReturns400WhenPostBodyIsInvalid() {
        postEntity(Entity.json("{garbage"), 400);
    }

    @Test
    public void serverReturns400WhenPostBodyIsEmptyJson() {
        postEntity(Entity.json("{}"), 400);
    }

    @Test
    public void serverRejectsUnsupportedMediaType() {
        postEntity(Entity.text(""), 415);
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
            Callable<Boolean> reader = () -> {
                return (getFlows(hour) != null);
            };
            result.add(reader);
        }
        return result;
    }

}
