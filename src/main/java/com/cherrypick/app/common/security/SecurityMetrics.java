package com.cherrypick.app.common.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 보안 관련 메트릭 수집 및 모니터링 컴포넌트
 * 실시간 보안 이벤트 추적 및 임계치 기반 알림 지원
 * Spring Boot Actuator를 통한 기본 메트릭 활용
 */
@Slf4j
@Component
public class SecurityMetrics {
    
    // 기본 보안 메트릭 카운터 (메모리 기반)
    private final AtomicLong authFailureCount = new AtomicLong(0);
    private final AtomicLong accessDeniedCount = new AtomicLong(0);
    private final AtomicLong businessExceptionCount = new AtomicLong(0);
    private final AtomicLong securityViolationCount = new AtomicLong(0);
    
    // 카테고리별 메트릭 추적
    private final ConcurrentHashMap<String, AtomicLong> authFailuresByReason = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> businessExceptionsByCode = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> accessDeniedByResource = new ConcurrentHashMap<>();
    
    // IP별 실패 횟수 추적
    private final ConcurrentHashMap<String, AtomicLong> ipFailureCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> ipLastFailureTime = new ConcurrentHashMap<>();
    
    // 임계치 설정
    private static final int MAX_FAILURES_PER_IP = 5;
    private static final long FAILURE_WINDOW_MINUTES = 5;
    
    /**
     * 인증 실패 메트릭 기록
     */
    public void recordAuthFailure(String reason, String clientIp) {
        authFailureCount.incrementAndGet();
        authFailuresByReason.computeIfAbsent(reason, k -> new AtomicLong(0)).incrementAndGet();
        
        // IP별 실패 횟수 추적
        trackIpFailure(clientIp);
        
        log.debug("Auth failure recorded: reason={}, ip={}, total={}", 
                reason, clientIp, authFailureCount.get());
    }
    
    /**
     * 접근 거부 메트릭 기록
     */
    public void recordAccessDenied(String resource, String clientIp) {
        accessDeniedCount.incrementAndGet();
        accessDeniedByResource.computeIfAbsent(resource, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("Access denied recorded: resource={}, ip={}, total={}", 
                resource, clientIp, accessDeniedCount.get());
    }
    
    /**
     * 비즈니스 예외 메트릭 기록
     */
    public void recordBusinessException(String errorCode, String endpoint) {
        businessExceptionCount.incrementAndGet();
        businessExceptionsByCode.computeIfAbsent(errorCode, k -> new AtomicLong(0)).incrementAndGet();
        
        log.debug("Business exception recorded: code={}, endpoint={}, total={}", 
                errorCode, endpoint, businessExceptionCount.get());
    }
    
    /**
     * 보안 위반 메트릭 기록
     */
    public void recordSecurityViolation(String violationType, String clientIp) {
        securityViolationCount.incrementAndGet();
        
        // 보안 위반은 심각한 이벤트이므로 즉시 알림 처리
        handleCriticalSecurityEvent(violationType, clientIp);
        
        log.warn("Security violation recorded: type={}, ip={}, total={}", 
                violationType, clientIp, securityViolationCount.get());
    }
    
    /**
     * IP별 실패 횟수 추적
     */
    private void trackIpFailure(String clientIp) {
        long currentTime = System.currentTimeMillis();
        
        // 이전 실패 시간 확인
        Long lastFailureTime = ipLastFailureTime.get(clientIp);
        if (lastFailureTime != null) {
            long timeDiff = currentTime - lastFailureTime;
            if (timeDiff > FAILURE_WINDOW_MINUTES * 60 * 1000) {
                // 윈도우 시간이 지났으면 카운트 리셋
                ipFailureCount.put(clientIp, new AtomicLong(1));
            } else {
                // 윈도우 시간 내라면 카운트 증가
                AtomicLong count = ipFailureCount.computeIfAbsent(clientIp, k -> new AtomicLong(0));
                long newCount = count.incrementAndGet();
                
                // 임계치 초과 시 알림
                if (newCount >= MAX_FAILURES_PER_IP) {
                    handleSuspiciousIpActivity(clientIp, newCount);
                }
            }
        } else {
            ipFailureCount.put(clientIp, new AtomicLong(1));
        }
        
        ipLastFailureTime.put(clientIp, currentTime);
    }
    
    /**
     * 의심스러운 IP 활동 처리
     */
    private void handleSuspiciousIpActivity(String clientIp, long failureCount) {
        log.error("Suspicious IP activity detected: ip={}, failures={}, window={}min", 
                 clientIp, failureCount, FAILURE_WINDOW_MINUTES);
        
        // 여기서 추가적인 보안 조치를 취할 수 있음
        // 예: IP 차단, 관리자 알림, 보안팀 통보 등
        
        // 메트릭에 의심스러운 IP 기록 (별도 카운터)
        log.info("Suspicious IP recorded: {}", maskIpAddress(clientIp));
    }
    
    /**
     * 심각한 보안 이벤트 처리
     */
    private void handleCriticalSecurityEvent(String violationType, String clientIp) {
        log.error("CRITICAL: Security violation detected: type={}, ip={}", violationType, clientIp);
        
        // 심각한 보안 이벤트에 대한 즉시 대응
        // 예: 자동 IP 차단, 긴급 알림 발송, 보안팀 호출 등
        
        // 메트릭에 심각한 이벤트 기록
        log.error("Critical security event recorded: type={}, ip={}", 
                violationType, maskIpAddress(clientIp));
    }
    
    /**
     * IP 주소 마스킹 (보안을 위해)
     */
    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        
        // IPv4 주소 마스킹 (마지막 옥텟만 마스킹)
        if (ipAddress.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = ipAddress.split("\\.");
            return parts[0] + "." + parts[1] + "." + parts[2] + ".***";
        }
        
        // IPv6나 기타 형태의 경우 간단히 마스킹
        return ipAddress.substring(0, Math.min(8, ipAddress.length())) + "***";
    }
    
    /**
     * 현재 보안 상태 요약 조회
     */
    public SecurityStatus getCurrentSecurityStatus() {
        return SecurityStatus.builder()
                .authFailures(authFailureCount.get())
                .accessDeniedCount(accessDeniedCount.get())
                .businessExceptions(businessExceptionCount.get())
                .securityViolations(securityViolationCount.get())
                .suspiciousIpCount(ipFailureCount.size())
                .averageAuthDuration(0.0) // 추후 Timer 구현 시 사용
                .build();
    }
    
    /**
     * 상세 메트릭 조회
     */
    public DetailedSecurityMetrics getDetailedMetrics() {
        return DetailedSecurityMetrics.builder()
                .authFailuresByReason(new ConcurrentHashMap<>(authFailuresByReason))
                .businessExceptionsByCode(new ConcurrentHashMap<>(businessExceptionsByCode))
                .accessDeniedByResource(new ConcurrentHashMap<>(accessDeniedByResource))
                .suspiciousIpCount(ipFailureCount.size())
                .totalAuthFailures(authFailureCount.get())
                .totalAccessDenied(accessDeniedCount.get())
                .totalBusinessExceptions(businessExceptionCount.get())
                .totalSecurityViolations(securityViolationCount.get())
                .build();
    }
    
    /**
     * 보안 상태 요약 정보
     */
    public static class SecurityStatus {
        private final double authFailures;
        private final double accessDeniedCount;
        private final double businessExceptions;
        private final double securityViolations;
        private final int suspiciousIpCount;
        private final double averageAuthDuration;
        
        private SecurityStatus(Builder builder) {
            this.authFailures = builder.authFailures;
            this.accessDeniedCount = builder.accessDeniedCount;
            this.businessExceptions = builder.businessExceptions;
            this.securityViolations = builder.securityViolations;
            this.suspiciousIpCount = builder.suspiciousIpCount;
            this.averageAuthDuration = builder.averageAuthDuration;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public double getAuthFailures() { return authFailures; }
        public double getAccessDeniedCount() { return accessDeniedCount; }
        public double getBusinessExceptions() { return businessExceptions; }
        public double getSecurityViolations() { return securityViolations; }
        public int getSuspiciousIpCount() { return suspiciousIpCount; }
        public double getAverageAuthDuration() { return averageAuthDuration; }
        
        public static class Builder {
            private double authFailures;
            private double accessDeniedCount;
            private double businessExceptions;
            private double securityViolations;
            private int suspiciousIpCount;
            private double averageAuthDuration;
            
            public Builder authFailures(double authFailures) {
                this.authFailures = authFailures;
                return this;
            }
            
            public Builder accessDeniedCount(double accessDeniedCount) {
                this.accessDeniedCount = accessDeniedCount;
                return this;
            }
            
            public Builder businessExceptions(double businessExceptions) {
                this.businessExceptions = businessExceptions;
                return this;
            }
            
            public Builder securityViolations(double securityViolations) {
                this.securityViolations = securityViolations;
                return this;
            }
            
            public Builder suspiciousIpCount(int suspiciousIpCount) {
                this.suspiciousIpCount = suspiciousIpCount;
                return this;
            }
            
            public Builder averageAuthDuration(double averageAuthDuration) {
                this.averageAuthDuration = averageAuthDuration;
                return this;
            }
            
            public SecurityStatus build() {
                return new SecurityStatus(this);
            }
        }
    }
    
    /**
     * 상세 보안 메트릭 정보
     */
    public static class DetailedSecurityMetrics {
        private final ConcurrentHashMap<String, AtomicLong> authFailuresByReason;
        private final ConcurrentHashMap<String, AtomicLong> businessExceptionsByCode;
        private final ConcurrentHashMap<String, AtomicLong> accessDeniedByResource;
        private final int suspiciousIpCount;
        private final long totalAuthFailures;
        private final long totalAccessDenied;
        private final long totalBusinessExceptions;
        private final long totalSecurityViolations;
        
        private DetailedSecurityMetrics(Builder builder) {
            this.authFailuresByReason = builder.authFailuresByReason;
            this.businessExceptionsByCode = builder.businessExceptionsByCode;
            this.accessDeniedByResource = builder.accessDeniedByResource;
            this.suspiciousIpCount = builder.suspiciousIpCount;
            this.totalAuthFailures = builder.totalAuthFailures;
            this.totalAccessDenied = builder.totalAccessDenied;
            this.totalBusinessExceptions = builder.totalBusinessExceptions;
            this.totalSecurityViolations = builder.totalSecurityViolations;
        }
        
        public static Builder builder() {
            return new Builder();
        }
        
        // Getters
        public ConcurrentHashMap<String, AtomicLong> getAuthFailuresByReason() { return authFailuresByReason; }
        public ConcurrentHashMap<String, AtomicLong> getBusinessExceptionsByCode() { return businessExceptionsByCode; }
        public ConcurrentHashMap<String, AtomicLong> getAccessDeniedByResource() { return accessDeniedByResource; }
        public int getSuspiciousIpCount() { return suspiciousIpCount; }
        public long getTotalAuthFailures() { return totalAuthFailures; }
        public long getTotalAccessDenied() { return totalAccessDenied; }
        public long getTotalBusinessExceptions() { return totalBusinessExceptions; }
        public long getTotalSecurityViolations() { return totalSecurityViolations; }
        
        public static class Builder {
            private ConcurrentHashMap<String, AtomicLong> authFailuresByReason;
            private ConcurrentHashMap<String, AtomicLong> businessExceptionsByCode;
            private ConcurrentHashMap<String, AtomicLong> accessDeniedByResource;
            private int suspiciousIpCount;
            private long totalAuthFailures;
            private long totalAccessDenied;
            private long totalBusinessExceptions;
            private long totalSecurityViolations;
            
            public Builder authFailuresByReason(ConcurrentHashMap<String, AtomicLong> authFailuresByReason) {
                this.authFailuresByReason = authFailuresByReason;
                return this;
            }
            
            public Builder businessExceptionsByCode(ConcurrentHashMap<String, AtomicLong> businessExceptionsByCode) {
                this.businessExceptionsByCode = businessExceptionsByCode;
                return this;
            }
            
            public Builder accessDeniedByResource(ConcurrentHashMap<String, AtomicLong> accessDeniedByResource) {
                this.accessDeniedByResource = accessDeniedByResource;
                return this;
            }
            
            public Builder suspiciousIpCount(int suspiciousIpCount) {
                this.suspiciousIpCount = suspiciousIpCount;
                return this;
            }
            
            public Builder totalAuthFailures(long totalAuthFailures) {
                this.totalAuthFailures = totalAuthFailures;
                return this;
            }
            
            public Builder totalAccessDenied(long totalAccessDenied) {
                this.totalAccessDenied = totalAccessDenied;
                return this;
            }
            
            public Builder totalBusinessExceptions(long totalBusinessExceptions) {
                this.totalBusinessExceptions = totalBusinessExceptions;
                return this;
            }
            
            public Builder totalSecurityViolations(long totalSecurityViolations) {
                this.totalSecurityViolations = totalSecurityViolations;
                return this;
            }
            
            public DetailedSecurityMetrics build() {
                return new DetailedSecurityMetrics(this);
            }
        }
    }
}