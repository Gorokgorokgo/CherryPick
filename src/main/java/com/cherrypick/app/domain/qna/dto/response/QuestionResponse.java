package com.cherrypick.app.domain.qna.dto.response;

import com.cherrypick.app.domain.qna.entity.Question;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class QuestionResponse {

    private final Long id;
    private final String content;
    private final QnaUserResponse questioner;
    private final Boolean isAnswered;
    private final AnswerResponse answer;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final Boolean canModify; // 현재 사용자가 수정 가능한지
    private final Boolean canDelete; // 현재 사용자가 삭제 가능한지

    /**
     * Question 엔티티로부터 QuestionResponse 생성 (답변 없음)
     */
    public static QuestionResponse from(Question question, Long currentUserId) {
        return from(question, null, currentUserId);
    }

    /**
     * Question 엔티티로부터 QuestionResponse 생성 (답변 포함)
     */
    public static QuestionResponse from(Question question, AnswerResponse answer, Long currentUserId) {
        boolean canModify = false;
        boolean canDelete = false;
        
        // 현재 사용자가 질문자이고, 경매 종료 30분 전이 아닌 경우
        if (currentUserId != null && question.isQuestionerMatches(question.getQuestioner()) && 
            currentUserId.equals(question.getQuestioner().getId()) && 
            !question.isNearAuctionEnd()) {
            
            // 답변이 달리지 않은 경우에만 수정/삭제 가능
            canModify = question.canModifyByQuestioner();
            canDelete = question.canModifyByQuestioner();
        }

        return new QuestionResponse(
            question.getId(),
            question.getContent(),
            QnaUserResponse.fromQuestioner(question.getQuestioner()),
            question.getIsAnswered(),
            answer,
            question.getCreatedAt(),
            question.getUpdatedAt(),
            canModify,
            canDelete
        );
    }
}