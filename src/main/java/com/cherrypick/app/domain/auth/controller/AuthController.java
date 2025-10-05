package com.cherrypick.app.domain.auth.controller;

import com.cherrypick.app.domain.auth.service.AuthService;
import com.cherrypick.app.domain.auth.dto.request.PhoneVerificationRequest;
import com.cherrypick.app.domain.auth.dto.request.VerifyCodeRequest;
import com.cherrypick.app.domain.auth.dto.request.SignupRequest;
import com.cherrypick.app.domain.auth.dto.request.LoginRequest;
import com.cherrypick.app.domain.auth.dto.request.PhoneLoginRequest;
import com.cherrypick.app.domain.auth.dto.request.NicknameCheckRequest;
import com.cherrypick.app.domain.auth.dto.request.EmailCheckRequest;
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
                   
                   **필수 정보**: 전화번호, 닉네임, 이메일, 비밀번호
                   **선택 정보**: 실명, 생년월일, 성별, 주소, 우편번호, 자기소개, 프로필 공개 설정
                   
                   선택 정보는 나중에 프로필 수정에서도 변경 가능합니다.
                   
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

    @PostMapping("/phone-login")
    @Operation(summary = "전화번호 로그인",
               description = """
                   전화번호와 인증번호로 로그인합니다.

                   **사용 흐름:**
                   1. `/api/auth/send-code`로 인증번호 발송
                   2. `/api/auth/phone-login`로 인증번호 입력하여 로그인

                   **특징:**
                   - 기존 가입 사용자만 로그인 가능
                   - 인증번호는 5분간 유효
                   - 로그인 성공 시 JWT 토큰 발급

                   **사용 예시:**
                   ```json
                   {
                     "phoneNumber": "01012345678",
                     "verificationCode": "123456"
                   }
                   ```

                   **성공 응답:**
                   ```json
                   {
                     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                     "user": {
                       "id": 1,
                       "email": "user@example.com",
                       "nickname": "체리유저"
                     }
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "전화번호 로그인 성공 - JWT 토큰 발급"),
            @ApiResponse(responseCode = "400", description = "미가입 전화번호, 잘못된 인증번호, 인증번호 만료 등")
    })
    public ResponseEntity<AuthResponse> phoneLogin(@Valid @RequestBody PhoneLoginRequest request) {
        AuthResponse response = authService.phoneLogin(request);

        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/check-nickname")
    @Operation(summary = "닉네임 중복 검사",
               description = """
                   닉네임의 중복 여부를 검사합니다.

                   **사용 예시:**
                   ```json
                   {
                     "nickname": "체리유저"
                   }
                   ```

                   **성공 응답:**
                   ```json
                   {
                     "available": true,
                     "message": "사용 가능한 닉네임입니다."
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "닉네임 중복 검사 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 닉네임 형식")
    })
    public ResponseEntity<AuthResponse> checkNickname(@Valid @RequestBody NicknameCheckRequest request) {
        AuthResponse response = authService.checkNickname(request.getNickname());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/check-email")
    @Operation(summary = "이메일 중복 검사",
               description = """
                   이메일의 중복 여부를 검사합니다.

                   **사용 예시:**
                   ```json
                   {
                     "email": "user@example.com"
                   }
                   ```

                   **성공 응답:**
                   ```json
                   {
                     "available": true,
                     "message": "사용 가능한 이메일입니다."
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이메일 중복 검사 완료"),
            @ApiResponse(responseCode = "400", description = "잘못된 이메일 형식")
    })
    public ResponseEntity<AuthResponse> checkEmail(@Valid @RequestBody EmailCheckRequest request) {
        AuthResponse response = authService.checkEmail(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃",
               description = """
                   현재 사용자를 로그아웃 처리합니다.
                   
                   JWT 토큰 기반 시스템이므로 서버에서 특별한 처리 없이
                   클라이언트에서 토큰 삭제로 로그아웃이 처리됩니다.
                   
                   **응답 예시:**
                   ```json
                   {
                     "message": "로그아웃이 완료되었습니다"
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<AuthResponse> logout() {
        // JWT는 stateless이므로 서버에서 특별한 처리 없이 성공 응답만 반환
        // 실제 토큰 무효화는 클라이언트에서 토큰 삭제로 처리
        AuthResponse response = new AuthResponse("로그아웃이 완료되었습니다");
        return ResponseEntity.ok(response);
    }
}
