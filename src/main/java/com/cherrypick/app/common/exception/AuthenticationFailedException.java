package com.cherrypick.app.common.exception;

/**
 * 인증 실패 예외
 */
public class AuthenticationFailedException extends BusinessException {
    
    public AuthenticationFailedException() {
        super(ErrorCode.UNAUTHORIZED);
    }
    
    public AuthenticationFailedException(ErrorCode errorCode) {
        super(errorCode);
    }
}