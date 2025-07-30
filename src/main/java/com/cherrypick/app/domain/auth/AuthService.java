package com.cherrypick.app.domain.auth;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.user.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtConfig jwtConfig;
    private final RedisTemplate<String, String> redisTemplate;

    public AuthService(AuthRepository authRepository, JwtConfig jwtConfig, 
                      RedisTemplate<String, String> redisTemplate) {
        this.authRepository = authRepository;
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
    }

    public AuthResponse sendVerificationCode(String phoneNumber) {
        // 인증 코드 생성 (6자리 숫자)
        String verificationCode = String.format("%06d", new Random().nextInt(1000000));
        
        // Redis에 인증 코드 저장 (5분 만료)
        String key = "verification:" + phoneNumber;
        redisTemplate.opsForValue().set(key, verificationCode, 5, TimeUnit.MINUTES);
        
        // TODO: 실제 SMS 발송 로직 구현
        // SmsService.sendSms(phoneNumber, "체리픽 인증 코드: " + verificationCode);
        
        return new AuthResponse("인증 코드가 발송되었습니다. (개발용: " + verificationCode + ")");
    }

    public AuthResponse signup(SignupRequest request) {
        // 인증 코드 검증
        if (!verifyCode(request.getPhoneNumber(), request.getVerificationCode())) {
            return new AuthResponse("인증 코드가 올바르지 않습니다.");
        }

        // 이미 가입된 사용자 확인
        if (authRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return new AuthResponse("이미 가입된 전화번호입니다.");
        }

        // 닉네임 중복 확인
        if (authRepository.findByNickname(request.getNickname()).isPresent()) {
            return new AuthResponse("이미 사용 중인 닉네임입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .nickname(request.getNickname())
                .pointBalance(0L)
                .level(1)
                .experience(0L)
                .build();

        User savedUser = authRepository.save(user);

        // 인증 코드 삭제
        redisTemplate.delete("verification:" + request.getPhoneNumber());

        // JWT 토큰 생성
        String token = jwtConfig.generateToken(savedUser.getPhoneNumber(), savedUser.getId());

        return new AuthResponse(token, savedUser.getId(), savedUser.getPhoneNumber(), savedUser.getNickname());
    }

    public AuthResponse login(LoginRequest request) {
        // 인증 코드 검증
        if (!verifyCode(request.getPhoneNumber(), request.getVerificationCode())) {
            return new AuthResponse("인증 코드가 올바르지 않습니다.");
        }

        // 사용자 조회
        Optional<User> userOpt = authRepository.findByPhoneNumber(request.getPhoneNumber());
        if (userOpt.isEmpty()) {
            return new AuthResponse("가입되지 않은 전화번호입니다.");
        }

        User user = userOpt.get();

        // 인증 코드 삭제
        redisTemplate.delete("verification:" + request.getPhoneNumber());

        // JWT 토큰 생성
        String token = jwtConfig.generateToken(user.getPhoneNumber(), user.getId());

        return new AuthResponse(token, user.getId(), user.getPhoneNumber(), user.getNickname());
    }

    private boolean verifyCode(String phoneNumber, String code) {
        String key = "verification:" + phoneNumber;
        String storedCode = redisTemplate.opsForValue().get(key);
        return storedCode != null && storedCode.equals(code);
    }
}