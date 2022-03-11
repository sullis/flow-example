package io.github.sullis.netflix.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Min;

public class AppConfig extends io.dropwizard.Configuration {
    @Min(1)
    private Integer aggregatorConcurrencyLevel;

    @JsonProperty
    public Integer getAggregatorConcurrencyLevel() {
        return aggregatorConcurrencyLevel;
    }

    @JsonProperty
    public void setAggregatorConcurrencyLevel(Integer level) {
        this.aggregatorConcurrencyLevel = level;
    }
}
