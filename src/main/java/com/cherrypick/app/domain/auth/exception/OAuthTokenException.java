package com.cherrypick.app.domain.auth.exception;

public class OAuthTokenException extends OAuthException {
    
    public OAuthTokenException(String provider, String message) {
        super(provider, "TOKEN_ERROR", message);
    }
    
    public OAuthTokenException(String provider, String message, Throwable cause) {
        super(provider, "TOKEN_ERROR", message, cause);
    }
}