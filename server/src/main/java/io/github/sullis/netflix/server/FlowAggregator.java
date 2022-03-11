package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

public class FlowAggregator {
    private Map<Integer /* hour */, Map<String, FlowTotal>> flowDataMap;

    private ForkJoinPool forkJoinPool;

    public FlowAggregator() {
        this(new ForkJoinPool(ForkJoinPool.getCommonPoolParallelism()));
    }

    public FlowAggregator(final ForkJoinPool forkJoinPool) {
        this.forkJoinPool = forkJoinPool;
        this.flowDataMap = new ConcurrentHashMap<>(32, 0.75f, 100);
    }

    public void record(final List<FlowLog> logs) {
        logs.parallelStream().forEach(log -> {
            Map<String, FlowTotal> data = findByHour(log.getHour());
            final var key = buildKey(log);
            // TODO use AtomicInteger instead of Long ???
            FlowTotal total = data.get(key);
            if (null == total) {
                total = new FlowTotal();
                data.put(key, total);
            }
            total.bytesRx.addAndGet(log.getBytesRx());
            total.bytesTx.addAndGet(log.getBytesTx());
        });
    }

    public class FlowTotal {
        public final AtomicInteger bytesRx = new AtomicInteger(0);
        public final AtomicInteger bytesTx = new AtomicInteger(0);
    }

    public Map<String, FlowTotal> findByHour(final Integer hour) {
        Map<String, FlowTotal> result = flowDataMap.get(hour);
        if (null == result) {
            result = new ConcurrentHashMap<>();
            flowDataMap.put(hour, result);
        }
        return result;
    }

    private static String buildKey(final FlowLog log) {
        final var sb = new StringBuilder();
        sb.append(log.getSrcApp());
        sb.append("-");
        sb.append(log.getDestApp());
        sb.append("-");
        sb.append(log.getVpcId());
        sb.append("-");
        sb.append(log.getHour());
        return sb.toString();
    }
}
