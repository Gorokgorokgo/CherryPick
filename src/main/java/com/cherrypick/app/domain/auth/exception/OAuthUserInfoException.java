package com.cherrypick.app.domain.auth.exception;

public class OAuthUserInfoException extends OAuthException {
    
    public OAuthUserInfoException(String provider, String message) {
        super(provider, "USER_INFO_ERROR", message);
    }
    
    public OAuthUserInfoException(String provider, String message, Throwable cause) {
        super(provider, "USER_INFO_ERROR", message, cause);
    }
}