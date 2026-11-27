package io.github.piotr_janczukowicz.http;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.piotr_janczukowicz.testsupport.UnitTest;
import org.junit.jupiter.api.Test;

@UnitTest
class HttpUpsertOutcomeTest {

    @Test
    void fromStatus_shouldReturnCreated_whenCreated() {
        assertThat(HttpUpsertOutcome.fromStatus(201)).isEqualTo(HttpUpsertOutcome.CREATED);
    }

    @Test
    void fromStatus_shouldReturnUpdated_whenNoContent() {
        assertThat(HttpUpsertOutcome.fromStatus(204)).isEqualTo(HttpUpsertOutcome.UPDATED);
    }

    @Test
    void fromStatus_shouldReturnUpdated_whenAnyOtherStatus() {
        assertThat(HttpUpsertOutcome.fromStatus(200)).isEqualTo(HttpUpsertOutcome.UPDATED);
    }
}
