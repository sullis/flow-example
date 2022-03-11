package io.github.sullis.netflix.server;

import io.dropwizard.setup.Environment;

public class App
    extends io.dropwizard.Application<AppConfig> {

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void run(final AppConfig configuration,
                    final Environment environment) {
        final var aggregator = new FlowAggregator(configuration.getAggregatorConcurrencyLevel());
        environment.jersey().register(new FlowsResource(aggregator));
        environment.healthChecks().register("DummyHealthCheck", new DummyHealthCheck());
    }
}
