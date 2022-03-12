package io.github.sullis.flow.server;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;

import static io.github.sullis.flow.server.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.extension.ExtendWith;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.openapitools.model.FlowLog;

import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

/**

 This test uses the [dropwizard-testing] library
 https://www.dropwizard.io/en/latest/manual/testing.html

 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class AppTest {
    private static final String CONTENT_TYPE = "application/json";
    private static final Random RANDOM = new SecureRandom();

    private static final DropwizardAppExtension<AppConfig> APP = new DropwizardAppExtension<>(
            App.class,
            ResourceHelpers.resourceFilePath("test_config.yml")
    );

    private static final String flowsUrl() {
        return "http://localhost:" + APP.getLocalPort() + "/flows";
    }

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

    @Test
    public void serverRejectsInvalidHour() {
        final var response = APP.client()
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
            final var response = APP.client()
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

    @Test
    public void example() {
        final var requestPayload = loadResource("example/request_payload.json");
        System.out.println(requestPayload);
        final var response = APP.client()
            .target(flowsUrl())
            .request()
            .post(Entity.json(requestPayload));
        assertThat(response.getStatus()).isEqualTo(204);

        final var fixture1 = loadResource("example/response_hour1.json");
        final var fixture2 = loadResource("example/response_hour2.json");
        final var fixture3 = loadResource("example/response_hour3.json");

        final var flows1 = getFlowsResponse(1).readEntity(String.class);
        assertThatJson(flows1)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(fixture1);

        final var flows2 = getFlowsResponse(2).readEntity(String.class);
        System.out.println("flows2: " + flows2);
        assertThatJson(flows2)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(fixture2);

        final var flows3 = getFlowsResponse(3).readEntity(String.class);
        assertThatJson(flows3)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(fixture3);
    }


    private Response postEntity(final Entity entity, final int expectedStatus) {
        final var response = APP.client()
                .target(flowsUrl())
                .request()
                .accept(CONTENT_TYPE)
                .post(entity);
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        return response;
    }

    private Response postFlows(final List<FlowLog> payload) {
        return postEntity(Entity.json(payload), 204);
    }

    private Response getFlowsResponse(final int hour) {
        final var response = APP.client()
                .target(flowsUrl())
                .queryParam("hour", hour)
                .request()
                .accept(CONTENT_TYPE)
                .get();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getLength()).isGreaterThan(0);
        return response;
    }

    private List<FlowLog> getFlows(final int hour) {
        return getFlowsResponse(hour).readEntity(GET_RESPONSE_TYPE);
    }
}
