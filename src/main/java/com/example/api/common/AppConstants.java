package com.example.api.common;

public final class AppConstants {
    public static final String DEFAULT_ERROR_MESSAGE = "An error occurred, please see logs for details.";

    public static final String ERROR_MESSAGE_FORMAT = "traceId: %s, Message: %s";

    private AppConstants() { throw new IllegalStateException("Utility class."); }
}
