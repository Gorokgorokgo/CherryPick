package com.cherrypick.app.domain.qna.dto.response;

import com.cherrypick.app.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QnaUserResponse {

    private final Long id;
    private final String nickname;
    private final String maskedNickname;
    private final String profileImageUrl;
    private final Integer level;
    private final Boolean isSeller;

    /**
     * 질문자 정보 생성 (닉네임 마스킹 적용)
     */
    public static QnaUserResponse fromQuestioner(User user) {
        return new QnaUserResponse(
            user.getId(),
            user.getNickname(),
            maskNickname(user.getNickname()),
            user.getProfileImageUrl(),
            user.getLevel(),
            false
        );
    }

    /**
     * 판매자 정보 생성 (판매자 태그 적용)
     */
    public static QnaUserResponse fromSeller(User user) {
        return new QnaUserResponse(
            user.getId(),
            user.getNickname(),
            user.getNickname(), // 판매자는 마스킹 없이
            user.getProfileImageUrl(),
            user.getLevel(),
            true
        );
    }

    /**
     * 닉네임 마스킹 처리
     * - 한글: 첫 글자만 보임
     * - 영문/숫자: 첫 두 글자만 보임
     */
    private static String maskNickname(String nickname) {
        if (nickname == null || nickname.isEmpty()) {
            return nickname;
        }

        // 한글 확인
        char firstChar = nickname.charAt(0);
        boolean isKorean = firstChar >= 0xAC00 && firstChar <= 0xD7A3;

        if (isKorean) {
            // 한글: 첫 글자만 보임
            return nickname.charAt(0) + "*".repeat(nickname.length() - 1);
        } else {
            // 영문/숫자: 첫 두 글자만 보임 (길이가 2 이하면 그대로)
            if (nickname.length() <= 2) {
                return nickname;
            }
            return nickname.substring(0, 2) + "*".repeat(nickname.length() - 2);
        }
    }
}