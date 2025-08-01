package com.cherrypick.app.domain.qna.entity;

import com.cherrypick.app.domain.common.entity.BaseEntity;
import com.cherrypick.app.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Answer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false, unique = true)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answerer_id", nullable = false)
    private User answerer;

    @NotBlank(message = "답변 내용은 필수입니다.")
    @Size(max = 1000, message = "답변은 1000자 이내로 작성해주세요.")
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // === 정적 팩토리 메서드 ===
    
    /**
     * 새로운 답변 생성
     */
    public static Answer createAnswer(Question question, User answerer, String content) {
        // 판매자가 아닌 경우 예외 발생
        if (!question.isSellerMatches(answerer)) {
            throw new IllegalArgumentException("답변은 해당 경매의 판매자만 작성할 수 있습니다.");
        }
        
        Answer answer = new Answer(
            null,
            question,
            answerer,
            content
        );
        
        // 질문에 답변 완료 표시
        question.markAsAnswered();
        
        return answer;
    }

    // === 비즈니스 메서드 ===
    
    /**
     * 답변 내용 수정
     */
    public void updateContent(String newContent) {
        // 경매 종료 30분 전이거나 경매가 종료된 경우 수정 불가
        if (this.question.isNearAuctionEnd() || this.question.getAuction().isEnded()) {
            throw new IllegalStateException("경매 종료 30분 전부터는 답변을 수정할 수 없습니다.");
        }
        this.content = newContent;
    }
    
    /**
     * 답변자가 맞는지 확인
     */
    public boolean isAnswererMatches(User user) {
        return this.answerer.getId().equals(user.getId());
    }
    
    /**
     * 답변 수정 가능 여부 확인 (판매자 기준)
     */
    public boolean canModifyBySeller() {
        return !this.question.isNearAuctionEnd() && !this.question.getAuction().isEnded();
    }
}