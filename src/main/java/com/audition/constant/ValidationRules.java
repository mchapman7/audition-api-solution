package com.audition.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ValidationRules {

    /**
     * Positive integer, at most 9 digits (bounds the length of values substituted into upstream URLs).
     */
    public static final String POSITIVE_INTEGER = "^[1-9][0-9]{0,8}$";
    public static final int MAX_FILTER_LENGTH = 100;
}
