package com.cherrypick.app.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class StateService {

    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String STATE_PREFIX = "oauth:state:";
    private static final int STATE_LENGTH = 32;
    private static final long STATE_EXPIRY_MINUTES = 10;
    
    private final SecureRandom secureRandom = new SecureRandom();

    public String generateState() {
        byte[] randomBytes = new byte[STATE_LENGTH];
        secureRandom.nextBytes(randomBytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        
        String key = STATE_PREFIX + state;
        redisTemplate.opsForValue().set(key, "valid", STATE_EXPIRY_MINUTES, TimeUnit.MINUTES);
        
        log.debug("Generated OAuth state: {}", state);
        return state;
    }

    public boolean validateAndRemoveState(String state) {
        if (state == null || state.isEmpty()) {
            log.warn("Empty state parameter received");
            return false;
        }
        
        String key = STATE_PREFIX + state;
        String value = redisTemplate.opsForValue().get(key);
        
        if (value == null) {
            log.warn("Invalid or expired state: {}", state);
            return false;
        }
        
        redisTemplate.delete(key);
        log.debug("Validated and removed OAuth state: {}", state);
        return true;
    }

    public void cleanupExpiredStates() {
        log.info("OAuth state cleanup completed");
    }
}