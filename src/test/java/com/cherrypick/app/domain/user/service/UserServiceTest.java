package com.cherrypick.app.domain.user.service;

import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileRequest;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileImageRequest;
import com.cherrypick.app.domain.user.dto.response.UserProfileResponse;
import com.cherrypick.app.domain.common.service.ImageUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("testUser")
                .email("test@example.com")
                .password("password")
                .pointBalance(0L)
                .buyerLevel(1)
                .buyerExp(0)
                .sellerLevel(1)
                .sellerExp(0)
                .build();
    }

    @Test
    @DisplayName("프로필 조회 - 성공")
    void getUserProfile_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        UserProfileResponse response = userService.getUserProfile(1L);

        // Then
        assertNotNull(response);
        assertEquals(testUser.getId(), response.getId());
        assertEquals(testUser.getNickname(), response.getNickname());
        assertEquals(testUser.getPhoneNumber(), response.getPhoneNumber());
    }

    @Test
    @DisplayName("프로필 수정 - 성공")
    void updateProfile_Success() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("newNickname");
        request.setRealName("홍길동");
        request.setBirthDate(LocalDate.of(1990, 1, 1));
        request.setGender(User.Gender.MALE);
        request.setBio("안녕하세요!");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByNickname("newNickname")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileResponse response = userService.updateProfile(1L, request);

        // Then
        assertNotNull(response);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("프로필 이미지 수정 - 성공")
    void updateProfileImage_Success() {
        // Given
        UpdateProfileImageRequest request = new UpdateProfileImageRequest();
        request.setProfileImageUrl("https://example.com/new-image.jpg");

        testUser.setProfileImageUrl("https://example.com/old-image.jpg");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileResponse response = userService.updateProfileImage(1L, request);

        // Then
        assertNotNull(response);
        verify(imageUploadService).deleteImage("https://example.com/old-image.jpg");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("프로필 이미지 삭제 - 성공")
    void deleteProfileImage_Success() {
        // Given
        testUser.setProfileImageUrl("https://example.com/image.jpg");
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserProfileResponse response = userService.deleteProfileImage(1L);

        // Then
        assertNotNull(response);
        verify(imageUploadService).deleteImage("https://example.com/image.jpg");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("닉네임 중복 - 예외 발생")
    void updateProfile_DuplicateNickname_ThrowsException() {
        // Given
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("duplicateNickname");

        User anotherUser = User.builder()
                .id(2L)
                .nickname("duplicateNickname")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findByNickname("duplicateNickname"))
                .thenReturn(Optional.of(anotherUser));

        // When & Then
        assertThrows(RuntimeException.class, () -> {
            userService.updateProfile(1L, request);
        });
    }
}