package com.cherrypick.app.domain.auth.service;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.auth.repository.AuthRepository;
import com.cherrypick.app.domain.auth.dto.request.SignupRequest;
import com.cherrypick.app.domain.auth.dto.request.LoginRequest;
import com.cherrypick.app.domain.auth.dto.request.VerifyCodeRequest;
import com.cherrypick.app.domain.auth.dto.request.PhoneLoginRequest;
import com.cherrypick.app.domain.auth.dto.response.AuthResponse;
import com.cherrypick.app.domain.user.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
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

        // 사용자 생성 (비밀번호 암호화 + 번개장터 스타일 프로필)
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

        // 선택 프로필 정보 설정 (번개장터 스타일 - 거래 지역, 자기소개만)
        if (request.getAddress() != null && !request.getAddress().trim().isEmpty()) {
            userBuilder.address(request.getAddress().trim());
        }
        if (request.getBio() != null && !request.getBio().trim().isEmpty()) {
            userBuilder.bio(request.getBio().trim());
        }

        User user = userBuilder.build();

        User savedUser = authRepository.save(user);

        // 인증 완료 플래그 삭제
        redisTemplate.delete("verified:" + request.getPhoneNumber());

        // JWT 토큰 생성
        String token = jwtConfig.generateToken(savedUser.getEmail(), savedUser.getId());

        log.info("회원가입 완료 - 사용자: {} ({})", savedUser.getNickname(), savedUser.getEmail());

        return new AuthResponse(
            token,
            savedUser.getId(),
            savedUser.getEmail(),
            savedUser.getNickname(),
            savedUser.getProfileImageUrl(),
            savedUser.getAddress(),
            savedUser.getBio(),
            "회원가입 성공"
        );
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

        log.info("로그인 성공 - 사용자: {} ({})", user.getNickname(), user.getEmail());

        return new AuthResponse(
            token,
            user.getId(),
            user.getEmail(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getAddress(),
            user.getBio(),
            "로그인 성공"
        );
    }

    public AuthResponse phoneLogin(PhoneLoginRequest request) {
        // 등록된 사용자 확인
        Optional<User> userOpt = authRepository.findByPhoneNumber(request.getPhoneNumber());
        if (userOpt.isEmpty()) {
            return new AuthResponse("가입되지 않은 전화번호입니다.");
        }

        User user = userOpt.get();

        // 인증번호 확인
        if (!verifyCode(request.getPhoneNumber(), request.getVerificationCode())) {
            return new AuthResponse("인증번호가 올바르지 않거나 만료되었습니다.");
        }

        // 인증 성공 시 인증번호 삭제
        redisTemplate.delete("verification:" + request.getPhoneNumber());

        // JWT 토큰 생성
        String token = jwtConfig.generateToken(user.getEmail(), user.getId());

        log.info("전화번호 로그인 성공 - 사용자: {} ({})", user.getNickname(), user.getPhoneNumber());

        return new AuthResponse(
            token,
            user.getId(),
            user.getPhoneNumber(),
            user.getNickname(),
            user.getProfileImageUrl(),
            user.getAddress(),
            user.getBio(),
            "로그인 성공"
        );
    }

    private boolean verifyCode(String phoneNumber, String code) {
        String key = "verification:" + phoneNumber;
        String storedCode = redisTemplate.opsForValue().get(key);
        return storedCode != null && storedCode.equals(code);
    }

    /**
     * 닉네임 중복 검사
     */
    public AuthResponse checkNickname(String nickname) {
        try {
            // 닉네임 형식 검증
            if (nickname == null || nickname.trim().isEmpty()) {
                return new AuthResponse("닉네임을 입력해주세요.");
            }

            String trimmedNickname = nickname.trim();

            // 닉네임 길이 및 패턴 검증
            if (trimmedNickname.length() < 2 || trimmedNickname.length() > 12) {
                return new AuthResponse("닉네임은 2~12자 사이여야 합니다.");
            }

            if (!trimmedNickname.matches("^[가-힣a-zA-Z0-9_-]+$")) {
                return new AuthResponse("닉네임은 한글, 영문, 숫자, _, - 조합만 가능합니다.");
            }

            // 중복 확인
            if (authRepository.findByNickname(trimmedNickname).isPresent()) {
                return new AuthResponse("이미 사용 중인 닉네임입니다.");
            }

            return new AuthResponse("사용 가능한 닉네임입니다.");
        } catch (Exception e) {
            System.err.println("닉네임 중복 검사 오류: " + e.getMessage());
            return new AuthResponse("닉네임 검사 중 오류가 발생했습니다.");
        }
    }

    /**
     * 이메일 중복 검사
     */
    public AuthResponse checkEmail(String email) {
        try {
            // 이메일 형식 검증
            if (email == null || email.trim().isEmpty()) {
                return new AuthResponse("이메일을 입력해주세요.");
            }

            String trimmedEmail = email.trim().toLowerCase();

            // 기본 이메일 형식 검증
            if (!trimmedEmail.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
                return new AuthResponse("올바른 이메일 형식이 아닙니다.");
            }

            // 중복 확인
            if (authRepository.findByEmail(trimmedEmail).isPresent()) {
                return new AuthResponse("이미 가입된 이메일입니다.");
            }

            return new AuthResponse("사용 가능한 이메일입니다.");
        } catch (Exception e) {
            System.err.println("이메일 중복 검사 오류: " + e.getMessage());
            return new AuthResponse("이메일 검사 중 오류가 발생했습니다.");
        }
    }
}