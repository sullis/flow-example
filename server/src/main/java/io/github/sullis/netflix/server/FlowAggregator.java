package io.github.sullis.netflix.server;

import org.openapitools.model.FlowLog;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicLong;

public class FlowAggregator {
    private Map<Integer /* hour */, Map<String, FlowTotal>> flowDataMap;
    public AtomicLong flowLogCount = new AtomicLong(0);
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
            flowLogCount.incrementAndGet();
            Map<String, FlowTotal> data = findByHour(log.getHour());
            final var key = buildKey(log);
            // TODO use AtomicLong instead of Long ???
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
        public final AtomicLong bytesRx = new AtomicLong(0);
        public final AtomicLong bytesTx = new AtomicLong(0);
    }

    public Map<String, FlowTotal> findByHour(final Integer hour) {
        Map<String, FlowTotal> result = flowDataMap.get(hour);
        if (null == result) {
            result = new ConcurrentHashMap<>();
            flowDataMap.put(hour, result);
        }
        return result;
    }

    public long getFlowLogCount() {
        return flowLogCount.longValue();
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
