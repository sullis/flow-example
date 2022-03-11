package io.github.sullis.flow.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.openapitools.model.FlowLog;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static io.github.sullis.flow.server.TestUtils.makeFlowLog;
import static io.github.sullis.flow.server.TestUtils.makeFlowLogs;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class FlowAggregatorTest {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> VPC_LIST = List.of("vpc-0", "vpc-1");
    private static final List<String> DEST_LIST = List.of("dest-0", "dest-1");
    private static final List<String> SRC_LIST = List.of("src-0", "src-1", "src-2");
    private static final List<Integer> HOURS_LIST = Hours.stream().boxed().toList();

    private FlowAggregator aggregator;

    @BeforeEach
    public void beforeEach() {
        aggregator = new FlowAggregator(100);
    }

    @Test
    public void testNoData() {
        Hours.stream().forEach(hour -> {
            assertThat(aggregator.findByHour(hour)).isEmpty();
        });
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(ints = { -1, 25, 100 })
    public void testInvalidHourIsIgnored(final Integer invalidHour) {
        final var vpcId = "vpc-0";
        assertThat(Hours.isValidHour(invalidHour)).isFalse();
        final var validLog = makeFlowLog(3, vpcId);
        final var invalidLog = makeFlowLog(invalidHour, vpcId);
        aggregator.record(List.of(validLog, invalidLog, validLog));
        assertThat(aggregator.getFlowLogCount()).isEqualTo(2);
        assertThat(aggregator.getInvalidFlowLogCount()).isEqualTo(1);
    }

    @CartesianTest
    public void concurrency(
            @Values(ints = { 1, 5, 10, 25 }) final int numThreads,
            @Values(ints = { 1, 2, 3, 10 }) final int numReaders,
            @Values(ints = { 1, 2, 3, 10 }) final int numWriters) throws Exception {
        final List<Integer> hours = HOURS_LIST;
        final int numVpcs = VPC_LIST.size();
        final var logs = hours.stream()
                .map(h -> makeFlowLogs(h, VPC_LIST))
                .flatMap(Collection::stream)
                .toList();
        final var executor = Executors.newFixedThreadPool(numThreads);
        final var callables = new LinkedList<Callable<Boolean>>();
        for (int n = 0; n < numReaders; n++) {
            Callable<Boolean> reader = () -> {
                final var result = aggregator.findByHour(logs.get(0).getHour());
                return (result != null);
            };
            callables.add(reader);
        }
        for (int n = 0; n < numWriters; n++) {
            Callable<Boolean> writer = () -> {
                aggregator.record(logs);
                return true;
            };
            callables.add(writer);
        }

        Collections.shuffle(callables, RANDOM);

        final var futures = executor.invokeAll(callables);
        await().atMost(Duration.ofSeconds(5))
                .pollDelay(Duration.ofMillis(100))
                .until(() -> futures.stream().allMatch(f -> isSuccess(f)));

        assertThat(aggregator.getFlowLogCount()).isEqualTo(numWriters * logs.size());

        hours.stream().forEach(hour -> {
            final var result = aggregator.findByHour(logs.get(0).getHour());
            assertThat(result).hasSize(numVpcs);
            final var flowTotal = result.values().iterator().next();
            assertThat(flowTotal.bytesRx.longValue()).isGreaterThan(0);
            assertThat(flowTotal.bytesTx.longValue()).isGreaterThan(0);
        });
        assertThat(aggregator.getInvalidFlowLogCount()).isZero();
        executor.shutdown();
    }

    @Test
    public void invokeRecordMethodMultipleTimes() {
        final AtomicLong logCount = new AtomicLong(0);
        hours().forEach(hour -> {
            VPC_LIST.forEach(vpc -> {
                DEST_LIST.forEach(dest -> {
                    SRC_LIST.forEach(src -> {
                        final var log = new FlowLog();
                        log.setHour(hour);
                        log.setBytesRx(1000);
                        log.setBytesTx(701);
                        log.setVpcId(vpc);
                        log.setDestApp(dest);
                        log.setSrcApp(src);
                        aggregator.record(List.of(log, log));
                        logCount.addAndGet(2);
                    });
                });
            });
        });

        assertThat(aggregator.getFlowLogCount()).isEqualTo(logCount.get());

        hours().forEach(hour -> {
            final var result = aggregator.findByHour(hour);
            assertThat(result.values()).hasSize(VPC_LIST.size() * SRC_LIST.size() * DEST_LIST.size());
            final var flowTotal = result.values().iterator().next();
            assertThat(flowTotal.bytesRx.longValue()).isEqualTo(2000L);
            assertThat(flowTotal.bytesTx.longValue()).isEqualTo(1402L);
        });

    }

    private static IntStream hours() { return Hours.stream(); }

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
