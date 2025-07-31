package com.cherrypick.app.common.exception;

import com.cherrypick.app.common.security.SecurityLogger;
import com.cherrypick.app.common.security.SecurityMetrics;
import com.cherrypick.app.config.JwtConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 * 보안을 강화하고 사용자 친화적인 오류 메시지 제공
 * 민감한 정보 노출 방지 및 보안 이벤트 로깅
 * 실시간 보안 메트릭 수집 및 모니터링 지원
 */
@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private final SecurityLogger securityLogger;
    private final SecurityMetrics securityMetrics;
    private final JwtConfig jwtConfig;
    
    /**
     * 비즈니스 로직 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {
        
        Long userId = getCurrentUserId();
        
        // 보안 로깅 및 메트릭 수집
        securityLogger.logBusinessException(e.getErrorCode().getCode(), request.getRequestURI(), userId);
        securityMetrics.recordBusinessException(e.getErrorCode().getCode(), request.getRequestURI());
        
        ErrorResponse response = ErrorResponse.of(e.getErrorCode(), request.getRequestURI());
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(response);
    }
    
    /**
     * Bean Validation 예외 처리 (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException e, HttpServletRequest request) {
        
        log.warn("Validation failed at {}: {}", request.getRequestURI(), e.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, 
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * Bean Validation 예외 처리 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(
            BindException e, HttpServletRequest request) {
        
        log.warn("Bind validation failed at {}: {}", request.getRequestURI(), e.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ErrorResponse.FieldError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 제약 조건 위반 예외 처리
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException e, HttpServletRequest request) {
        
        log.warn("Constraint violation at {}: {}", request.getRequestURI(), e.getMessage());
        
        List<ErrorResponse.FieldError> fieldErrors = e.getConstraintViolations()
                .stream()
                .map(violation -> new ErrorResponse.FieldError(
                        violation.getPropertyPath().toString(),
                        violation.getInvalidValue(),
                        violation.getMessage()))
                .collect(Collectors.toList());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE,
                request.getRequestURI(), fieldErrors);
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 타입 미스매치 예외 처리
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        
        log.warn("Type mismatch at {}: parameter '{}' with value '{}'", 
                request.getRequestURI(), e.getName(), e.getValue());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_TYPE_VALUE, request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * HTTP 메서드 불허 예외 처리
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        
        log.warn("Method not allowed at {}: {} method not supported", 
                request.getRequestURI(), e.getMethod());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }
    
    /**
     * Spring Security 인증 예외 처리
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException e, HttpServletRequest request) {
        
        String reason = e.getClass().getSimpleName();
        
        // 보안 로깅 및 메트릭 수집
        securityLogger.logAuthenticationFailure("unknown", reason);
        securityMetrics.recordAuthFailure(reason, getClientIp(request));
        
        ErrorCode errorCode = (e instanceof BadCredentialsException) ? 
                ErrorCode.INVALID_CREDENTIALS : ErrorCode.UNAUTHORIZED;
        
        ErrorResponse response = ErrorResponse.of(errorCode, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }
    
    /**
     * Spring Security 접근 거부 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException e, HttpServletRequest request) {
        
        Long userId = getCurrentUserId();
        
        // 보안 로깅 및 메트릭 수집
        securityLogger.logAccessDenied(userId, request.getRequestURI());
        securityMetrics.recordAccessDenied(request.getRequestURI(), getClientIp(request));
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.ACCESS_DENIED, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }
    
    /**
     * 파일 업로드 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    protected ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e, HttpServletRequest request) {
        
        log.warn("File size exceeded at {}: max size is {}", 
                request.getRequestURI(), e.getMaxUploadSize());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.FILE_SIZE_EXCEEDED, request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * IllegalArgumentException 처리 (기존 코드 호환성)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException e, HttpServletRequest request) {
        
        log.warn("IllegalArgumentException at {}: {}", request.getRequestURI(), e.getMessage());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INVALID_INPUT_VALUE, request.getRequestURI());
        return ResponseEntity.badRequest().body(response);
    }
    
    /**
     * 기타 모든 예외 처리 (보안상 상세 정보 노출 방지)
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(
            Exception e, HttpServletRequest request) {
        
        Long userId = getCurrentUserId();
        
        // 보안상 민감한 정보가 포함될 수 있으므로 상세 로깅
        log.error("Unexpected error at {}: {}", request.getRequestURI(), e.getMessage(), e);
        
        // 예상치 못한 예외는 보안 위험 가능성이 있으므로 별도 추적
        securityLogger.logSuspiciousActivity("unexpected_exception", userId, 
                "Exception: " + e.getClass().getSimpleName());
        
        ErrorResponse response = ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    
    /**
     * 현재 인증된 사용자 ID 조회
     */
    private Long getCurrentUserId() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                
                // JWT 토큰에서 userId 클레임 직접 추출
                if (authentication.getDetails() instanceof String) {
                    String token = (String) authentication.getDetails();
                    return jwtConfig.extractUserId(token);
                }
                
                // 백업: Subject가 숫자인 경우 (기존 로직)
                String name = authentication.getName();
                if (name.matches("\\d+")) {
                    return Long.parseLong(name);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}