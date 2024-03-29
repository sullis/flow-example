package io.github.sullis.flow.server;

import org.junit.jupiter.api.Test;

import static io.github.sullis.flow.server.TestUtils.makeFlowLog;
import static io.github.sullis.flow.server.TestUtils.GET_RESPONSE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.extension.ExtendWith;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.openapitools.model.FlowLog;

import javax.ws.rs.client.Entity;
import java.util.List;

/**

 This class uses the [dropwizard-testing] library
 https://www.dropwizard.io/en/latest/manual/testing.html

 */
@ExtendWith(DropwizardExtensionsSupport.class)
class AppTest extends AbstractDropwizardTest {
    private static final String CONTENT_TYPE = "application/json";

    private static final DropwizardAppExtension<AppConfig> APP = setupAppExtension();

    @Test
    void happyPath() {
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

    @Test
    void serverRejectsInvalidHour() {
        final var response = getClient()
                .target(flowsUrl())
                .queryParam("hour", -1)
                .request()
                .accept(CONTENT_TYPE)
                .get();
        assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    void serverAcceptsAllValidHours() {
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
    void serverReturns400WhenPostBodyIsInvalid() {
        postEntity(Entity.json("{garbage"), 400);
    }

    @Test
    void serverReturns400WhenPostBodyIsEmptyJson() {
        postEntity(Entity.json("{}"), 400);
    }

    @Test
    void serverRejectsUnsupportedMediaType() {
        postEntity(Entity.text(""), 415);
    }

    @Override
    protected DropwizardAppExtension getExtension() {
        return APP;
    }

}
