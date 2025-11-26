package com.cherrypick.app.domain.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "인증 응답")
public class AuthResponse {

    @Schema(description = "JWT 인증 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 닉네임", example = "체리픽유저")
    private String nickname;
    
    @Schema(description = "사용자 이메일", example = "user@example.com")
    private String email;

    @Schema(description = "프로필 이미지 URL", example = "https://s3.amazonaws.com/...")
    private String profileImageUrl;

    @Schema(description = "거래 지역", example = "서울시 강남구")
    private String address;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private String bio;

    @Schema(description = "응답 메시지", example = "로그인 성공")
    private String message;

    // 성공 시 사용하는 생성자 (기본)
    public AuthResponse(String token, Long userId, String email, String nickname) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.message = "로그인 성공";
    }

    // 성공 시 사용하는 생성자 (메시지 포함)
    public AuthResponse(String token, Long userId, String email, String nickname, String message) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.message = message;
    }

    // 성공 시 사용하는 생성자 (전체 프로필 포함)
    public AuthResponse(String token, Long userId, String email, String nickname, 
                       String profileImageUrl, String address, String bio, String message) {
        this.token = token;
        this.userId = userId;
        this.email = email;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.address = address;
        this.bio = bio;
        this.message = message;
    }

    // 실패 시 사용하는 생성자
    public AuthResponse(String message) {
        this.message = message;
    }
}