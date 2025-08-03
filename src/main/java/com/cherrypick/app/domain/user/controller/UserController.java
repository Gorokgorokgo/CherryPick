package com.cherrypick.app.domain.user.controller;

import com.cherrypick.app.common.exception.AuthenticationFailedException;
import com.cherrypick.app.config.JwtConfig;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileRequest;
import com.cherrypick.app.domain.user.dto.request.UpdateProfileImageRequest;
import com.cherrypick.app.domain.user.dto.response.UserProfileResponse;
import com.cherrypick.app.domain.user.dto.response.UserLevelInfoResponse;
import com.cherrypick.app.domain.user.dto.response.LevelProgressResponse;
import com.cherrypick.app.domain.user.service.UserService;
import com.cherrypick.app.domain.user.service.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@Tag(name = "2단계 - 사용자 프로필", description = "사용자 정보 관리 | 닉네임, 개인정보 설정")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final ExperienceService experienceService;
    private final JwtConfig jwtConfig;

    public UserController(UserService userService, ExperienceService experienceService, JwtConfig jwtConfig) {
        this.userService = userService;
        this.experienceService = experienceService;
        this.jwtConfig = jwtConfig;
    }

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회", description = "현재 로그인된 사용자의 프로필 정보를 조회합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> getProfile(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "프로필 수정", 
               description = "사용자의 프로필 정보를 수정합니다. 닉네임, 프로필 이미지, 실명, 생년월일, 성별, 주소, 자기소개, 공개 설정 등을 수정할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 중복 닉네임"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest updateRequest,
            HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse updatedProfile = userService.updateProfile(userId, updateRequest);
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/profile/image")
    @Operation(summary = "프로필 이미지 수정", 
               description = "사용자의 프로필 이미지를 수정합니다. 기존 이미지는 자동으로 삭제됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 이미지 URL"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> updateProfileImage(
            @Valid @RequestBody UpdateProfileImageRequest imageRequest,
            HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse updatedProfile = userService.updateProfileImage(userId, imageRequest);
        return ResponseEntity.ok(updatedProfile);
    }

    @DeleteMapping("/profile/image")
    @Operation(summary = "프로필 이미지 삭제", 
               description = "사용자의 프로필 이미지를 삭제합니다. S3에서도 완전히 제거됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "프로필 이미지 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<UserProfileResponse> deleteProfileImage(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserProfileResponse updatedProfile = userService.deleteProfileImage(userId);
        return ResponseEntity.ok(updatedProfile);
    }

    private Long extractUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtConfig.extractUserId(token);
        }
        throw new AuthenticationFailedException();
    }
    
    @GetMapping("/level")
    @Operation(summary = "레벨 정보 조회", description = "구매자/판매자 레벨 진행률을 조회합니다 (레벨대별 차등 표시)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "레벨 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    public ResponseEntity<UserLevelInfoResponse> getUserLevelInfo(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        UserLevelInfoResponse levelInfo = experienceService.getUserLevelInfo(userId);
        return ResponseEntity.ok(levelInfo);
    }
    
    @GetMapping("/level/buyer")
    @Operation(summary = "구매자 레벨 진행률 조회", description = "구매자 레벨 진행률을 조회합니다 (심리적 배려)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "구매자 레벨 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<LevelProgressResponse> getBuyerLevelProgress(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        LevelProgressResponse progress = experienceService.getBuyerLevelProgress(userId);
        return ResponseEntity.ok(progress);
    }
    
    @GetMapping("/level/seller")
    @Operation(summary = "판매자 레벨 진행률 조회", description = "판매자 레벨 진행률을 조회합니다 (심리적 배려)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "판매자 레벨 정보 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    public ResponseEntity<LevelProgressResponse> getSellerLevelProgress(HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request);
        LevelProgressResponse progress = experienceService.getSellerLevelProgress(userId);
        return ResponseEntity.ok(progress);
    }
}