package io.github.sullis.flow.server;

public record LookupKey(
        String srcApp,
        String destApp,
        String vpcId,
        int hour) { }
