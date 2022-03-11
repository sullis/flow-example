package io.github.sullis.flow.server;

import org.openapitools.model.FlowLog;

public class Utils {
    static public FlowLog buildFlowLog(final LookupKey key, final FlowTotal flowTotal) {
        final var log = new FlowLog();
        log.setSrcApp(key.srcApp());
        log.setDestApp(key.destApp());
        log.setVpcId(key.vpcId());
        log.setHour(key.hour());
        log.setBytesRx(flowTotal.bytesRx.intValue());
        log.setBytesTx(flowTotal.bytesTx.intValue());
        return log;
    }
}
