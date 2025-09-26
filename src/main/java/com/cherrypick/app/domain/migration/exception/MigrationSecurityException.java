package com.cherrypick.app.domain.migration.exception;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;

/**
 * 마이그레이션 보안 예외
 */
public class MigrationSecurityException extends BusinessException {

    public MigrationSecurityException(String message) {
        super(ErrorCode.MIGRATION_SECURITY_ERROR, message);
    }

    public MigrationSecurityException(String message, Throwable cause) {
        super(ErrorCode.MIGRATION_SECURITY_ERROR, message, cause);
    }
}