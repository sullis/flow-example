package io.github.sullis.netflix.server;

import io.dropwizard.setup.Environment;
import org.openapitools.api.FlowsApi;

public class Application
    extends io.dropwizard.Application<Configuration> {

    public static void main(String[] args) throws Exception {
        new Application().run(args);
    }

    @Override
    public void run(final Configuration configuration,
                    final Environment environment) {
        final var config = environment.getJerseyServletContainer().getServletConfig();
        environment.jersey().register(new FlowsApi(config));
    }
}
