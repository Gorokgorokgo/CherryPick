package com.cherrypick.app.common.exception;

public class LocationException extends RuntimeException {
    public LocationException(String message) {
        super(message);
    }

    public LocationException(String message, Throwable cause) {
        super(message, cause);
    }
}
