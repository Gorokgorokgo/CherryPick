package com.cherrypick.app.domain.auth.controller;

import com.cherrypick.app.domain.auth.service.AuthService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
@DisplayName("AuthController - 회원 탈퇴 통합 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser = User.builder()
                .phoneNumber("01012345678")
                .nickname("테스트유저")
                .email("test@example.com")
                .password("encodedPassword")
                .pointBalance(0L)
                .buyerLevel(1)
                .buyerExp(0)
                .sellerLevel(1)
                .sellerExp(0)
                .enabled(true)
                .build();

        testUser = userRepository.save(testUser);
    }

    @Test
    @DisplayName("DELETE /api/auth/delete-account - 회원 탈퇴 성공")
    void deleteAccount_Success() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("계정이 성공적으로 삭제되었습니다"));

        // 데이터베이스에서 확인
        User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();
        assertThat(deletedUser.getDeletedAt()).isNotNull();
        assertThat(deletedUser.getEnabled()).isFalse();
    }

    @Test
    @DisplayName("DELETE /api/auth/delete-account - 존재하지 않는 사용자")
    void deleteAccount_UserNotFound() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", 99999L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("DELETE /api/auth/delete-account - 이미 탈퇴한 사용자")
    void deleteAccount_AlreadyDeleted() throws Exception {
        // given - 먼저 탈퇴 처리
        authService.deleteAccount(testUser.getId());

        // when & then
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DisplayName("회원 탈퇴 후 데이터 보존 확인")
    void deleteAccount_DataPreservation() throws Exception {
        // given
        String originalNickname = testUser.getNickname();
        String originalEmail = testUser.getEmail();

        // when
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // then - 데이터가 보존되는지 확인
        User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deletedUser.getNickname()).isEqualTo(originalNickname);
        assertThat(deletedUser.getEmail()).isEqualTo(originalEmail);
        assertThat(deletedUser.getPhoneNumber()).isNotNull();
        assertThat(deletedUser.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("탈퇴 후 동일 이메일로 재가입 가능 확인")
    void deleteAccount_CanReregisterWithSameEmail() throws Exception {
        // given
        String originalEmail = testUser.getEmail();

        // when - 탈퇴
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // then - 탈퇴한 사용자 제외하고 이메일 중복 체크 시 사용 가능해야 함
        User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();

        // 탈퇴하지 않은 사용자 중에서는 동일 이메일이 없어야 함
        boolean emailAvailable = userRepository.findByEmailAndNotDeleted(originalEmail).isEmpty();
        assertThat(emailAvailable).isTrue();
    }

    @Test
    @DisplayName("탈퇴 후 동일 닉네임으로 재가입 가능 확인")
    void deleteAccount_CanReregisterWithSameNickname() throws Exception {
        // given
        String originalNickname = testUser.getNickname();

        // when - 탈퇴
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // then - 탈퇴하지 않은 사용자 중에서는 동일 닉네임이 없어야 함
        boolean nicknameAvailable = userRepository.findByNicknameAndNotDeleted(originalNickname).isEmpty();
        assertThat(nicknameAvailable).isTrue();
    }

    @Test
    @DisplayName("탈퇴 후 복구 가능 확인")
    void deleteAccount_CanRestore() throws Exception {
        // given - 탈퇴
        mockMvc.perform(delete("/api/auth/delete-account")
                        .header("User-Id", testUser.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        User deletedUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(deletedUser.isDeleted()).isTrue();

        // when - 복구
        authService.restoreAccount(testUser.getId());

        // then
        User restoredUser = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(restoredUser.isDeleted()).isFalse();
        assertThat(restoredUser.getDeletedAt()).isNull();
        assertThat(restoredUser.getEnabled()).isTrue();
    }
}
