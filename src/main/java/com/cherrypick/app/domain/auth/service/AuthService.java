package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.repository.AuthRepository;
import com.cherrypick.app.domain.auth.dto.request.SignupRequest;
import com.cherrypick.app.domain.auth.dto.request.LoginRequest;
import com.cherrypick.app.domain.auth.dto.request.VerifyCodeRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthRepository authRepository, JwtConfig jwtConfig, 
                      RedisTemplate<String, String> redisTemplate, PasswordEncoder passwordEncoder) {
        this.authRepository = authRepository;
        this.jwtConfig = jwtConfig;
        this.redisTemplate = redisTemplate;
        this.passwordEncoder = passwordEncoder;
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

    public AuthResponse verifyCode(VerifyCodeRequest request) {
        if (verifyCode(request.getPhoneNumber(), request.getVerificationCode())) {
            // 인증 성공 시 인증 완료 플래그 저장 (10분 유효)
            String verifiedKey = "verified:" + request.getPhoneNumber();
            redisTemplate.opsForValue().set(verifiedKey, "true", 10, TimeUnit.MINUTES);
            
            // 인증 코드는 삭제
            redisTemplate.delete("verification:" + request.getPhoneNumber());
            
            return new AuthResponse("인증이 완료되었습니다.");
        } else {
            return new AuthResponse("인증 코드가 올바르지 않거나 만료되었습니다.");
        }
    }

    public AuthResponse signup(SignupRequest request) {
        // 인증 완료 여부 확인
        String verifiedKey = "verified:" + request.getPhoneNumber();
        String verified = redisTemplate.opsForValue().get(verifiedKey);
        if (verified == null || !verified.equals("true")) {
            return new AuthResponse("전화번호 인증이 필요합니다.");
        }

        // 중복 확인
        if (authRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent()) {
            return new AuthResponse("이미 가입된 전화번호입니다.");
        }
        if (authRepository.findByEmail(request.getEmail()).isPresent()) {
            return new AuthResponse("이미 가입된 이메일입니다.");
        }
        if (authRepository.findByNickname(request.getNickname()).isPresent()) {
            return new AuthResponse("이미 사용 중인 닉네임입니다.");
        }

        // 사용자 생성 (비밀번호 암호화 + 추가 프로필 정보)
        User.UserBuilder userBuilder = User.builder()
                .phoneNumber(request.getPhoneNumber())
                .nickname(request.getNickname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .pointBalance(0L)
                .buyerLevel(1)
                .buyerExp(0)
                .sellerLevel(1)
                .sellerExp(0);

        // 선택 프로필 정보 설정
        if (request.getRealName() != null && !request.getRealName().trim().isEmpty()) {
            userBuilder.realName(request.getRealName().trim());
        }
        if (request.getBirthDate() != null) {
            userBuilder.birthDate(request.getBirthDate());
        }
        if (request.getGender() != null) {
            userBuilder.gender(request.getGender());
        }
        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            userBuilder.address(request.getAddress().trim());
        }
        if (request.getZipCode() != null && !request.getZipCode().trim().isEmpty()) {
            userBuilder.zipCode(request.getZipCode().trim());
        }
        if (request.getBio() != null && !request.getBio().trim().isEmpty()) {
            userBuilder.bio(request.getBio().trim());
        }

        // 프로필 공개 설정 (기본값 적용)
        userBuilder.isProfilePublic(request.getIsProfilePublic() != null ? request.getIsProfilePublic() : true);
        userBuilder.isRealNamePublic(request.getIsRealNamePublic() != null ? request.getIsRealNamePublic() : false);
        userBuilder.isBirthDatePublic(request.getIsBirthDatePublic() != null ? request.getIsBirthDatePublic() : false);

        User user = userBuilder.build();

        User savedUser = authRepository.save(user);

        // 인증 완료 플래그 삭제
        redisTemplate.delete("verified:" + request.getPhoneNumber());

        // JWT 토큰 생성
        String token = jwtConfig.generateToken(savedUser.getEmail(), savedUser.getId());

        return new AuthResponse(token, savedUser.getId(), savedUser.getEmail(), savedUser.getNickname(), "회원가입 성공");
    }

    public AuthResponse login(LoginRequest request) {
        // 사용자 조회
        Optional<User> userOpt = authRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            return new AuthResponse("가입되지 않은 이메일입니다.");
        }

        User user = userOpt.get();

        // 비밀번호 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return new AuthResponse("비밀번호가 올바르지 않습니다.");
        }

        // JWT 토큰 생성
        String token = jwtConfig.generateToken(user.getEmail(), user.getId());

        return new AuthResponse(token, user.getId(), user.getEmail(), user.getNickname());
    }

    private boolean verifyCode(String phoneNumber, String code) {
        String key = "verification:" + phoneNumber;
        String storedCode = redisTemplate.opsForValue().get(key);
        return storedCode != null && storedCode.equals(code);
    }
}