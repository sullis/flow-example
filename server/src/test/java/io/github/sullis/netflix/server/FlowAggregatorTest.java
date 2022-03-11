package io.github.sullis.netflix.server;

import com.google.common.collect.Range;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.model.FlowLog;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class FlowAggregatorTest {
    private static final Range<Integer> HOURS_RANGE = Range.closed(0, 23);
    private static final List<String> VPC_LIST = List.of("vpc1", "vpc2");

    private FlowAggregator aggregator;

    private static IntStream hours() {
        return IntStream.rangeClosed(HOURS_RANGE.lowerEndpoint(), HOURS_RANGE.upperEndpoint());
    }

    @BeforeEach
    public void beforeEach() {
        aggregator = new FlowAggregator();
    }

    @Test
    public void testNoData() {
        hours().forEach(hour -> {
            assertThat(aggregator.findByHour(hour)).isEmpty();
        });
    }

    @Test
    public void invokeRecordMethodMultipleTimes() {
        hours().forEach(hour -> {
            VPC_LIST.forEach(vpc -> {
                final var log = new FlowLog();
                log.setHour(hour);
                log.setBytesRx(1230);
                log.setBytesTx(2340);
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
            assertThat(flowTotal.bytesRx.longValue()).isEqualTo(2460L);
            assertThat(flowTotal.bytesTx.longValue()).isEqualTo(4680L);
        });

    }

    private static final FlowLog makeLog(int hour) {
        final var log = new FlowLog();
        log.setHour(hour);
        log.setBytesRx(1230);
        log.setBytesTx(2340);
        log.setVpcId(VPC_LIST.get(0));
        log.setDestApp("destApp1");
        log.setSrcApp("srcApp1");
        return log;
    }
}
