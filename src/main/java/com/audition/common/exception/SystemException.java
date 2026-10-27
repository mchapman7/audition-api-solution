package com.audition.common.exception;

import lombok.Getter;

@Getter
public class SystemException extends RuntimeException {

    private static final long serialVersionUID = -5876728854007114881L;

    private final Integer statusCode;
    private final String title;

    public SystemException(final String message, final String title, final int statusCode) {
        this(message, title, statusCode, null);
    }

    public SystemException(final String message, final String title, final int statusCode,
        final Throwable cause) {
        super(message, cause);
        this.title = title;
        this.statusCode = statusCode;
    }
}
