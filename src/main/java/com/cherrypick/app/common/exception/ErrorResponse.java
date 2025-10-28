package com.cherrypick.app.common.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API 에러 응답 표준 형식
 * 보안을 위해 민감한 정보는 제외하고 사용자 친화적인 메시지만 포함
 */
@Schema(description = "API 에러 응답")
public class ErrorResponse {
    
    @Schema(description = "에러 코드", example = "C001")
    private String code;
    
    @Schema(description = "에러 메시지", example = "입력값이 올바르지 않습니다.")
    private String message;
    
    @Schema(description = "HTTP 상태 코드", example = "400")
    private int status;
    
    @Schema(description = "발생 시간", example = "2024-12-25T14:30:00")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @Schema(description = "요청 경로", example = "/api/auctions")
    private String path;
    
    @Schema(description = "상세 검증 오류 목록")
    private List<FieldError> fieldErrors;
    
    private ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }
    
    public static ErrorResponse of(ErrorCode errorCode, String path) {
        ErrorResponse response = new ErrorResponse();
        response.code = errorCode.getCode();
        response.message = errorCode.getMessage();
        response.status = errorCode.getHttpStatus().value();
        response.path = path;
        return response;
    }

    public static ErrorResponse of(ErrorCode errorCode, String path, String customMessage) {
        ErrorResponse response = new ErrorResponse();
        response.code = errorCode.getCode();
        response.message = customMessage;
        response.status = errorCode.getHttpStatus().value();
        response.path = path;
        return response;
    }
    
    public static ErrorResponse of(ErrorCode errorCode, String path, List<FieldError> fieldErrors) {
        ErrorResponse response = of(errorCode, path);
        response.fieldErrors = fieldErrors;
        return response;
    }
    
    @Schema(description = "필드 검증 오류")
    public static class FieldError {
        @Schema(description = "필드명", example = "bidAmount")
        private String field;
        
        @Schema(description = "입력된 값", example = "500")
        private Object rejectedValue;
        
        @Schema(description = "오류 메시지", example = "입찰 금액은 최소 1,000원 이상이어야 합니다")
        private String message;
        
        public FieldError(String field, Object rejectedValue, String message) {
            this.field = field;
            this.rejectedValue = rejectedValue;
            this.message = message;
        }
        
        // Getters
        public String getField() { return field; }
        public Object getRejectedValue() { return rejectedValue; }
        public String getMessage() { return message; }
    }
    
    // Getters
    public String getCode() { return code; }
    public String getMessage() { return message; }
    public int getStatus() { return status; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getPath() { return path; }
    public List<FieldError> getFieldErrors() { return fieldErrors; }
}