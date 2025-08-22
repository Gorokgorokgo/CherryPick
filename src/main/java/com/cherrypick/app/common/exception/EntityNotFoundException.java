package com.cherrypick.app.common.exception;

/**
 * 엔티티를 찾을 수 없는 예외
 */
public class EntityNotFoundException extends BusinessException {
    
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public static EntityNotFoundException user() {
        return new EntityNotFoundException(ErrorCode.USER_NOT_FOUND);
    }
    
    public static EntityNotFoundException auction() {
        return new EntityNotFoundException(ErrorCode.AUCTION_NOT_FOUND);
    }
    
    public static EntityNotFoundException bid() {
        return new EntityNotFoundException(ErrorCode.BID_NOT_FOUND);
    }
    
    public static EntityNotFoundException account() {
        return new EntityNotFoundException(ErrorCode.ACCOUNT_NOT_FOUND);
    }
    
    public static EntityNotFoundException chatRoom() {
        return new EntityNotFoundException(ErrorCode.ENTITY_NOT_FOUND);
    }
}