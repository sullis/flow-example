package io.github.sullis.flow.server;

import org.openapitools.model.FlowLog;

import static io.github.sullis.flow.server.Hours.isValidHour;

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

    public static boolean isValid(final FlowLog log) {
        if (!isValidHour(log.getHour())) {
            return false;
        }
        if ((log.getBytesRx() == null) || (log.getBytesRx() < 0)) {
            return false;
        }
        if ((log.getBytesTx() == null) || (log.getBytesTx() < 0)) {
            return false;
        }
        if (log.getSrcApp() == null) {
            return false;
        }
        if (log.getDestApp() == null) {
            return false;
        }
        if (log.getVpcId() == null) {
            return false;
        }
        return true;
    }

}
