package com.cherrypick.app.domain.auth.exception;

public class InvalidStateException extends OAuthException {
    
    public InvalidStateException(String provider, String state) {
        super(provider, "INVALID_STATE", 
              String.format("Invalid or expired state parameter for %s OAuth: %s", provider, state));
    }
}