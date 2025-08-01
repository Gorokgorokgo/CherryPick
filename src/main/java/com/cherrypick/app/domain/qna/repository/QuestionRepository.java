package com.cherrypick.app.domain.qna.repository;

import com.cherrypick.app.domain.qna.entity.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * 특정 경매의 질문 목록 조회 (페이징)
     */
    @Query("SELECT q FROM Question q " +
           "WHERE q.auction.id = :auctionId " +
           "ORDER BY q.createdAt DESC")
    Page<Question> findByAuctionIdOrderByCreatedAtDesc(@Param("auctionId") Long auctionId, Pageable pageable);

    /**
     * 특정 경매의 질문 목록 조회 (전체)
     */
    @Query("SELECT q FROM Question q " +
           "WHERE q.auction.id = :auctionId " +
           "ORDER BY q.createdAt DESC")
    List<Question> findByAuctionIdOrderByCreatedAtDesc(@Param("auctionId") Long auctionId);

    /**
     * 특정 사용자가 작성한 질문 목록 조회
     */
    @Query("SELECT q FROM Question q " +
           "WHERE q.questioner.id = :questionerId " +
           "ORDER BY q.createdAt DESC")
    Page<Question> findByQuestionerIdOrderByCreatedAtDesc(@Param("questionerId") Long questionerId, Pageable pageable);

    /**
     * 특정 경매의 답변되지 않은 질문 수 조회
     */
    @Query("SELECT COUNT(q) FROM Question q " +
           "WHERE q.auction.id = :auctionId " +
           "AND q.isAnswered = false")
    Long countUnansweredByAuctionId(@Param("auctionId") Long auctionId);

    /**
     * 특정 경매의 총 질문 수 조회
     */
    Long countByAuctionId(Long auctionId);

    /**
     * 특정 질문을 질문자와 함께 조회
     */
    @Query("SELECT q FROM Question q " +
           "JOIN FETCH q.questioner " +
           "WHERE q.id = :questionId")
    Optional<Question> findByIdWithQuestioner(@Param("questionId") Long questionId);

    /**
     * 특정 질문을 경매 및 질문자와 함께 조회
     */
    @Query("SELECT q FROM Question q " +
           "JOIN FETCH q.questioner " +
           "JOIN FETCH q.auction " +
           "WHERE q.id = :questionId")
    Optional<Question> findByIdWithQuestionerAndAuction(@Param("questionId") Long questionId);
}