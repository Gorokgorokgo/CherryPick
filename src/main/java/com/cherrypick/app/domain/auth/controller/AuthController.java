package com.cherrypick.app.domain.auth.controller;

import com.cherrypick.app.domain.auth.service.AuthService;
import com.cherrypick.app.domain.auth.dto.request.PhoneVerificationRequest;
import com.cherrypick.app.domain.auth.dto.request.VerifyCodeRequest;
import com.cherrypick.app.domain.auth.dto.request.SignupRequest;
import com.cherrypick.app.domain.auth.dto.request.LoginRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "1단계 - 인증", description = "전화번호 기반 회원가입/로그인 | 모든 서비스 이용의 첫 번째 단계")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    @Operation(summary = "인증 코드 발송", 
               description = """
                   전화번호로 SMS 인증 코드를 발송합니다.
                   
                   **사용 예시:**
                   ```json
                   {
                     "phoneNumber": "01012345678"
                   }
                   ```
                   
                   **주의사항:**
                   - 전화번호는 하이픈(-) 없이 입력
                   - 1일 최대 5회 발송 제한
                   - 인증 코드 유효시간: 3분
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 전화번호 형식 또는 일일 한도 초과")
    })
    public ResponseEntity<AuthResponse> sendVerificationCode(
            @Valid @RequestBody PhoneVerificationRequest request) {
        AuthResponse response = authService.sendVerificationCode(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-code")
    @Operation(summary = "인증 코드 검증", description = "발송받은 인증 코드를 검증합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패 또는 만료")
    })
    public ResponseEntity<AuthResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        AuthResponse response = authService.verifyCode(request);
        
        if (response.getMessage().contains("완료")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", 
               description = """
                   전화번호 인증 후 회원가입을 진행합니다.
                   
                   **사용 예시:**
                   ```json
                   {
                     "phoneNumber": "01012345678",
                     "verificationCode": "123456",
                     "email": "user@cherrypick.com",
                     "nickname": "체리유저",
                     "agreeToTerms": true,
                     "agreeToPrivacy": true
                   }
                   ```
                   
                   **응답 예시:**
                   ```json
                   {
                     "message": "회원가입이 완료되었습니다",
                     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                     "user": {
                       "id": 1,
                       "email": "user@cherrypick.com",
                       "nickname": "체리유저"
                     }
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공 - JWT 토큰 발급"),
            @ApiResponse(responseCode = "400", description = "인증 코드 만료/오류, 중복 사용자, 필수 약관 미동의")
    })
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        System.out.println("AuthController.signup - Request received: " + request.getEmail()); // 디버깅용
        AuthResponse response = authService.signup(request);
        
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "전화번호와 인증코드로 로그인을 진행합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패 또는 미등록 사용자")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
}