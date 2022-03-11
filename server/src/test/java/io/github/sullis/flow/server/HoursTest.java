package io.github.sullis.flow.server;

import org.junit.jupiter.api.Test;

import static io.github.sullis.flow.server.Hours.RANGE;
import static org.assertj.core.api.Assertions.assertThat;

public class HoursTest {
    @Test
    public void rangeCheck() {
        assertThat(RANGE.contains(0)).isTrue();
        assertThat(RANGE.contains(23)).isTrue();

        assertThat(RANGE.contains(-1)).isFalse();
        assertThat(RANGE.contains(24)).isFalse();
        assertThat(RANGE.contains(25)).isFalse();
    }

    @Test
    public void allValuesArePresent() {
        final var list = Hours.stream().boxed().toList();
        assertThat(list).hasSize(24);
        assertThat(list).containsExactly(
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23);
    }
}
