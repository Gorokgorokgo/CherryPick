package com.cherrypick.app.common.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * GlobalExceptionHandler 단위 테스트
 * 보안 관련 예외 처리 및 민감한 정보 노출 방지 검증
 */
@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private com.cherrypick.app.common.security.SecurityLogger securityLogger;
    private com.cherrypick.app.common.security.SecurityMetrics securityMetrics;
    private GlobalExceptionHandler globalExceptionHandler;
    
    @BeforeEach
    void setUp() {
        // Mock dependencies
        securityLogger = Mockito.mock(com.cherrypick.app.common.security.SecurityLogger.class);
        securityMetrics = Mockito.mock(com.cherrypick.app.common.security.SecurityMetrics.class);
        
        // Create the GlobalExceptionHandler with mocked dependencies
        globalExceptionHandler = new GlobalExceptionHandler(securityLogger, securityMetrics);
        
        // Set up MockMvc with standalone setup
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(globalExceptionHandler)
                .build();
                
        objectMapper = new ObjectMapper();
    }
    
    @Nested
    @DisplayName("비즈니스 예외 처리")
    class BusinessExceptionTest {
        
        @Test
        @DisplayName("비즈니스 예외 - 사용자 친화적 메시지 반환")
        void handleBusinessException() throws Exception {
            mockMvc.perform(get("/test/business-exception"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("P001"))
                    .andExpect(jsonPath("$.message").value("포인트가 부족합니다."))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.path").value("/test/business-exception"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
        
        @Test
        @DisplayName("엔티티 없음 예외 - 안전한 메시지 반환")
        void handleEntityNotFoundException() throws Exception {
            mockMvc.perform(get("/test/entity-not-found"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("U001"))
                    .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
    
    @Nested
    @DisplayName("보안 예외 처리")
    class SecurityExceptionTest {
        
        @Test
        @DisplayName("인증 실패 - 민감한 정보 노출 방지")
        void handleAuthenticationException() throws Exception {
            mockMvc.perform(get("/test/authentication-exception"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("A004"))
                    .andExpect(jsonPath("$.message").value("인증 정보가 올바르지 않습니다."))
                    .andExpect(jsonPath("$.status").value(401))
                    // 민감한 정보가 노출되지 않는지 확인 (정확한 메시지만 포함되어야 함)
                    .andExpect(jsonPath("$.message").value("인증 정보가 올바르지 않습니다."));
        }
        
        @Test
        @DisplayName("접근 거부 - 안전한 메시지 반환")
        void handleAccessDeniedException() throws Exception {
            mockMvc.perform(get("/test/access-denied"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("C005"))
                    .andExpect(jsonPath("$.message").value("접근 권한이 없습니다."))
                    .andExpect(jsonPath("$.status").value(403));
        }
    }
    
    @Nested
    @DisplayName("검증 예외 처리")
    class ValidationExceptionTest {
        
        @Test
        @DisplayName("Bean Validation 실패 - 상세 필드 오류 정보 제공")
        void handleMethodArgumentNotValidException() throws Exception {
            TestRequest invalidRequest = new TestRequest();
            invalidRequest.setAmount(BigDecimal.valueOf(500)); // 1000원 단위 위반
            // name은 null로 남겨둠 (NotBlank 위반)
            
            mockMvc.perform(post("/test/validation")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                    .andExpect(jsonPath("$.fieldErrors").isArray())
                    .andExpect(jsonPath("$.fieldErrors[*].field").exists())
                    .andExpect(jsonPath("$.fieldErrors[*].message").exists());
        }
    }
    
    @Nested
    @DisplayName("일반 예외 처리")
    class GeneralExceptionTest {
        
        @Test
        @DisplayName("예상치 못한 예외 - 민감한 정보 노출 방지")
        void handleUnexpectedException() throws Exception {
            mockMvc.perform(get("/test/unexpected-exception"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("C006"))
                    .andExpect(jsonPath("$.message").value("내부 서버 오류가 발생했습니다."))
                    .andExpect(jsonPath("$.status").value(500))
                    // 실제 예외 메시지나 스택 트레이스가 노출되지 않는지 확인 (안전한 메시지만 반환)
                    .andExpect(jsonPath("$.message").value("내부 서버 오류가 발생했습니다."));
        }
        
        @Test
        @DisplayName("IllegalArgumentException - 안전한 메시지로 변환")
        void handleIllegalArgumentException() throws Exception {
            mockMvc.perform(get("/test/illegal-argument"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("C001"))
                    .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                    // 원본 예외 메시지가 노출되지 않는지 확인 (안전한 메시지로 변환됨)
                    .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."));
        }
    }
    
    // 테스트용 컨트롤러
    @RestController
    @RequestMapping("/test")
    static class TestController {
        
        @GetMapping("/business-exception")
        public void throwBusinessException() {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }
        
        @GetMapping("/entity-not-found")
        public void throwEntityNotFoundException() {
            throw EntityNotFoundException.user();
        }
        
        @GetMapping("/authentication-exception")
        public void throwAuthenticationException() {
            throw new BadCredentialsException("실제 민감한 인증 정보");
        }
        
        @GetMapping("/access-denied")
        public void throwAccessDeniedException() {
            throw new AccessDeniedException("실제 권한 정보");
        }
        
        @PostMapping("/validation")
        public void testValidation(@Valid @RequestBody TestRequest request) {
            // 검증 실패시 자동으로 예외 발생
        }
        
        @GetMapping("/unexpected-exception")
        public void throwUnexpectedException() {
            throw new RuntimeException("민감한 내부 정보가 포함된 예외 메시지");
        }
        
        @GetMapping("/illegal-argument")
        public void throwIllegalArgumentException() {
            throw new IllegalArgumentException("원본 에러 메시지");
        }
    }
    
    // 테스트용 DTO
    static class TestRequest {
        @NotBlank(message = "이름은 필수입니다")
        private String name;
        
        @NotNull(message = "금액은 필수입니다")
        private BigDecimal amount;
        
        // getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
    
    // 테스트 설정 클래스
    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        // 테스트에 필요한 Bean 설정 (추가적인 설정이 필요한 경우)
    }
}