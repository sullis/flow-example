package io.github.sullis.flow.server;

import java.util.concurrent.atomic.LongAdder;

public class FlowTotal {
    public final LongAdder bytesRx = new LongAdder();
    public final LongAdder bytesTx = new LongAdder();
}
