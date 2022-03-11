package io.github.sullis.netflix.server;

import org.junit.jupiter.api.Test;

import static io.github.sullis.netflix.server.TestUtils.makeLog;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.extension.ExtendWith;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import org.openapitools.model.FlowLog;

import javax.ws.rs.core.Response;
import javax.ws.rs.client.Entity;
import java.util.List;

/**

 This test uses the [dropwizard-testing] library
 https://www.dropwizard.io/en/latest/manual/testing.html

 */
@ExtendWith(DropwizardExtensionsSupport.class)
public class AppTest {
    private static final String CONTENT_TYPE = "application/json";

    private static final DropwizardAppExtension<AppConfig> APP = new DropwizardAppExtension<>(
            App.class,
            ResourceHelpers.resourceFilePath("test_config.yml")
    );

    private static final String flowsUrl() {
        return "http://localhost:" + APP.getLocalPort() + "/flows";
    }

    @Test
    public void happyPath() {
        final var vpc = "vpc-0";
        postFlows(List.of(makeLog(3, vpc), makeLog(12, vpc)));
        postFlows(List.of(makeLog(3, vpc)));
        final var response = getFlows(3);
        assertThat(response.getStatus()).isEqualTo(200);
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
        return postEntity(Entity.json(payload), 200);
    }

    private Response getFlows(final int hour) {
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
}
