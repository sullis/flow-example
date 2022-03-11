package io.github.sullis.netflix.server;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
// import static io.restassured.RestAssured.given;

@ExtendWith(DropwizardExtensionsSupport.class)
public class AppTest {
    private static final DropwizardAppExtension<AppConfig> APP = new DropwizardAppExtension<>(
            App.class,
            ResourceHelpers.resourceFilePath("test_config.yml")
    );

    private static String flowsEndpointUrl() {
        return "http://localhost:" + APP.getLocalPort() + "/flows";
    }

    @Test
    public void happyPath() throws Exception {
        /*
        given()
                .request()
                .get(flowsEndpointUrl())
                .then()
                .assertThat()
                .statusCode(200)
                .contentType("application/json")
                .body(containsString("Hello Obama")); */
    }
}
