package com.audition.web.advice;

import static com.audition.constant.ErrorMessage.DEFAULT_MESSAGE;
import static com.audition.constant.ErrorMessage.DEFAULT_TITLE;
import static com.audition.constant.ErrorMessage.VALIDATION_TITLE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.audition.common.exception.SystemException;
import com.audition.common.logging.AuditionLogger;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;


@ControllerAdvice
@RequiredArgsConstructor
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionControllerAdvice.class);

    private final AuditionLogger logger;

    @ExceptionHandler(SystemException.class)
    ProblemDetail handleSystemException(final SystemException e) {

        final HttpStatusCode status = resolveStatus(e);
        final String title = Optional.ofNullable(e.getTitle()).orElse(DEFAULT_TITLE);
        // 4xx messages are meant for the caller; 5xx details stay in the log.
        final String detail = status.is5xxServerError() ? DEFAULT_MESSAGE : messageOf(e);

        return createProblemDetail(status, title, detail, e);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(final ConstraintViolationException e) {

        return createProblemDetail(BAD_REQUEST, VALIDATION_TITLE, describe(e), e);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleMainException(final Exception e) {

        return createProblemDetail(INTERNAL_SERVER_ERROR, DEFAULT_TITLE, DEFAULT_MESSAGE, e);
    }


    private ProblemDetail createProblemDetail(final HttpStatusCode status, final String title, final String detail,
        final Exception exception) {

        final ProblemDetail problemDetail = ProblemDetail.forStatus(status);
        problemDetail.setTitle(title);
        problemDetail.setDetail(detail);
        if (status.is5xxServerError()) {
            logger.logStandardProblemDetail(LOG, problemDetail, exception);
        } else {
            logger.warn(LOG, "Request rejected: " + detail);
        }
        return problemDetail;
    }

    private HttpStatusCode resolveStatus(final SystemException e) {
        final Integer code = e.getStatusCode();
        if (code == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        try {
            return HttpStatusCode.valueOf(code);
        } catch (final IllegalArgumentException iae) {
            logger.warn(LOG, "Status code could not be mapped to a valid HttpStatus Code - " + code);
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

    private static String messageOf(final Exception e) {
        final String message = e.getMessage();
        return message == null || message.isBlank() ? DEFAULT_MESSAGE : message;
    }

    /**
     * Uses only the parameter name (not the "method.parameter" path) so internals aren't exposed.
     */
    private static String describe(final ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
            .map(violation -> {
                final String path = violation.getPropertyPath().toString();
                return path.substring(path.lastIndexOf('.') + 1) + " " + violation.getMessage();
            })
            .sorted()
            .collect(Collectors.joining("; "));
    }
}



