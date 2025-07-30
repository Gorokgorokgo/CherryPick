package com.cherrypick.app.domain.auth;

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
@Tag(name = "인증", description = "회원가입/로그인 API")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/send-code")
    @Operation(summary = "인증 코드 발송", description = "전화번호로 SMS 인증 코드를 발송합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청")
    })
    public ResponseEntity<AuthResponse> sendVerificationCode(
            @Valid @RequestBody PhoneVerificationRequest request) {
        AuthResponse response = authService.sendVerificationCode(request.getPhoneNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "전화번호와 인증코드로 회원가입을 진행합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "인증 실패 또는 중복 사용자")
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
}