package com.cherrypick.app.domain.qna.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.qna.dto.request.CreateAnswerRequest;
import com.cherrypick.app.domain.qna.dto.request.CreateQuestionRequest;
import com.cherrypick.app.domain.qna.dto.response.AnswerResponse;
import com.cherrypick.app.domain.qna.dto.response.QuestionResponse;
import com.cherrypick.app.domain.qna.entity.Answer;
import com.cherrypick.app.domain.qna.entity.Question;
import com.cherrypick.app.domain.qna.repository.AnswerRepository;
import com.cherrypick.app.domain.qna.repository.QuestionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class QnaServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QnaService qnaService;

    private User seller;
    private User questioner;
    private Auction auction;
    private Question question;

    @BeforeEach
    void setUp() throws Exception {
        // 판매자 설정
        seller = User.builder()
            .email("seller@test.com")
            .nickname("판매자")
            .phoneNumber("01012345678")
            .password("password")
            .pointBalance(100000L)
            .build();
        setId(seller, 1L);

        // 질문자 설정
        questioner = User.builder()
            .email("buyer@test.com")
            .nickname("구매자")
            .phoneNumber("01087654321")
            .password("password")
            .pointBalance(50000L)
            .build();
        setId(questioner, 2L);

        // 경매 설정 (진행 중)
        auction = Auction.createAuction(
            seller,
            "테스트 상품",
            "테스트 상품 설명",
            Category.ELECTRONICS,
            BigDecimal.valueOf(10000),
            BigDecimal.valueOf(50000),
            24,
            RegionScope.NATIONWIDE,
            null,
            null
        );

        // 질문 설정 - Builder 사용
        question = Question.builder()
            .auction(auction)
            .questioner(questioner)
            .content("상품 상태가 어떤가요?")
            .isAnswered(false)
            .build();
    }

    @Test
    @DisplayName("정상적인 질문 등록")
    void createQuestion_Success() {
        // given
        CreateQuestionRequest request = new CreateQuestionRequest("상품 상태가 어떤가요?");
        
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(userRepository.findById(2L)).thenReturn(Optional.of(questioner));
        when(questionRepository.save(any(Question.class))).thenReturn(question);

        // when
        QuestionResponse response = qnaService.createQuestion(1L, 2L, request);

        // then
        assertNotNull(response);
        assertEquals("상품 상태가 어떤가요?", response.getContent());
        assertEquals("구매자", response.getQuestioner().getNickname());
        assertEquals("구**", response.getQuestioner().getMaskedNickname());
        assertFalse(response.getIsAnswered());
        assertTrue(response.getCanModify());
        
        verify(questionRepository).save(any(Question.class));
    }

    @Test
    @DisplayName("자신의 경매에 질문 등록 시 예외 발생")
    void createQuestion_SelfAuction_ThrowsException() {
        // given
        CreateQuestionRequest request = new CreateQuestionRequest("자기 경매에 질문");
        
        when(auctionRepository.findById(1L)).thenReturn(Optional.of(auction));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller)); // 판매자가 질문

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> qnaService.createQuestion(1L, 1L, request)
        );
        
        assertEquals("자신의 경매에는 질문할 수 없습니다.", exception.getMessage());
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("존재하지 않는 경매에 질문 등록 시 예외 발생")
    void createQuestion_AuctionNotFound_ThrowsException() {
        // given
        CreateQuestionRequest request = new CreateQuestionRequest("질문 내용");
        
        when(auctionRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> qnaService.createQuestion(999L, 2L, request)
        );
        
        assertEquals("존재하지 않는 경매입니다.", exception.getMessage());
        verify(questionRepository, never()).save(any(Question.class));
    }

    @Test
    @DisplayName("정상적인 답변 등록") 
    void createAnswer_Success() throws Exception {
        // given
        CreateAnswerRequest request = new CreateAnswerRequest("상품 상태는 매우 좋습니다.");
        
        // Mock Answer 객체 생성
        Answer mockAnswer = mock(Answer.class);
        when(mockAnswer.getId()).thenReturn(1L);
        when(mockAnswer.getContent()).thenReturn(request.getContent());
        when(mockAnswer.getAnswerer()).thenReturn(seller);
        when(mockAnswer.isAnswererMatches(seller)).thenReturn(true);
        when(mockAnswer.canModifyBySeller()).thenReturn(true);
        
        when(questionRepository.findByIdWithQuestionerAndAuction(1L)).thenReturn(Optional.of(question));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        when(answerRepository.save(any(Answer.class))).thenReturn(mockAnswer);

        // when
        AnswerResponse response = qnaService.createAnswer(1L, 1L, request);

        // then
        assertNotNull(response);
        assertEquals("상품 상태는 매우 좋습니다.", response.getContent());
        assertEquals("판매자", response.getAnswerer().getNickname());
        assertTrue(response.getAnswerer().getIsSeller());
        assertTrue(response.getCanModify());
        
        verify(answerRepository).save(any(Answer.class));
    }

    @Test
    @DisplayName("판매자가 아닌 사용자가 답변 등록 시 예외 발생")
    void createAnswer_NotSeller_ThrowsException() {
        // given
        CreateAnswerRequest request = new CreateAnswerRequest("답변 내용");
        
        when(questionRepository.findByIdWithQuestionerAndAuction(1L)).thenReturn(Optional.of(question));
        when(userRepository.findById(2L)).thenReturn(Optional.of(questioner)); // 질문자가 답변

        // when & then
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> qnaService.createAnswer(1L, 2L, request)
        );
        
        assertEquals("해당 경매의 판매자만 답변할 수 있습니다.", exception.getMessage());
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("이미 답변이 달린 질문에 답변 등록 시 예외 발생")
    void createAnswer_AlreadyAnswered_ThrowsException() {
        // given
        CreateAnswerRequest request = new CreateAnswerRequest("또 다른 답변");
        question.markAsAnswered(); // 이미 답변 완료 표시
        
        when(questionRepository.findByIdWithQuestionerAndAuction(1L)).thenReturn(Optional.of(question));
        when(userRepository.findById(1L)).thenReturn(Optional.of(seller));

        // when & then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> qnaService.createAnswer(1L, 1L, request)
        );
        
        assertEquals("이미 답변이 달린 질문입니다.", exception.getMessage());
        verify(answerRepository, never()).save(any(Answer.class));
    }

    @Test
    @DisplayName("답변이 달린 질문 수정 시 예외 발생")
    void updateQuestion_AlreadyAnswered_ThrowsException() {
        // given
        question.markAsAnswered(); // 답변 완료 표시

        // when & then
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> question.updateContent("수정된 질문")
        );
        
        assertEquals("답변이 달린 질문은 수정할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("Q&A 통계 조회")
    void getQnaStatistics_Success() {
        // given
        when(auctionRepository.existsById(1L)).thenReturn(true);
        when(questionRepository.countByAuctionId(1L)).thenReturn(5L);
        when(questionRepository.countUnansweredByAuctionId(1L)).thenReturn(2L);

        // when
        QnaService.QnaStatistics statistics = qnaService.getQnaStatistics(1L);

        // then
        assertNotNull(statistics);
        assertEquals(5L, statistics.getTotalQuestions());
        assertEquals(2L, statistics.getUnansweredQuestions());
        assertEquals(3L, statistics.getAnsweredQuestions());
    }

    /**
     * Reflection을 사용해서 ID 필드를 설정하는 헬퍼 메서드
     */
    private void setId(Object entity, Long id) throws Exception {
        Field field = entity.getClass().getDeclaredField("id");
        field.setAccessible(true);
        field.set(entity, id);
    }
}