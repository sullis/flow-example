package io.github.sullis.netflix.server;

import com.codahale.metrics.health.HealthCheck;

// Dropwizard expects at least 1 healthcheck
// This class is a dummy (placeholder) implementation.
public class DummyHealthCheck extends HealthCheck {

    @Override
    protected Result check() {
        return Result.healthy();
    }
}