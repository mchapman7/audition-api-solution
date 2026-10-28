package com.audition.common.logging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@ExtendWith(MockitoExtension.class)
class AuditionLoggerTest {

    private final AuditionLogger auditionLogger = new AuditionLogger();

    @Mock
    private Logger logger;

    // ---- level enabled ----

    @Test
    void infoLogsWhenEnabled() {
        when(logger.isInfoEnabled()).thenReturn(true);

        auditionLogger.info(logger, "hello");

        verify(logger).info("hello");
    }

    @Test
    void infoWithArgumentLogsWhenEnabled() {
        when(logger.isInfoEnabled()).thenReturn(true);

        auditionLogger.info(logger, "value: {}", 42);

        verify(logger).info("value: {}", 42);
    }

    @Test
    void debugLogsWhenEnabled() {
        when(logger.isDebugEnabled()).thenReturn(true);

        auditionLogger.debug(logger, "details");

        verify(logger).debug("details");
    }

    @Test
    void warnLogsWhenEnabled() {
        when(logger.isWarnEnabled()).thenReturn(true);

        auditionLogger.warn(logger, "careful");

        verify(logger).warn("careful");
    }

    @Test
    void errorLogsWhenEnabled() {
        when(logger.isErrorEnabled()).thenReturn(true);

        auditionLogger.error(logger, "failed");

        verify(logger).error("failed");
    }

    @Test
    void logErrorWithExceptionPassesTheExceptionSoTheStackTraceIsKept() {
        when(logger.isErrorEnabled()).thenReturn(true);
        final Exception exception = new IllegalStateException("boom");

        auditionLogger.logErrorWithException(logger, "failed", exception);

        verify(logger).error("failed", exception);
    }

    // ---- level disabled ----

    @Test
    void nothingIsLoggedWhenTheLevelIsDisabled() {
        when(logger.isInfoEnabled()).thenReturn(false);
        when(logger.isDebugEnabled()).thenReturn(false);
        when(logger.isWarnEnabled()).thenReturn(false);
        when(logger.isErrorEnabled()).thenReturn(false);

        auditionLogger.info(logger, "i");
        auditionLogger.info(logger, "i {}", 1);
        auditionLogger.debug(logger, "d");
        auditionLogger.warn(logger, "w");
        auditionLogger.error(logger, "e");
        auditionLogger.logErrorWithException(logger, "e", new RuntimeException());
        auditionLogger.logStandardProblemDetail(logger, ProblemDetail.forStatus(500), new RuntimeException());
        auditionLogger.logHttpStatusCodeError(logger, "m", 500);

        verify(logger, never()).info(anyString());
        verify(logger, never()).debug(anyString());
        verify(logger, never()).warn(anyString());
        verify(logger, never()).error(anyString());
        verify(logger, never()).error(anyString(), org.mockito.ArgumentMatchers.any(Throwable.class));
    }

    // ---- message formatting ----

    @Test
    void logStandardProblemDetailIncludesStatusTitleAndDetailAndTheException() {
        when(logger.isErrorEnabled()).thenReturn(true);
        final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        problemDetail.setTitle("Upstream Service Error");
        problemDetail.setDetail("the real cause");
        final Exception exception = new IllegalStateException("boom");

        auditionLogger.logStandardProblemDetail(logger, problemDetail, exception);

        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(logger).error(message.capture(), eq(exception));
        assertThat(message.getValue())
            .contains("502")
            .contains("Upstream Service Error")
            .contains("the real cause");
    }

    @Test
    void logHttpStatusCodeErrorIncludesTheCodeAndTheMessage() {
        when(logger.isErrorEnabled()).thenReturn(true);

        auditionLogger.logHttpStatusCodeError(logger, "not found", 404);

        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        verify(logger).error(message.capture());
        assertThat(message.getValue()).contains("404").contains("not found");
    }
}