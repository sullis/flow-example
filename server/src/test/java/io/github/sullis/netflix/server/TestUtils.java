package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;

import java.util.List;

public class TestUtils {
    public static final List<FlowLog> makeLogs(final int hour, final List<String> vpcs) {
        return vpcs.stream().map(vpc -> makeLog(hour, vpc)).toList();
    }

    public static final FlowLog makeLog(int hour, final String vpcId) {
        final var log = new FlowLog();
        log.setHour(hour);
        log.setBytesRx(1000);
        log.setBytesTx(701);
        log.setVpcId(vpcId);
        log.setDestApp("destApp1");
        log.setSrcApp("srcApp1");
        return log;
    }

}