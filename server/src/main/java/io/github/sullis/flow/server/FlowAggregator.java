package io.github.sullis.flow.server;

import org.openapitools.model.FlowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import static io.github.sullis.flow.server.Hours.HOURS_PER_DAY;
import static io.github.sullis.flow.server.Hours.isValidHour;
import static io.github.sullis.flow.server.Utils.isValid;

public class FlowAggregator {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowAggregator.class);
    private final ConcurrentHashMap<LookupKey, FlowTotal>[] flowDataArray;
    private final LongAdder flowLogCount = new LongAdder();
    private final LongAdder invalidFlowLogCount = new LongAdder();
    private final LongAdder processLogExceptionCount = new LongAdder();

    /**
     *
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     *
     */
    public FlowAggregator(final int concurrencyLevel) {
        this.flowDataArray = new ConcurrentHashMap[HOURS_PER_DAY];
        Hours.stream().forEach(hour -> {
            flowDataArray[hour] = new ConcurrentHashMap<LookupKey, FlowTotal>(100, DEFAULT_LOAD_FACTOR, concurrencyLevel);
        });
    }

    public void record(final List<FlowLog> logs) {
        // NOTE: using Java's parallelStream to process FlowLog's in parallel.
        // TODO: add more tests to confirm that parallelStream improves processing performance.
        logs.parallelStream().forEach(log -> {
            if (isValid(log)) {
                try {
                    processLog(log);
                    flowLogCount.increment();
                } catch (Exception ex) {
                    LOGGER.warn("FlowLog processing error: " + log, ex);
                    processLogExceptionCount.increment();
                }
            } else {
                LOGGER.warn("invalid FlowLog: {}", log);
                invalidFlowLogCount.increment();
                // TODO : report a metric to DataDog or equivalent
            }
        });
    }

    private void processLog(final FlowLog log) {
        Map<LookupKey, FlowTotal> data = findByHour(log.getHour());
        final var key = buildLookupKey(log);
        final FlowTotal total = data.computeIfAbsent(key, (k) -> new FlowTotal());
        total.bytesRx.add(log.getBytesRx());
        total.bytesTx.add(log.getBytesTx());
    }

    public Map<LookupKey, FlowTotal> findByHour(final Integer hour) {
        if (isValidHour(hour)) {
            return this.flowDataArray[hour];
        } else {
            LOGGER.warn("Invalid hour: " + hour);
            return Collections.emptyMap();
        }
    }

    public long getFlowLogCount() {
        return flowLogCount.sum();
    }

    public long getInvalidFlowLogCount() {
        return invalidFlowLogCount.sum();
    }

    private static LookupKey buildLookupKey(final FlowLog log) {
        return new LookupKey(log.getSrcApp(),
                            log.getDestApp(),
                            log.getVpcId(),
                            log.getHour());
    }
}
