package io.github.sullis.netflix.server;

import com.google.common.collect.Range;
import java.util.stream.IntStream;

public class Hours {
    public static final Range<Integer> RANGE = Range.closed(0, 23);
    public static IntStream stream() {
        return IntStream.rangeClosed(RANGE.lowerEndpoint(), RANGE.upperEndpoint());
    }
}
