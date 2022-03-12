package io.github.sullis.flow.server;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import org.openapitools.model.FlowLog;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.util.List;

import static io.github.sullis.flow.server.TestUtils.GET_RESPONSE_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class AbstractDropwizardTest {
    protected static final String DEFAULT_CONTENT_TYPE = "application/json";
    protected static final String TEST_CONFIG_FILENAME = "test_config.yml";

    protected static DropwizardAppExtension setupAppExtension() {
        return new DropwizardAppExtension<>(
                App.class,
                ResourceHelpers.resourceFilePath(TEST_CONFIG_FILENAME));
    }

    protected abstract DropwizardAppExtension getExtension();

    protected Client getClient() {
        return getExtension().client();
    }

    protected String flowsUrl() {
        return "http://localhost:" + getExtension().getLocalPort() + "/flows";
    }

    protected Response postEntity(final Entity entity, final int expectedStatus) {
        final var response = getClient()
                .target(flowsUrl())
                .request()
                .accept(DEFAULT_CONTENT_TYPE)
                .post(entity);
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        return response;
    }

    protected Response postFlows(final List<FlowLog> payload) {
        return postEntity(Entity.json(payload), 204);
    }

    protected Response getFlowsResponse(final int hour) {
        final var response = getClient()
                .target(flowsUrl())
                .queryParam("hour", hour)
                .request()
                .accept(DEFAULT_CONTENT_TYPE)
                .get();
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getLength()).isGreaterThan(0);
        return response;
    }

    protected List<FlowLog> getFlows(final int hour) {
        return getFlowsResponse(hour).readEntity(GET_RESPONSE_TYPE);
    }
}
