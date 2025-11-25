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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트 - 번개장터 스타일")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageUploadService imageUploadService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UpdateProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        // 테스트용 사용자 생성 (번개장터 스타일 - 최소 필드)
        testUser = User.builder()
                .id(1L)
                .phoneNumber("01012345678")
                .nickname("테스트유저")
                .email("test@example.com")
                .password("encodedPassword")
                .pointBalance(1000L)
                .buyerLevel(1)
                .buyerExp(50)
                .sellerLevel(1)
                .sellerExp(30)
                .profileImageUrl("https://s3.amazonaws.com/old-image.jpg")
                .address("서울시 강남구")
                .bio("안녕하세요")
                .enabled(true)
                .emailVerified(true)
                .role(User.Role.USER)
                .build();

        // 테스트용 프로필 수정 요청 생성 (번개장터 스타일 - 4개 필드만)
        updateRequest = new UpdateProfileRequest();
        updateRequest.setNickname("변경된닉네임");
        updateRequest.setProfileImageUrl("https://s3.amazonaws.com/new-image.jpg");
        updateRequest.setAddress("부산시 해운대구");
        updateRequest.setBio("반갑습니다");
    }

    @Test
    @DisplayName("프로필 조회 성공")
    void getUserProfile_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        UserProfileResponse response = userService.getUserProfile(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getNickname()).isEqualTo("테스트유저");
        assertThat(response.getProfileImageUrl()).isEqualTo("https://s3.amazonaws.com/old-image.jpg");
        assertThat(response.getAddress()).isEqualTo("서울시 강남구");
        assertThat(response.getBio()).isEqualTo("안녕하세요");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("프로필 조회 실패 - 존재하지 않는 사용자")
    void getUserProfile_UserNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.getUserProfile(999L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    @DisplayName("프로필 수정 성공 - 모든 필드 업데이트")
    void updateProfile_Success_AllFields() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.findByNickname("변경된닉네임")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(testUser);
        willDoNothing().given(imageUploadService).deleteImage(anyString());

        // when
        UserProfileResponse response = userService.updateProfile(1L, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(testUser.getNickname()).isEqualTo("변경된닉네임");
        assertThat(testUser.getProfileImageUrl()).isEqualTo("https://s3.amazonaws.com/new-image.jpg");
        assertThat(testUser.getAddress()).isEqualTo("부산시 해운대구");
        assertThat(testUser.getBio()).isEqualTo("반갑습니다");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByNickname("변경된닉네임");
        verify(userRepository, times(1)).save(testUser);
        verify(imageUploadService, times(1)).deleteImage("https://s3.amazonaws.com/old-image.jpg");
    }

    @Test
    @DisplayName("프로필 수정 성공 - 닉네임만 변경")
    void updateProfile_Success_OnlyNickname() {
        // given
        UpdateProfileRequest nicknameOnlyRequest = new UpdateProfileRequest();
        nicknameOnlyRequest.setNickname("새로운닉네임");

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.findByNickname("새로운닉네임")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(testUser);

        // when
        UserProfileResponse response = userService.updateProfile(1L, nicknameOnlyRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(testUser.getNickname()).isEqualTo("새로운닉네임");
        // 다른 필드는 변경되지 않음
        assertThat(testUser.getProfileImageUrl()).isEqualTo("https://s3.amazonaws.com/old-image.jpg");
        assertThat(testUser.getAddress()).isEqualTo("서울시 강남구");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
        verify(imageUploadService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("프로필 수정 실패 - 닉네임 중복")
    void updateProfile_Fail_DuplicateNickname() {
        // given
        User anotherUser = User.builder().id(2L).nickname("변경된닉네임").build();

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.findByNickname("변경된닉네임")).willReturn(Optional.of(anotherUser));

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(1L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이미 사용 중인 닉네임입니다.");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).findByNickname("변경된닉네임");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("프로필 수정 실패 - 존재하지 않는 사용자")
    void updateProfile_Fail_UserNotFound() {
        // given
        given(userRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateProfile(999L, updateRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");

        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("프로필 이미지 업데이트 성공")
    void updateProfileImage_Success() {
        // given
        UpdateProfileImageRequest imageRequest = new UpdateProfileImageRequest();
        imageRequest.setProfileImageUrl("https://s3.amazonaws.com/updated-image.jpg");

        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willReturn(testUser);
        willDoNothing().given(imageUploadService).deleteImage(anyString());

        // when
        UserProfileResponse response = userService.updateProfileImage(1L, imageRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(testUser.getProfileImageUrl()).isEqualTo("https://s3.amazonaws.com/updated-image.jpg");

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
        verify(imageUploadService, times(1)).deleteImage("https://s3.amazonaws.com/old-image.jpg");
    }

    @Test
    @DisplayName("프로필 이미지 삭제 성공")
    void deleteProfileImage_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.save(any(User.class))).willReturn(testUser);
        willDoNothing().given(imageUploadService).deleteImage(anyString());

        // when
        UserProfileResponse response = userService.deleteProfileImage(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(testUser.getProfileImageUrl()).isNull();

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
        verify(imageUploadService, times(1)).deleteImage("https://s3.amazonaws.com/old-image.jpg");
    }

    @Test
    @DisplayName("프로필 이미지 삭제 - 기존 이미지 없음")
    void deleteProfileImage_NoExistingImage() {
        // given
        testUser.setProfileImageUrl(null);
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        UserProfileResponse response = userService.deleteProfileImage(1L);

        // then
        assertThat(response).isNotNull();
        assertThat(testUser.getProfileImageUrl()).isNull();

        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
        verify(imageUploadService, never()).deleteImage(anyString());
    }

    @Test
    @DisplayName("이미지 삭제 실패 시에도 프로필 수정은 성공")
    void updateProfile_Success_EvenWhenImageDeleteFails() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
        given(userRepository.findByNickname("변경된닉네임")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(testUser);
        doThrow(new RuntimeException("S3 삭제 실패"))
                .when(imageUploadService).deleteImage(anyString());

        // when
        UserProfileResponse response = userService.updateProfile(1L, updateRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(testUser.getNickname()).isEqualTo("변경된닉네임");
        assertThat(testUser.getProfileImageUrl()).isEqualTo("https://s3.amazonaws.com/new-image.jpg");

        verify(userRepository, times(1)).save(testUser);
        verify(imageUploadService, times(1)).deleteImage("https://s3.amazonaws.com/old-image.jpg");
    }

    @Test
    @DisplayName("사용자 ID로 사용자 조회 성공")
    void getUserById_Success() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

        // when
        User user = userService.getUserById(1L);

        // then
        assertThat(user).isNotNull();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getNickname()).isEqualTo("테스트유저");
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("이메일로 사용자 ID 조회 성공")
    void getUserIdByEmail_Success() {
        // given
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

        // when
        Long userId = userService.getUserIdByEmail("test@example.com");

        // then
        assertThat(userId).isEqualTo(1L);
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    @DisplayName("이메일로 사용자 조회 성공")
    void getUserByEmail_Success() {
        // given
        given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(testUser));

        // when
        User user = userService.getUserByEmail("test@example.com");

        // then
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo("test@example.com");
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }
}
