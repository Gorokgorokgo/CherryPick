package com.cherrypick.app.domain.auth.repository;

import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AuthRepository {

    private final UserRepository userRepository;

    public AuthRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber);
    }

    public Optional<User> findByNickname(String nickname) {
        return userRepository.findByNickname(nickname);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public boolean existsByPhoneNumber(String phoneNumber) {
        return userRepository.existsByPhoneNumber(phoneNumber);
    }

    public boolean existsByNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public void delete(User user) {
        userRepository.delete(user);
    }

    // Soft Delete 고려 조회 메서드 (탈퇴하지 않은 사용자만)
    public Optional<User> findByPhoneNumberAndNotDeleted(String phoneNumber) {
        return userRepository.findByPhoneNumberAndNotDeleted(phoneNumber);
    }

    public Optional<User> findByNicknameAndNotDeleted(String nickname) {
        return userRepository.findByNicknameAndNotDeleted(nickname);
    }

    public Optional<User> findByEmailAndNotDeleted(String email) {
        return userRepository.findByEmailAndNotDeleted(email);
    }

    public Optional<User> findByIdAndNotDeleted(Long id) {
        return userRepository.findByIdAndNotDeleted(id);
    }

    public boolean existsByPhoneNumberAndNotDeleted(String phoneNumber) {
        return userRepository.existsByPhoneNumberAndNotDeleted(phoneNumber);
    }

    public boolean existsByNicknameAndNotDeleted(String nickname) {
        return userRepository.existsByNicknameAndNotDeleted(nickname);
    }

    public boolean existsByEmailAndNotDeleted(String email) {
        return userRepository.existsByEmailAndNotDeleted(email);
    }
}