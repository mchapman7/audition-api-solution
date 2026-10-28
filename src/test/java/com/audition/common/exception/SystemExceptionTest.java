package com.audition.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SystemExceptionTest {

    private static final RuntimeException CAUSE = new RuntimeException("root cause");

    @Test
    void fullConstructorKeepsTheMessageTitleStatusAndCause() {
        final SystemException e = new SystemException("failed", "Some Title", 502, CAUSE);

        assertThat(e.getMessage()).isEqualTo("failed");
        assertThat(e.getTitle()).isEqualTo("Some Title");
        assertThat(e.getStatusCode()).isEqualTo(502);
        assertThat(e.getCause()).isSameAs(CAUSE);
    }

    @Test
    void constructorWithoutACauseHasNoCause() {
        final SystemException e = new SystemException("failed", "Some Title", 404);

        assertThat(e.getMessage()).isEqualTo("failed");
        assertThat(e.getTitle()).isEqualTo("Some Title");
        assertThat(e.getStatusCode()).isEqualTo(404);
        assertThat(e.getCause()).isNull();
    }
}
