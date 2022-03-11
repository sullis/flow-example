package io.github.sullis.netflix.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.Values;
import org.openapitools.model.FlowLog;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class FlowAggregatorTest {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final List<String> VPC_LIST = List.of("vpc1", "vpc2");

    private FlowAggregator aggregator;

    @BeforeEach
    public void beforeEach() {
        aggregator = new FlowAggregator();
    }

    @Test
    public void testNoData() {
        Hours.stream().forEach(hour -> {
            assertThat(aggregator.findByHour(hour)).isEmpty();
        });
    }

    @CartesianTest
    public void concurrency(
            @Values(ints = { 1, 2, 3, 10 }) final int numThreads,
            @Values(ints = { 1, 2, 3, 10 }) final int numReaders,
            @Values(ints = { 1, 2, 3, 10 }) final int numWriters) throws Exception {
        final List<Integer> hours = List.of(5, 10, 3, 12, 1);
        final var logs = hours.stream().map(h -> makeLog(h)).collect(Collectors.toList());
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
                .until(() -> futures.stream().allMatch(f -> f.isDone()));

        assertThat(aggregator.getFlowLogCount()).isEqualTo(numWriters * logs.size());

        hours.stream().forEach(hour -> {
            final var result = aggregator.findByHour(logs.get(0).getHour());
            assertThat(result).hasSize(1);
            final var flowTotal = result.values().iterator().next();
            assertThat(flowTotal.bytesRx.longValue()).isGreaterThan(0);
            assertThat(flowTotal.bytesTx.longValue()).isGreaterThan(0);
        });
        executor.shutdown();
    }

    @Test
    public void invokeRecordMethodMultipleTimes() {
        hours().forEach(hour -> {
            VPC_LIST.forEach(vpc -> {
                final var log = new FlowLog();
                log.setHour(hour);
                log.setBytesRx(1000);
                log.setBytesTx(700);
                log.setVpcId(vpc);
                log.setDestApp("destApp1");
                log.setSrcApp("srcApp1");
                aggregator.record(List.of(log, log));
            });
        });

        assertThat(aggregator.getFlowLogCount()).isEqualTo(96L);

        hours().forEach(hour -> {
            final var result = aggregator.findByHour(hour);
            assertThat(result.values()).hasSize(VPC_LIST.size());
            final var flowTotal = result.values().iterator().next();
            assertThat(flowTotal.bytesRx.longValue()).isEqualTo(2000L);
            assertThat(flowTotal.bytesTx.longValue()).isEqualTo(1400L);
        });

    }

    private static IntStream hours() { return Hours.stream(); }

    private static final FlowLog makeLog(int hour) {
        final var log = new FlowLog();
        log.setHour(hour);
        log.setBytesRx(12301);
        log.setBytesTx(23401);
        log.setVpcId(VPC_LIST.get(0));
        log.setDestApp("destApp1");
        log.setSrcApp("srcApp1");
        return log;
    }
}
