package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.domain.auth.component.OAuthResponseValidator;
import com.cherrypick.app.domain.auth.exception.OAuthTokenException;
import com.cherrypick.app.domain.auth.exception.OAuthUserInfoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OAuthClientService {

    private final WebClient.Builder webClientBuilder;
    private final OAuthResponseValidator responseValidator;
    
    public String getAccessToken(String provider, String tokenUrl, Map<String, String> tokenRequest) {
        try {
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            tokenRequest.forEach(formData::add);

            WebClient webClient = webClientBuilder.build();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) webClient
                .post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(formData))
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(responseBody);

            responseValidator.validateTokenResponse(provider, response);
            
            String accessToken = (String) response.getBody().get("access_token");
            log.debug("{} Access Token 획득 성공", provider);
            
            return accessToken;
            
        } catch (Exception e) {
            log.error("{} Access Token 획득 실패: {}", provider, e.getMessage());
            throw new OAuthTokenException(provider, "토큰 획득 중 오류 발생", e);
        }
    }

    public Map<String, Object> getUserInfo(String provider, String userInfoUrl, String accessToken) {
        try {
            WebClient webClient = webClientBuilder.build();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) webClient
                .get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(responseBody);

            responseValidator.validateUserInfoResponse(provider, response);
            
            log.debug("{} 사용자 정보 획득 성공", provider);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("{} 사용자 정보 획득 실패: {}", provider, e.getMessage());
            throw new OAuthUserInfoException(provider, "사용자 정보 획득 중 오류 발생", e);
        }
    }

    public Map<String, Object> getKakaoUserInfo(String provider, String userInfoUrl, String accessToken) {
        try {
            WebClient webClient = webClientBuilder.build();
            
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = (Map<String, Object>) webClient
                .get()
                .uri(userInfoUrl)
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
                .retrieve()
                .bodyToMono(Map.class)
                .block();
            
            ResponseEntity<Map<String, Object>> response = ResponseEntity.ok(responseBody);

            responseValidator.validateUserInfoResponse(provider, response);
            
            log.debug("{} 사용자 정보 획득 성공", provider);
            return response.getBody();
            
        } catch (Exception e) {
            log.error("{} 사용자 정보 획득 실패: {}", provider, e.getMessage());
            throw new OAuthUserInfoException(provider, "사용자 정보 획득 중 오류 발생", e);
        }
    }
}