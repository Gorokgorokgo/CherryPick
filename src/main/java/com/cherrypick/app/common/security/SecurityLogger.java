package com.cherrypick.app.common.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * 보안 관련 로깅을 위한 유틸리티 클래스
 * 민감한 정보 노출 방지 및 표준화된 보안 로그 생성
 */
@Slf4j
@Component
public class SecurityLogger {
    
    /**
     * 인증 실패 로그 기록
     */
    public void logAuthenticationFailure(String username, String reason) {
        String clientIp = getCurrentClientIp();
        log.error("Authentication failed: user={}, ip={}, reason={}", 
                 maskUsername(username), clientIp, reason);
    }
    
    /**
     * 권한 부족 로그 기록
     */
    public void logAccessDenied(Long userId, String resource) {
        String clientIp = getCurrentClientIp();
        log.error("Access denied: userId={}, resource={}, ip={}", 
                 userId, resource, clientIp);
    }
    
    /**
     * 의심스러운 활동 로그 기록
     */
    public void logSuspiciousActivity(String activity, Long userId, String details) {
        String clientIp = getCurrentClientIp();
        log.warn("Suspicious activity detected: activity={}, userId={}, ip={}, details={}", 
                activity, userId, clientIp, details);
    }
    
    /**
     * 비즈니스 예외 로그 기록
     */
    public void logBusinessException(String errorCode, String requestUri, Long userId) {
        String clientIp = getCurrentClientIp();
        log.warn("Business exception occurred: code={}, uri={}, userId={}, ip={}", 
                errorCode, requestUri, userId, clientIp);
    }
    
    /**
     * 보안 위반 시도 로그 기록
     */
    public void logSecurityViolation(String violationType, Long userId, String details) {
        String clientIp = getCurrentClientIp();
        log.error("Security violation detected: type={}, userId={}, ip={}, details={}", 
                 violationType, userId, clientIp, details);
    }
    
    /**
     * 중요한 비즈니스 액션 로그 기록
     */
    public void logBusinessAction(String action, Long userId, String resourceId) {
        log.info("Business action executed: action={}, userId={}, resourceId={}", 
                action, userId, resourceId);
    }
    
    /**
     * 데이터 접근 로그 기록
     */
    public void logDataAccess(String dataType, Long userId, String operation) {
        log.info("Data access: type={}, userId={}, operation={}", 
                dataType, userId, operation);
    }
    
    /**
     * 사용자명 마스킹 (보안을 위해 일부만 표시)
     */
    private String maskUsername(String username) {
        if (username == null || username.length() <= 3) {
            return "***";
        }
        
        // 이메일 형태인 경우
        if (username.contains("@")) {
            String[] parts = username.split("@");
            if (parts[0].length() <= 3) {
                return "***@" + parts[1];
            }
            return parts[0].substring(0, 3) + "***@" + parts[1];
        }
        
        // 일반 사용자명인 경우
        return username.substring(0, 3) + "***";
    }
    
    /**
     * 전화번호 마스킹
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.length() < 8) {
            return "***-****-****";
        }
        
        String digits = phoneNumber.replaceAll("[^0-9]", "");
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-****-" + digits.substring(7);
        } else if (digits.length() == 10) {
            return digits.substring(0, 3) + "-***-" + digits.substring(6);
        }
        
        return "***-****-****";
    }
    
    /**
     * 계좌번호 마스킹 (마지막 4자리만 표시)
     */
    public static String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****-****-****";
        }
        
        String digits = accountNumber.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            return "****-****-" + digits.substring(digits.length() - 4);
        }
        
        return "****-****-****";
    }
    
    /**
     * 이메일 마스킹
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.com";
        }
        
        String[] parts = email.split("@");
        String localPart = parts[0];
        String domainPart = parts[1];
        
        if (localPart.length() <= 3) {
            return "***@" + domainPart;
        }
        
        return localPart.substring(0, 3) + "***@" + domainPart;
    }
    
    /**
     * 현재 요청의 클라이언트 IP 주소 획득
     */
    private String getCurrentClientIp() {
        return getCurrentRequest()
                .map(this::extractClientIp)
                .orElse("unknown");
    }
    
    /**
     * 현재 HTTP 요청 객체 획득
     */
    private Optional<HttpServletRequest> getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return Optional.of(attributes.getRequest());
        } catch (IllegalStateException e) {
            // 요청 컨텍스트가 없는 경우 (비동기 작업, 배치 작업 등)
            return Optional.empty();
        }
    }
    
    /**
     * HTTP 요청에서 실제 클라이언트 IP 추출
     * 프록시, 로드밸런서를 고려한 IP 추출
     */
    private String extractClientIp(HttpServletRequest request) {
        // X-Forwarded-For 헤더 확인 (프록시를 통한 요청)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // 여러 IP가 있는 경우 첫 번째가 실제 클라이언트 IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        // X-Real-IP 헤더 확인
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        // Proxy-Client-IP 헤더 확인
        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if (proxyClientIp != null && !proxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(proxyClientIp)) {
            return proxyClientIp;
        }
        
        // WL-Proxy-Client-IP 헤더 확인 (WebLogic)
        String wlProxyClientIp = request.getHeader("WL-Proxy-Client-IP");
        if (wlProxyClientIp != null && !wlProxyClientIp.isEmpty() && !"unknown".equalsIgnoreCase(wlProxyClientIp)) {
            return wlProxyClientIp;
        }
        
        // HTTP_CLIENT_IP 헤더 확인
        String httpClientIp = request.getHeader("HTTP_CLIENT_IP");
        if (httpClientIp != null && !httpClientIp.isEmpty() && !"unknown".equalsIgnoreCase(httpClientIp)) {
            return httpClientIp;
        }
        
        // HTTP_X_FORWARDED_FOR 헤더 확인
        String httpXForwardedFor = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (httpXForwardedFor != null && !httpXForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(httpXForwardedFor)) {
            return httpXForwardedFor;
        }
        
        // 위의 모든 헤더가 없으면 기본 RemoteAddr 사용
        return request.getRemoteAddr();
    }
}