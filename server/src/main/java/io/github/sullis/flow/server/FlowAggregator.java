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

public class FlowAggregator {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowAggregator.class);
    private final ConcurrentHashMap<LookupKey, FlowTotal>[] flowDataArray;
    private final LongAdder flowLogCount = new LongAdder();
    private final LongAdder invalidFlowLogCount = new LongAdder();

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
        // Note:  using Java's parallelStream to improve
        //        performance. If I had more time, I would add
        //        load tests that confirm this.
        logs.parallelStream().forEach(log -> {
            if (isValid(log)) {
                Map<LookupKey, FlowTotal> data = findByHour(log.getHour());
                final var key = buildLookupKey(log);
                final FlowTotal total = data.computeIfAbsent(key, (k) -> new FlowTotal());
                total.bytesRx.add(log.getBytesRx());
                total.bytesTx.add(log.getBytesTx());
                flowLogCount.increment();
            } else {
                LOGGER.warn("invalid FlowLog: {}", log);
                invalidFlowLogCount.increment();
                // TODO : report a metric to DataDog or equivalent
            }
        });
    }

    private static boolean isValid(final FlowLog log) {
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
