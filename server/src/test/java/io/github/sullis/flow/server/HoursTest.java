package io.github.sullis.flow.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.IntStream;

import static io.github.sullis.flow.server.Hours.RANGE;
import static org.assertj.core.api.Assertions.assertThat;

class HoursTest {
    @Test
    void rangeCheck() {
        assertThat(RANGE.contains(0)).isTrue();
        assertThat(RANGE.contains(23)).isTrue();

        assertThat(RANGE.contains(-1)).isFalse();
        assertThat(RANGE.contains(24)).isFalse();
        assertThat(RANGE.contains(25)).isFalse();
    }

    @Test
    void allValuesArePresent() {
        final var list = Hours.stream().boxed().toList();
        assertThat(list).hasSize(Hours.HOURS_PER_DAY);
        assertThat(list).containsExactly(
                0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 25, 100})
    void isValidHourReturnsFalse(final int hour) {
        assertThat(Hours.isValidHour(hour)).isFalse();
    }

    @ParameterizedTest
    @MethodSource
    void isValidHourReturnsTrue(final int hour) {
        assertThat(Hours.isValidHour(hour)).isTrue();
    }

    private static IntStream isValidHourReturnsTrue() {
        return Hours.stream();
    }
}
