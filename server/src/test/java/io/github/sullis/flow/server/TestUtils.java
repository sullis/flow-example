package io.github.sullis.flow.server;

import com.google.common.io.Resources;
import org.openapitools.model.FlowLog;

import javax.ws.rs.core.GenericType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;

import static org.awaitility.Awaitility.await;

public class TestUtils {
    public static final GenericType<List<FlowLog>> GET_RESPONSE_TYPE = new GenericType<List<FlowLog>>() {};

    public static List<FlowLog> makeFlowLogs(final int hour, final List<String> vpcs) {
        return vpcs.stream().map(vpc -> makeFlowLog(hour, vpc)).toList();
    }

    public static FlowLog makeFlowLog(final Integer hour, final String vpcId) {
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
                .until(() -> futures.stream().allMatch(TestUtils::isSuccess));
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

    public static String loadResource(final String resourceName) {
        final var url = Resources.getResource(resourceName);
        try {
            return Resources.asCharSource(url, StandardCharsets.UTF_8).read();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
