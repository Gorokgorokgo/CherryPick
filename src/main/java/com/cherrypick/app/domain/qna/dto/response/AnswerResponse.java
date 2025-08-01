package com.cherrypick.app.domain.qna.dto.response;

import com.cherrypick.app.domain.qna.entity.Answer;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AnswerResponse {

    private final Long id;
    private final String content;
    private final QnaUserResponse answerer;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Boolean canModify; // 현재 사용자가 수정 가능한지

    /**
     * Answer 엔티티로부터 AnswerResponse 생성
     */
    public static AnswerResponse from(Answer answer, Long currentUserId) {
        boolean canModify = false;
        
        // 현재 사용자가 답변자(판매자)이고, 수정 가능한 상태인지 확인
        if (currentUserId != null && answer.isAnswererMatches(answer.getAnswerer()) && 
            currentUserId.equals(answer.getAnswerer().getId())) {
            canModify = answer.canModifyBySeller();
        }

        return new AnswerResponse(
            answer.getId(),
            answer.getContent(),
            QnaUserResponse.fromSeller(answer.getAnswerer()),
            answer.getCreatedAt(),
            answer.getUpdatedAt(),
            canModify
        );
    }
}