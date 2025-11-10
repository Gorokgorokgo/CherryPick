package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileRequest;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileImageRequest;
import com.cherrypick.app.domain.user.dto.response.UserProfileResponse;
import com.cherrypick.app.domain.common.service.ImageUploadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ImageUploadService imageUploadService;

    public UserService(UserRepository userRepository, ImageUploadService imageUploadService) {
        this.userRepository = userRepository;
        this.imageUploadService = imageUploadService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        return UserProfileResponse.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .nickname(user.getNickname())
                .pointBalance(user.getPointBalance())
                .buyerLevel(user.getBuyerLevel())
                .buyerExp(user.getBuyerExp())
                .sellerLevel(user.getSellerLevel())
                .sellerExp(user.getSellerExp())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress())
                .bio(user.getBio())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 닉네임 중복 확인 (현재 사용자 제외)
        Optional<User> existingUser = userRepository.findByNickname(request.getNickname());
        if (existingUser.isPresent() && !existingUser.get().getId().equals(userId)) {
            throw new RuntimeException("이미 사용 중인 닉네임입니다.");
        }

        // 닉네임 업데이트
        user.setNickname(request.getNickname());

        // 프로필 이미지 업데이트 (기존 이미지가 있고 새 이미지가 다르면 기존 이미지 삭제)
        if (request.getProfileImageUrl() != null) {
            if (user.getProfileImageUrl() != null &&
                !user.getProfileImageUrl().equals(request.getProfileImageUrl())) {
                try {
                    imageUploadService.deleteImage(user.getProfileImageUrl());
                } catch (Exception e) {
                    // 이미지 삭제 실패는 로그만 남기고 진행
                    System.err.println("기존 프로필 이미지 삭제 실패: " + e.getMessage());
                }
            }
            user.setProfileImageUrl(request.getProfileImageUrl());
        }

        // 거래 지역 업데이트
        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        // 자기소개 업데이트
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        return getUserProfile(updatedUser.getId());
    }

    public UserProfileResponse updateProfileImage(Long userId, UpdateProfileImageRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        // 기존 프로필 이미지 삭제
        if (user.getProfileImageUrl() != null) {
            try {
                imageUploadService.deleteImage(user.getProfileImageUrl());
            } catch (Exception e) {
                System.err.println("기존 프로필 이미지 삭제 실패: " + e.getMessage());
            }
        }

        user.setProfileImageUrl(request.getProfileImageUrl());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        return getUserProfile(userId);
    }

    public UserProfileResponse deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

        if (user.getProfileImageUrl() != null) {
            try {
                imageUploadService.deleteImage(user.getProfileImageUrl());
            } catch (Exception e) {
                System.err.println("프로필 이미지 삭제 실패: " + e.getMessage());
            }
            
            user.setProfileImageUrl(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return getUserProfile(userId);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Long getUserIdByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
        return user.getId();
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}