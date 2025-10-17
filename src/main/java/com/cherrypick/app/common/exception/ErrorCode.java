package com.cherrypick.app.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 시스템 전체 에러 코드 정의
 * 보안을 위해 민감한 정보 노출 방지, 사용자 친화적 메시지 제공
 */
public enum ErrorCode {
    
    // 공통 에러
    INVALID_INPUT_VALUE("C001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    INVALID_TYPE_VALUE("C002", "타입이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    ENTITY_NOT_FOUND("C003", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("C004", "허용되지 않은 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    ACCESS_DENIED("C005", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INTERNAL_SERVER_ERROR("C006", "내부 서버 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("C007", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    FORBIDDEN("C008", "접근이 금지되었습니다.", HttpStatus.FORBIDDEN),
    CONCURRENCY_CONFLICT("C009", "다른 입찰이 먼저 처리되었습니다. 다시 시도해주세요.", HttpStatus.CONFLICT),
    
    // 인증/인가
    UNAUTHORIZED("A001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN("A002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED("A003", "토큰이 만료되었습니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("A004", "인증 정보가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    PHONE_VERIFICATION_REQUIRED("A005", "전화번호 인증이 필요합니다.", HttpStatus.BAD_REQUEST),
    VERIFICATION_CODE_EXPIRED("A006", "인증 코드가 만료되었습니다.", HttpStatus.BAD_REQUEST),
    INVALID_VERIFICATION_CODE("A007", "인증 코드가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    
    // 사용자 관리
    USER_NOT_FOUND("U001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_NICKNAME("U002", "이미 사용 중인 닉네임입니다.", HttpStatus.CONFLICT),
    DUPLICATE_PHONE_NUMBER("U003", "이미 등록된 전화번호입니다.", HttpStatus.CONFLICT),
    
    // 계좌 관리
    ACCOUNT_NOT_FOUND("AC001", "계좌를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ACCOUNT_LIMIT_EXCEEDED("AC002", "계좌 등록 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_ACCOUNT("AC003", "이미 등록된 계좌입니다.", HttpStatus.CONFLICT),
    ACCOUNT_ACCESS_DENIED("AC004", "본인의 계좌만 접근할 수 있습니다.", HttpStatus.FORBIDDEN),
    PRIMARY_ACCOUNT_DELETE_DENIED("AC005", "기본 계좌는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_VERIFIED("AC006", "인증되지 않은 계좌입니다.", HttpStatus.BAD_REQUEST),
    
    // 포인트 시스템
    INSUFFICIENT_POINTS("P001", "포인트가 부족합니다.", HttpStatus.BAD_REQUEST),
    INVALID_AMOUNT_UNIT("P002", "금액은 1,000원 단위로 입력해주세요.", HttpStatus.BAD_REQUEST),
    AMOUNT_LIMIT_EXCEEDED("P003", "금액 한도를 초과했습니다.", HttpStatus.BAD_REQUEST),
    PAYMENT_PROCESSING_FAILED("P004", "결제 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    TRANSFER_PROCESSING_FAILED("P005", "송금 처리에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    
    // 경매 시스템
    AUCTION_NOT_FOUND("AU001", "경매를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    AUCTION_NOT_ACTIVE("AU002", "진행 중인 경매가 아닙니다.", HttpStatus.BAD_REQUEST),
    AUCTION_ENDED("AU003", "종료된 경매입니다.", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_DEPOSIT("AU004", "보증금이 부족합니다.", HttpStatus.BAD_REQUEST),
    INVALID_PRICE_RANGE("AU005", "가격 설정이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    REGION_INFO_REQUIRED("AU006", "지역 정보가 필요합니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_ACCESS("AU007", "해당 경매에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN),
    AUCTION_HAS_BIDS("AU008", "입찰이 있는 경매는 수정하거나 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    AUCTION_CANNOT_BE_UPDATED("AU009", "종료되었거나 취소된 경매는 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    AUCTION_CANNOT_BE_DELETED("AU010", "종료되었거나 취소된 경매는 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    AUCTION_MODIFY_RESTRICTED_NEAR_END("AU011", "경매 종료 30분 전부터는 경매를 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    AUCTION_DELETE_RESTRICTED_NEAR_END("AU012", "경매 종료 30분 전부터는 경매를 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    
    // 입찰 시스템
    BID_NOT_FOUND("B001", "입찰을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    SELF_BID_NOT_ALLOWED("B002", "자신의 경매에는 입찰할 수 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_BID_AMOUNT("B003", "입찰 금액이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    NO_BID_EXISTS("B004", "입찰 내역이 없습니다.", HttpStatus.NOT_FOUND),
    
    // 이미지 업로드
    FILE_UPLOAD_FAILED("F001", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_TYPE("F002", "지원하지 않는 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("F003", "파일 크기가 너무 큽니다.", HttpStatus.BAD_REQUEST),
    EMPTY_FILE("F004", "빈 파일은 업로드할 수 없습니다.", HttpStatus.BAD_REQUEST),
    FILE_COUNT_EXCEEDED("F005", "업로드 가능한 파일 수를 초과했습니다.", HttpStatus.BAD_REQUEST),
    
    // 이미지 관리
    IMAGE_NOT_FOUND("I001", "이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    IMAGE_DELETE_ACCESS_DENIED("I002", "본인이 업로드한 이미지만 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    IMAGE_ALREADY_DELETED("I003", "이미 삭제된 이미지입니다.", HttpStatus.BAD_REQUEST),
    
    // Q&A 시스템
    QUESTION_NOT_FOUND("Q001", "질문을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    ANSWER_NOT_FOUND("Q002", "답변을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    QUESTION_CONTENT_EMPTY("Q003", "질문 내용을 입력해주세요.", HttpStatus.BAD_REQUEST),
    QUESTION_CONTENT_TOO_LONG("Q004", "질문은 1,000자 이내로 작성해주세요.", HttpStatus.BAD_REQUEST),
    ANSWER_CONTENT_EMPTY("Q005", "답변 내용을 입력해주세요.", HttpStatus.BAD_REQUEST),
    ANSWER_CONTENT_TOO_LONG("Q006", "답변은 1,000자 이내로 작성해주세요.", HttpStatus.BAD_REQUEST),
    SELF_QUESTION_NOT_ALLOWED("Q007", "본인의 경매에는 질문할 수 없습니다.", HttpStatus.FORBIDDEN),
    QUESTION_TIME_LIMIT_EXCEEDED("Q008", "경매 종료 30분 전부터는 질문할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ANSWERED_QUESTION_CANNOT_MODIFY("Q009", "답변이 달린 질문은 수정하거나 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ONLY_SELLER_CAN_ANSWER("Q010", "판매자만 답변할 수 있습니다.", HttpStatus.FORBIDDEN),
    ANSWER_ALREADY_EXISTS("Q011", "이미 답변이 등록되어 있습니다.", HttpStatus.CONFLICT),
    ANSWER_AFTER_AUCTION_END("Q012", "경매가 종료된 후에는 답변을 수정하거나 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    NOT_QUESTION_AUTHOR("Q013", "질문 작성자만 수정하거나 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    NOT_ANSWER_AUTHOR("Q014", "답변 작성자만 수정하거나 삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
    QNA_MODIFY_RESTRICTED_NEAR_END("Q015", "경매 종료 30분 전부터는 질문/답변을 수정할 수 없습니다.", HttpStatus.BAD_REQUEST),
    QNA_DELETE_RESTRICTED_NEAR_END("Q016", "경매 종료 30분 전부터는 질문/답변을 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    QUESTION_DELETE_RESTRICTED_HAS_ANSWER("Q017", "답변이 달린 질문은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),
    ANSWER_TIME_LIMIT_EXCEEDED("Q018", "경매 종료 30분 전부터는 답변할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 마이그레이션 시스템
    MIGRATION_SECURITY_ERROR("M001", "마이그레이션 보안 오류가 발생했습니다.", HttpStatus.FORBIDDEN),
    MIGRATION_NOT_ELIGIBLE("M002", "마이그레이션 대상이 아닙니다.", HttpStatus.BAD_REQUEST),
    MIGRATION_PHASE_INVALID("M003", "유효하지 않은 마이그레이션 단계입니다.", HttpStatus.BAD_REQUEST),
    MIGRATION_ROLLBACK_LIMIT("M004", "일일 롤백 횟수를 초과했습니다.", HttpStatus.BAD_REQUEST),
    MIGRATION_COOLING_PERIOD("M005", "마이그레이션 쿨링 기간입니다.", HttpStatus.BAD_REQUEST),
    MIGRATION_SYSTEM_UNSAFE("M006", "마이그레이션 시스템이 안전하지 않습니다.", HttpStatus.SERVICE_UNAVAILABLE);
    
    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
    
    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}