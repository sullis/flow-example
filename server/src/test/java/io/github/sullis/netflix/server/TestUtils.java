package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;

import javax.ws.rs.core.GenericType;
import java.util.List;

public class TestUtils {
    public static final GenericType<List<FlowLog>> GET_RESPONSE_TYPE = new GenericType<List<FlowLog>>() {};

    public static final List<FlowLog> makeFlowLogs(final int hour, final List<String> vpcs) {
        return vpcs.stream().map(vpc -> makeFlowLog(hour, vpc)).toList();
    }

    public static final FlowLog makeFlowLog(int hour, final String vpcId) {
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
