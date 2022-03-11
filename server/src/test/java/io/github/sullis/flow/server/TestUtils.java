package io.github.sullis.flow.server;

import org.openapitools.model.FlowLog;

import javax.ws.rs.core.GenericType;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;

public class TestUtils {
    public static final GenericType<List<FlowLog>> GET_RESPONSE_TYPE = new GenericType<List<FlowLog>>() {};

    public static final List<FlowLog> makeFlowLogs(final int hour, final List<String> vpcs) {
        return vpcs.stream().map(vpc -> makeFlowLog(hour, vpc)).toList();
    }

    public static final FlowLog makeFlowLog(final Integer hour, final String vpcId) {
        final var log = new FlowLog();
        log.setHour(hour);
        log.setBytesRx(1000);
        log.setBytesTx(701);
        log.setVpcId(vpcId);
        log.setDestApp("destApp1");
        log.setSrcApp("srcApp1");
        return log;
    }

    public static void waitForSuccess(final List<Future<Boolean>> futures, final Duration atMost) {
        await().atMost(atMost)
                .pollDelay(Duration.ofMillis(100))
                .until(() -> futures.stream().allMatch(f -> isSuccess(f)));
    }

    private static boolean isSuccess(final Future<Boolean> future) {
        try {
            return future.isDone()
                    && !future.isCancelled()
                    && future.get().booleanValue();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
