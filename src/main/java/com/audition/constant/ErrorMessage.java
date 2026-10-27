package com.audition.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ErrorMessage {

    // Titles
    public static final String DEFAULT_TITLE = "API Error Occurred";
    public static final String VALIDATION_TITLE = "Invalid Request";
    public static final String RESOURCE_NOT_FOUND = "Resource Not Found";
    public static final String HTTP_CLIENT_ERROR = "Upstream Client Error";
    public static final String UPSTREAM_ERROR = "Upstream Service Error";
    public static final String UPSTREAM_UNAVAILABLE = "Upstream Service Unavailable";
    public static final String UNEXPECTED_ERROR = "Unexpected Error";

    // Messages
    public static final String DEFAULT_MESSAGE = "API Error occurred. Please contact support or administrator.";
    public static final String POSITIVE_INTEGER_MESSAGE = "must be a positive whole number";

}
