package com.cherrypick.app.domain.auth;

import lombok.Data;

@Data
public class AuthResponse {
    private String token;
    private Long userId;
    private String phoneNumber;
    private String nickname;
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