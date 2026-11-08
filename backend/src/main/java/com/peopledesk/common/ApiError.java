package com.peopledesk.common;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned by every API endpoint.
 *
 * @param fieldErrors validation messages keyed by request field name (only for 400/409 responses)
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, null);
    }

    public static ApiError withFields(int status, String error, String message, String path,
                                      Map<String, String> fieldErrors) {
        return new ApiError(Instant.now(), status, error, message, path, fieldErrors);
    }
}
