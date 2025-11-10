package com.cherrypick.app.domain.user.controller;

import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileRequest;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileImageRequest;
import com.cherrypick.app.domain.user.dto.response.UserProfileResponse;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.service.UserService;
import com.cherrypick.app.domain.user.service.ExperienceService;
import com.cherrypick.app.domain.auction.service.AuctionBookmarkService;
import com.cherrypick.app.domain.auction.repository.AuctionImageRepository;
import com.cherrypick.app.domain.auction.service.AuctionService;
import com.cherrypick.app.domain.bid.service.BidService;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@WebMvcTest(UserController.class)
@DisplayName("UserController 통합 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ExperienceService experienceService;

    @MockBean
    private JwtConfig jwtConfig;

    @MockBean
    private AuctionBookmarkService bookmarkService;

    @MockBean
    private AuctionImageRepository auctionImageRepository;

    @MockBean
    private AuctionService auctionService;

    @MockBean
    private BidService bidService;

    @MockBean
    private BidRepository bidRepository;

    private String validToken;
    private Long userId;
    private UserProfileResponse mockProfileResponse;
    private UpdateProfileRequest updateRequest;

    @BeforeEach
    void setUp() {
        validToken = "valid.jwt.token";
        userId = 1L;

        // Mock UserProfileResponse
        mockProfileResponse = UserProfileResponse.builder()
                .id(userId)
                .phoneNumber("01012345678")
                .nickname("테스트유저")
                .pointBalance(1000L)
                .buyerLevel(1)
                .buyerExp(50)
                .sellerLevel(1)
                .sellerExp(30)
                .profileImageUrl("https://s3.amazonaws.com/profile.jpg")
                .realName("홍길동")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender(User.Gender.MALE)
                .address("서울시 강남구")
                .zipCode("12345")
                .bio("안녕하세요")
                .isProfilePublic(true)
                .isRealNamePublic(false)
                .isBirthDatePublic(false)
                .build();

        // Mock UpdateProfileRequest
        updateRequest = new UpdateProfileRequest();
        updateRequest.setNickname("변경된닉네임");
        updateRequest.setProfileImageUrl("https://s3.amazonaws.com/new-profile.jpg");
        updateRequest.setRealName("김철수");
        updateRequest.setBirthDate(LocalDate.of(1995, 5, 5));
        updateRequest.setGender(User.Gender.FEMALE);
        updateRequest.setAddress("부산시 해운대구");
        updateRequest.setZipCode("54321");
        updateRequest.setBio("반갑습니다");
        updateRequest.setIsProfilePublic(false);
        updateRequest.setIsRealNamePublic(true);
        updateRequest.setIsBirthDatePublic(true);

        // Mock JWT 토큰 추출
        given(jwtConfig.extractUserId(validToken)).willReturn(userId);
    }

    @Test
    @DisplayName("GET /api/users/profile - 프로필 조회 성공")
    void getProfile_Success() throws Exception {
        // given
        given(userService.getUserProfile(userId)).willReturn(mockProfileResponse);

        // when & then
        mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://s3.amazonaws.com/profile.jpg"))
                .andExpect(jsonPath("$.realName").value("홍길동"))
                .andExpect(jsonPath("$.address").value("서울시 강남구"))
                .andExpect(jsonPath("$.bio").value("안녕하세요"))
                .andExpect(jsonPath("$.isProfilePublic").value(true));
    }

    @Test
    @DisplayName("GET /api/users/profile - 인증 실패 (401)")
    void getProfile_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(get("/api/users/profile"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/profile - 프로필 수정 성공")
    void updateProfile_Success() throws Exception {
        // given
        UserProfileResponse updatedResponse = UserProfileResponse.builder()
                .id(userId)
                .phoneNumber("01012345678")
                .nickname("변경된닉네임")
                .pointBalance(1000L)
                .buyerLevel(1)
                .buyerExp(50)
                .sellerLevel(1)
                .sellerExp(30)
                .profileImageUrl("https://s3.amazonaws.com/new-profile.jpg")
                .realName("김철수")
                .birthDate(LocalDate.of(1995, 5, 5))
                .gender(User.Gender.FEMALE)
                .address("부산시 해운대구")
                .zipCode("54321")
                .bio("반갑습니다")
                .isProfilePublic(false)
                .isRealNamePublic(true)
                .isBirthDatePublic(true)
                .build();

        given(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class)))
                .willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("변경된닉네임"))
                .andExpect(jsonPath("$.profileImageUrl").value("https://s3.amazonaws.com/new-profile.jpg"))
                .andExpect(jsonPath("$.realName").value("김철수"))
                .andExpect(jsonPath("$.address").value("부산시 해운대구"))
                .andExpect(jsonPath("$.bio").value("반갑습니다"))
                .andExpect(jsonPath("$.isProfilePublic").value(false))
                .andExpect(jsonPath("$.isRealNamePublic").value(true));
    }

    @Test
    @DisplayName("PUT /api/users/profile - 유효성 검증 실패 (닉네임 길이)")
    void updateProfile_ValidationFail_NicknameTooShort() throws Exception {
        // given
        updateRequest.setNickname("a"); // 1자 (최소 2자)

        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/profile - 유효성 검증 실패 (닉네임 너무 긺)")
    void updateProfile_ValidationFail_NicknameTooLong() throws Exception {
        // given
        updateRequest.setNickname("12345678901"); // 11자 (최대 10자)

        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/profile - 유효성 검증 실패 (자기소개 너무 긺)")
    void updateProfile_ValidationFail_BioTooLong() throws Exception {
        // given
        updateRequest.setBio("a".repeat(501)); // 501자 (최대 500자)

        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /api/users/profile - 인증 실패 (401)")
    void updateProfile_Unauthorized() throws Exception {
        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /api/users/profile - 닉네임 중복 (서비스 레벨 예외)")
    void updateProfile_DuplicateNickname() throws Exception {
        // given
        given(userService.updateProfile(eq(userId), any(UpdateProfileRequest.class)))
                .willThrow(new RuntimeException("이미 사용 중인 닉네임입니다."));

        // when & then
        mockMvc.perform(put("/api/users/profile")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("PUT /api/users/profile/image - 프로필 이미지 수정 성공")
    void updateProfileImage_Success() throws Exception {
        // given
        UpdateProfileImageRequest imageRequest = new UpdateProfileImageRequest();
        imageRequest.setProfileImageUrl("https://s3.amazonaws.com/updated-image.jpg");

        UserProfileResponse updatedResponse = UserProfileResponse.builder()
                .id(userId)
                .phoneNumber("01012345678")
                .nickname("테스트유저")
                .pointBalance(1000L)
                .buyerLevel(1)
                .buyerExp(50)
                .sellerLevel(1)
                .sellerExp(30)
                .profileImageUrl("https://s3.amazonaws.com/updated-image.jpg")
                .build();

        given(userService.updateProfileImage(eq(userId), any(UpdateProfileImageRequest.class)))
                .willReturn(updatedResponse);

        // when & then
        mockMvc.perform(put("/api/users/profile/image")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(imageRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").value("https://s3.amazonaws.com/updated-image.jpg"));
    }

    @Test
    @DisplayName("DELETE /api/users/profile/image - 프로필 이미지 삭제 성공")
    void deleteProfileImage_Success() throws Exception {
        // given
        UserProfileResponse updatedResponse = UserProfileResponse.builder()
                .id(userId)
                .phoneNumber("01012345678")
                .nickname("테스트유저")
                .pointBalance(1000L)
                .buyerLevel(1)
                .buyerExp(50)
                .sellerLevel(1)
                .sellerExp(30)
                .profileImageUrl(null) // 이미지 삭제됨
                .build();

        given(userService.deleteProfileImage(userId)).willReturn(updatedResponse);

        // when & then
        mockMvc.perform(delete("/api/users/profile/image")
                        .header("Authorization", "Bearer " + validToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/users/me - 현재 사용자 정보 조회 성공")
    void getMe_Success() throws Exception {
        // given
        given(userService.getUserProfile(userId)).willReturn(mockProfileResponse);

        // when & then
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + validToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.nickname").value("테스트유저"));
    }
}
