package com.cherrypick.app.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 표준 API 응답 래퍼 클래스
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "표준 API 응답")
public class ApiResponse<T> {

    @Schema(description = "성공 여부", example = "true")
    private boolean success;

    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다")
    private String message;

    @Schema(description = "응답 데이터")
    private T data;

    @Schema(description = "오류 정보")
    private ErrorInfo error;

    @Schema(description = "응답 시간")
    private LocalDateTime timestamp;

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .error(ErrorInfo.builder()
                        .code(errorCode)
                        .message(message)
                        .timestamp(LocalDateTime.now())
                        .build())
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 오류 정보 클래스
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "오류 정보")
    public static class ErrorInfo {
        @Schema(description = "오류 코드", example = "MIGRATION_001")
        private String code;

        @Schema(description = "오류 메시지", example = "사용자가 마이그레이션 대상이 아닙니다")
        private String message;

        @Schema(description = "오류 발생 시간")
        private LocalDateTime timestamp;

        @Schema(description = "추가 상세 정보")
        private Object details;
    }
}