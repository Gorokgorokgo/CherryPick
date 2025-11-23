package com.cherrypick.app.domain.qna.entity;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id", nullable = false)
    private Auction auction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "questioner_id", nullable = false)
    private User questioner;

    @NotBlank(message = "질문 내용은 필수입니다.")
    @Size(max = 1000, message = "질문은 1000자 이내로 작성해주세요.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    @Column(name = "is_answered", nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private Boolean isAnswered = false;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 새로운 질문 생성
     */
    public static Question createQuestion(Auction auction, User questioner, String content) {
        return new Question(
            null,
            auction,
            questioner,
            content,
            false
        );
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 질문 내용 수정
     */
    public void updateContent(String newContent) {
        if (this.isAnswered) {
            throw new BusinessException(ErrorCode.ANSWERED_QUESTION_CANNOT_MODIFY);
        }
        this.content = newContent;
    }
    
    /**
     * 답변 완료 표시
     */
    public void markAsAnswered() {
        this.isAnswered = true;
    }
    
    /**
     * 답변 미완료 표시
     */
    public void markAsUnanswered() {
        this.isAnswered = false;
    }
    
    /**
     * 질문자가 맞는지 확인
     */
    public boolean isQuestionerMatches(User user) {
        return this.questioner.getId().equals(user.getId());
    }
    
    /**
     * 해당 경매의 판매자가 맞는지 확인
     */
    public boolean isSellerMatches(User user) {
        return this.auction.getSeller().getId().equals(user.getId());
    }
    
    /**
     * 질문 수정/삭제 가능 여부 확인 (질문자 기준)
     */
    public boolean canModifyByQuestioner() {
        return !this.isAnswered;
    }
    
    /**
     * 경매 종료 30분 전인지 확인
     */
    public boolean isNearAuctionEnd() {
        return this.auction.getEndAt().minusMinutes(30).isBefore(java.time.LocalDateTime.now());
    }

    /**
     * 질문 삭제 가능 여부 확인
     */
    public boolean canDelete() {
        return !this.isAnswered && !this.isNearAuctionEnd();
    }
}