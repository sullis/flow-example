package io.github.sullis.flow.server;

import com.google.common.collect.Range;
import java.util.stream.IntStream;

public class Hours {
    public static final int HOURS_PER_DAY = 24;
    public static final Range<Integer> RANGE = Range.closed(0, 23);

    public static IntStream stream() {
        return IntStream.rangeClosed(RANGE.lowerEndpoint(), RANGE.upperEndpoint());
    }

    public static boolean isValidHour(Integer hour) {
        return (hour != null) && RANGE.contains(hour);
    }
}
