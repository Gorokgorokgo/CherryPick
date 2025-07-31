package com.cherrypick.app.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "인증 응답")
public class AuthResponse {
    
    @Schema(description = "JWT 인증 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;
    
    @Schema(description = "사용자 ID", example = "1")
    private Long userId;
    
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;
    
    @Schema(description = "사용자 닉네임", example = "체리픽유저")
    private String nickname;
    
    @Schema(description = "응답 메시지", example = "로그인 성공")
    private String message;
    
    public AuthResponse(String token, Long userId, String phoneNumber, String nickname) {
        this.token = token;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
        this.message = "로그인 성공";
    }
    
    public AuthResponse(String token, Long userId, String phoneNumber, String nickname, String message) {
        this.token = token;
        this.userId = userId;
        this.phoneNumber = phoneNumber;
        this.nickname = nickname;
        this.message = message;
    }
    
    public AuthResponse(String message) {
        this.message = message;
    }
}