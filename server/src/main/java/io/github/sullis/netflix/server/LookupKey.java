package io.github.sullis.netflix.server;

public record LookupKey(
        String srcApp,
        String destApp,
        String vpcId,
        int hour) { }
