package io.github.sullis.flow.server;

import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;

import javax.ws.rs.client.Entity;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static io.github.sullis.flow.server.TestUtils.loadResource;
import static org.assertj.core.api.Assertions.assertThat;


/**

 This class uses the [dropwizard-testing] library
 https://www.dropwizard.io/en/latest/manual/testing.html

 This class [ExampleTest] exists because I wanted to try to reproduce
 the exact JSON example from the PDF document.

 Unfortunately, there are errors in the original PDF file.
 I had to edit the JSON examples to make things work.

 */
@ExtendWith(DropwizardExtensionsSupport.class)
class ExampleTest extends AbstractDropwizardTest {

    private static final DropwizardAppExtension<AppConfig> APP = setupAppExtension();

    @Test
    void example() {
        final var requestPayload = loadResource("example/request_payload.json");
        final var response = getClient()
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
        assertThatJson(flows2)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(fixture2);

        final var flows3 = getFlowsResponse(3).readEntity(String.class);
        assertThatJson(flows3)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(fixture3);
    }

    @Override
    protected DropwizardAppExtension getExtension() {
        return APP;
    }
}
