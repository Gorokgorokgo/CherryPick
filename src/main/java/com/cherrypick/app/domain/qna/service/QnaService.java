package com.cherrypick.app.domain.qna.service;

import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.qna.dto.request.CreateAnswerRequest;
import com.cherrypick.app.domain.qna.dto.request.CreateQuestionRequest;
import com.cherrypick.app.domain.qna.dto.request.UpdateAnswerRequest;
import com.cherrypick.app.domain.qna.dto.request.UpdateQuestionRequest;
import com.cherrypick.app.domain.qna.dto.response.AnswerResponse;
import com.cherrypick.app.domain.qna.dto.response.QuestionResponse;
import com.cherrypick.app.domain.qna.entity.Answer;
import com.cherrypick.app.domain.qna.entity.Question;
import com.cherrypick.app.domain.qna.repository.AnswerRepository;
import com.cherrypick.app.domain.qna.repository.QuestionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnaService {

    private final QuestionRepository questionRepository;
    private final AnswerRepository answerRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 질문 등록
     * 
     * 비즈니스 로직:
     * 1. 경매 존재 여부 확인
     * 2. 경매가 진행 중인지 확인
     * 3. 경매 종료 30분 전인지 확인
     * 4. 질문자와 판매자가 동일한지 확인 (자기 경매에는 질문 불가)
     * 5. 질문 생성 및 저장
     * 
     * @param auctionId 경매 ID
     * @param userId 질문자 사용자 ID
     * @param request 질문 등록 요청
     * @return 생성된 질문 정보
     */
    @Transactional
    public QuestionResponse createQuestion(Long auctionId, Long userId, CreateQuestionRequest request) {
        // 경매 및 사용자 조회
        Auction auction = auctionRepository.findById(auctionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        User questioner = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 경매 진행 상태 확인
        if (!auction.isActive()) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_ACTIVE);
        }

        // 경매 종료 30분 전 확인
        if (auction.getEndAt().minusMinutes(30).isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.QUESTION_TIME_LIMIT_EXCEEDED);
        }

        // 자기 경매에는 질문 불가
        if (auction.getSeller().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.SELF_QUESTION_NOT_ALLOWED);
        }

        // 질문 생성 및 저장
        Question question = Question.createQuestion(auction, questioner, request.getContent());
        Question savedQuestion = questionRepository.save(question);

        return QuestionResponse.from(savedQuestion, userId);
    }

    /**
     * 질문 수정
     * 
     * 비즈니스 로직:
     * 1. 질문 존재 여부 확인
     * 2. 질문자 본인 확인
     * 3. 답변이 달렸는지 확인 (답변 달린 후에는 수정 불가)
     * 4. 경매 종료 30분 전인지 확인
     * 5. 질문 내용 수정
     * 
     * @param questionId 질문 ID
     * @param userId 현재 사용자 ID
     * @param request 질문 수정 요청
     * @return 수정된 질문 정보
     */
    @Transactional
    public QuestionResponse updateQuestion(Long questionId, Long userId, UpdateQuestionRequest request) {
        Question question = questionRepository.findByIdWithQuestionerAndAuction(questionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 질문자 본인 확인
        if (!question.getQuestioner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_QUESTION_AUTHOR);
        }

        // 경매 종료 30분 전 확인
        if (question.isNearAuctionEnd()) {
            throw new BusinessException(ErrorCode.QNA_MODIFY_RESTRICTED_NEAR_END);
        }

        // 질문 수정 (답변 달림 여부는 Question 엔티티에서 확인)
        question.updateContent(request.getContent());

        return QuestionResponse.from(question, userId);
    }

    /**
     * 질문 삭제
     * 
     * 비즈니스 로직:
     * 1. 질문 존재 여부 확인
     * 2. 질문자 본인 확인
     * 3. 답변이 달렸는지 확인 (답변 달린 후에는 삭제 불가)
     * 4. 경매 종료 30분 전인지 확인
     * 5. 답변이 있다면 함께 삭제
     * 6. 질문 삭제
     * 
     * @param questionId 질문 ID
     * @param userId 현재 사용자 ID
     */
    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        Question question = questionRepository.findByIdWithQuestionerAndAuction(questionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        // 질문자 본인 확인
        if (!question.getQuestioner().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_QUESTION_AUTHOR);
        }

        // 답변 달림 여부 확인
        if (question.getIsAnswered()) {
            throw new BusinessException(ErrorCode.QUESTION_DELETE_RESTRICTED_HAS_ANSWER);
        }

        // 경매 종료 30분 전 확인
        if (question.isNearAuctionEnd()) {
            throw new BusinessException(ErrorCode.QNA_DELETE_RESTRICTED_NEAR_END);
        }

        // 질문 Soft Delete
        question.softDelete(userId);
        questionRepository.save(question);
    }

    /**
     * 답변 등록
     * 
     * 비즈니스 로직:
     * 1. 질문 존재 여부 확인
     * 2. 판매자 본인 확인
     * 3. 이미 답변이 달렸는지 확인
     * 4. 경매 종료 30분 전인지 확인
     * 5. 답변 생성 및 저장
     * 6. 질문에 답변 완료 표시
     * 
     * @param questionId 질문 ID
     * @param userId 답변자(판매자) 사용자 ID
     * @param request 답변 등록 요청
     * @return 생성된 답변 정보
     */
    @Transactional
    public AnswerResponse createAnswer(Long questionId, Long userId, CreateAnswerRequest request) {
        Question question = questionRepository.findByIdWithQuestionerAndAuction(questionId)
            .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        User answerer = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 판매자 본인 확인
        if (!question.isSellerMatches(answerer)) {
            throw new BusinessException(ErrorCode.ONLY_SELLER_CAN_ANSWER);
        }

        // 이미 답변이 달렸는지 확인
        if (question.getIsAnswered()) {
            throw new BusinessException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        // 경매 종료 30분 전 확인
        if (question.isNearAuctionEnd()) {
            throw new BusinessException(ErrorCode.ANSWER_TIME_LIMIT_EXCEEDED);
        }

        // 답변 생성 및 저장 (Question에 답변 완료 표시도 포함)
        Answer answer = Answer.createAnswer(question, answerer, request.getContent());
        Answer savedAnswer = answerRepository.save(answer);

        return AnswerResponse.from(savedAnswer, userId);
    }

    /**
     * 답변 수정
     * 
     * 비즈니스 로직:
     * 1. 답변 존재 여부 확인
     * 2. 답변자(판매자) 본인 확인
     * 3. 경매 진행 중인지 확인 (낙찰 후에는 수정 불가)
     * 4. 경매 종료 30분 전인지 확인
     * 5. 답변 내용 수정
     * 
     * @param answerId 답변 ID
     * @param userId 현재 사용자 ID
     * @param request 답변 수정 요청
     * @return 수정된 답변 정보
     */
    @Transactional
    public AnswerResponse updateAnswer(Long answerId, Long userId, UpdateAnswerRequest request) {
        Answer answer = answerRepository.findByIdWithQuestionAndAnswerer(answerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        // 답변자(판매자) 본인 확인
        if (!answer.getAnswerer().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_ANSWER_AUTHOR);
        }

        // 답변 수정 (경매 상태 확인은 Answer 엔티티에서 확인)
        answer.updateContent(request.getContent());

        return AnswerResponse.from(answer, userId);
    }

    /**
     * 답변 삭제
     * 
     * 비즈니스 로직:
     * 1. 답변 존재 여부 확인
     * 2. 답변자(판매자) 본인 확인
     * 3. 경매 진행 중인지 확인 (낙찰 후에는 삭제 불가)
     * 4. 경매 종료 30분 전인지 확인
     * 5. 답변 삭제
     * 6. 질문에 답변 미완료 표시
     * 
     * @param answerId 답변 ID
     * @param userId 현재 사용자 ID
     */
    @Transactional
    public void deleteAnswer(Long answerId, Long userId) {
        Answer answer = answerRepository.findByIdWithQuestionAndAnswerer(answerId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));

        // 답변자(판매자) 본인 확인
        if (!answer.getAnswerer().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NOT_ANSWER_AUTHOR);
        }

        // 수정 가능 여부 확인 (경매 상태 포함)
        if (!answer.canModifyBySeller()) {
            throw new BusinessException(ErrorCode.QNA_DELETE_RESTRICTED_NEAR_END);
        }

        // 질문에 답변 미완료 표시
        Question question = answer.getQuestion();
        question.markAsUnanswered();

        // 답변 Soft Delete
        answer.softDelete(userId);
        answerRepository.save(answer);
    }

    /**
     * 특정 경매의 Q&A 목록 조회
     * 
     * @param auctionId 경매 ID
     * @param currentUserId 현재 사용자 ID (권한 확인용)
     * @param pageable 페이징 정보
     * @return Q&A 목록
     */
    public Page<QuestionResponse> getQnaByAuction(Long auctionId, Long currentUserId, Pageable pageable) {
        // 경매 존재 여부 확인
        if (!auctionRepository.existsById(auctionId)) {
            throw new IllegalArgumentException("존재하지 않는 경매입니다.");
        }

        Page<Question> questions = questionRepository.findByAuctionIdOrderByCreatedAtDesc(auctionId, pageable);
        
        return questions.map(question -> {
            // 답변이 있는지 확인하고 함께 조회
            AnswerResponse answerResponse = null;
            if (question.getIsAnswered()) {
                Answer answer = answerRepository.findByQuestionIdWithAnswerer(question.getId()).orElse(null);
                if (answer != null) {
                    answerResponse = AnswerResponse.from(answer, currentUserId);
                }
            }
            
            return QuestionResponse.from(question, answerResponse, currentUserId);
        });
    }

    /**
     * 특정 경매의 Q&A 통계 조회
     * 
     * @param auctionId 경매 ID
     * @return Q&A 통계 정보
     */
    public QnaStatistics getQnaStatistics(Long auctionId) {
        // 경매 존재 여부 확인
        if (!auctionRepository.existsById(auctionId)) {
            throw new IllegalArgumentException("존재하지 않는 경매입니다.");
        }

        Long totalQuestions = questionRepository.countByAuctionId(auctionId);
        Long unansweredQuestions = questionRepository.countUnansweredByAuctionId(auctionId);

        return new QnaStatistics(totalQuestions, unansweredQuestions);
    }

    /**
     * Q&A 통계 정보 클래스
     */
    public static class QnaStatistics {
        private final Long totalQuestions;
        private final Long unansweredQuestions;

        public QnaStatistics(Long totalQuestions, Long unansweredQuestions) {
            this.totalQuestions = totalQuestions;
            this.unansweredQuestions = unansweredQuestions;
        }

        public Long getTotalQuestions() {
            return totalQuestions;
        }

        public Long getUnansweredQuestions() {
            return unansweredQuestions;
        }

        public Long getAnsweredQuestions() {
            return totalQuestions - unansweredQuestions;
        }
    }
}