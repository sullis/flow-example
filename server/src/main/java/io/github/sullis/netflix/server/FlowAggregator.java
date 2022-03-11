package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class FlowAggregator {
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowAggregator.class);
    private final Map<Integer /* hour */, Map<LookupKey, FlowTotal>> flowDataMap;
    private final LongAdder flowLogCount = new LongAdder();

    /**
     *
     * @param concurrencyLevel the estimated number of concurrently
     * updating threads. The implementation may use this value as
     * a sizing hint.
     *
     */
    public FlowAggregator(final int concurrencyLevel) {
        this.flowDataMap = new ConcurrentHashMap<>(24 /* 24 hours */, DEFAULT_LOAD_FACTOR, concurrencyLevel);
    }

    public void record(final List<FlowLog> logs) {
        // Note:  using Java's parallelStream to improve
        //        performance. If I had more time, I would add
        //        load tests that confirm this.
        logs.parallelStream().forEach(log -> {
            final Integer hour = log.getHour();
            if ((hour != null) && Hours.RANGE.contains(log.getHour())) {
                Map<LookupKey, FlowTotal> data = findByHour(log.getHour());
                final var key = buildLookupKey(log);
                final FlowTotal total = data.computeIfAbsent(key, (k) -> new FlowTotal());
                total.bytesRx.add(log.getBytesRx());
                total.bytesTx.add(log.getBytesTx());
                flowLogCount.increment();
            } else {
                LOGGER.warn("FlowLog contains invalid hour: {}", hour);
                // TODO : report a metric to DataDog or equivalent
            }
        });
    }

    public Map<LookupKey, FlowTotal> findByHour(final Integer hour) {
        return flowDataMap.computeIfAbsent(hour, (h) -> new ConcurrentHashMap<>());
    }

    public long getFlowLogCount() {
        return flowLogCount.sum();
    }

    private static LookupKey buildLookupKey(final FlowLog log) {
        return new LookupKey(log.getSrcApp(),
                            log.getDestApp(),
                            log.getVpcId(),
                            log.getHour());
    }
}
