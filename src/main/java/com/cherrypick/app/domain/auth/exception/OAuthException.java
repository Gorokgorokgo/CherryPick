package com.cherrypick.app.domain.auth.exception;

public class OAuthException extends RuntimeException {
    
    private final String provider;
    private final String errorCode;

    public OAuthException(String provider, String errorCode, String message) {
        super(message);
        this.provider = provider;
        this.errorCode = errorCode;
    }

    public OAuthException(String provider, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.provider = provider;
        this.errorCode = errorCode;
    }

    public String getProvider() {
        return provider;
    }

    public String getErrorCode() {
        return errorCode;
    }
}