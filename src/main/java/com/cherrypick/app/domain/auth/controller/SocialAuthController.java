package com.cherrypick.app.domain.auth.controller;

import com.cherrypick.app.domain.auth.dto.request.SocialLoginRequest;
import com.cherrypick.app.domain.auth.dto.request.SocialSignupRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.auth.dto.response.SocialUserInfo;
import com.cherrypick.app.domain.auth.service.SocialAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/social")
@CrossOrigin(origins = "*")
@Tag(name = "2단계 - 소셜 로그인", description = "Google, Kakao, Naver 소셜 로그인 | 간편하고 빠른 가입/로그인")
public class SocialAuthController {

    private final SocialAuthService socialAuthService;

    public SocialAuthController(SocialAuthService socialAuthService) {
        this.socialAuthService = socialAuthService;
    }

    @PostMapping("/check")
    @Operation(summary = "소셜 계정 확인", 
               description = """
                   소셜 계정이 이미 가입되어 있는지 확인합니다.
                   
                   **사용 예시:**
                   ```json
                   {
                     "provider": "GOOGLE",
                     "code": "4/0AX4XfWiX...",
                     "redirectUri": "http://localhost:3000/auth/callback"
                   }
                   ```
                   
                   **응답:**
                   - isExistingUser: true → 로그인 가능
                   - isExistingUser: false → 회원가입 필요
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소셜 계정 정보 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 OAuth 코드 또는 제공자")
    })
    public ResponseEntity<SocialUserInfo> checkSocialAccount(@Valid @RequestBody SocialLoginRequest request) {
        try {
            SocialUserInfo userInfo = socialAuthService.getSocialUserInfo(request);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/login")
    @Operation(summary = "소셜 로그인", 
               description = """
                   이미 가입된 소셜 계정으로 로그인합니다.
                   계정이 없으면 **자동으로 회원가입** 처리됩니다.
                   
                   **지원 제공자:**
                   - GOOGLE: Google 계정
                   - KAKAO: 카카오 계정  
                   - NAVER: 네이버 계정
                   
                   **사용 예시:**
                   ```json
                   {
                     "provider": "GOOGLE",
                     "code": "4/0AX4XfWiX...",
                     "redirectUri": "http://localhost:3000/auth/callback",
                     "state": "random-state-string"
                   }
                   ```
                   
                   **성공 응답:**
                   ```json
                   {
                     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                     "user": {
                       "id": 1,
                       "email": "user@gmail.com",
                       "nickname": "체리유저"
                     },
                     "message": "로그인 성공"
                   }
                   ```
                   """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "소셜 로그인 성공 - JWT 토큰 발급"),
            @ApiResponse(responseCode = "400", description = "OAuth 인증 실패")
    })
    public ResponseEntity<AuthResponse> socialLogin(@Valid @RequestBody SocialLoginRequest request) {
        AuthResponse response = socialAuthService.socialLogin(request);
        
        if (response.getToken() != null) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "지원하는 소셜 로그인 제공자 목록", 
               description = "체리픽에서 지원하는 소셜 로그인 제공자 목록을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "지원 제공자 목록")
    public ResponseEntity<String[]> getSupportedProviders() {
        String[] providers = {"GOOGLE", "KAKAO", "NAVER"};
        return ResponseEntity.ok(providers);
    }
}