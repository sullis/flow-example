package io.github.sullis.netflix.server;

import io.dropwizard.setup.Environment;
import org.openapitools.api.FlowsApi;

public class App
    extends io.dropwizard.Application<AppConfig> {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void run(final AppConfig configuration,
                    final Environment environment) {
        final var config = environment.getJerseyServletContainer().getServletConfig();
        environment.jersey().register(new FlowsApi(config));
    }
}
