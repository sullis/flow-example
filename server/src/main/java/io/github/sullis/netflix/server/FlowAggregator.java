package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

public class FlowAggregator {
    private Map<Integer /* hour */, Map<LookupKey, FlowTotal>> flowDataMap;
    public LongAdder flowLogCount = new LongAdder();

    public FlowAggregator() {
        this.flowDataMap = new ConcurrentHashMap<>(32, 0.75f, 100);
    }

    public void record(final List<FlowLog> logs) {
        logs.parallelStream().forEach(log -> {
            Map<LookupKey, FlowTotal> data = findByHour(log.getHour());
            final var key = buildLookupKey(log);
            FlowTotal total = data.computeIfAbsent(key, (k) -> new FlowTotal());
            total.bytesRx.add(log.getBytesRx());
            total.bytesTx.add(log.getBytesTx());
            flowLogCount.increment();
        });
    }

    static class FlowTotal {
        public final LongAdder bytesRx = new LongAdder();
        public final LongAdder bytesTx = new LongAdder();
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
