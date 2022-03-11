package io.github.sullis.netflix.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.Assertions;

public class FlowAggregatorTest {
    private FlowAggregator aggregator;

    @BeforeEach
    public void beforeEach() {
        aggregator = new FlowAggregator();
    }

    @Test
    public void testNoData() {
        final var map = aggregator.findByHour(1);
        assertThat(map).isEmpty();
    }

}
