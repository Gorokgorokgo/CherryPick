package com.cherrypick.app.domain.qna.repository;

import com.cherrypick.app.domain.qna.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    /**
     * 특정 질문의 답변 조회
     */
    @Query("SELECT a FROM Answer a " +
           "WHERE a.question.id = :questionId " +
           "AND a.deletedAt IS NULL")
    Optional<Answer> findByQuestionId(@Param("questionId") Long questionId);

    /**
     * 특정 질문의 답변을 답변자와 함께 조회
     */
    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.answerer " +
           "WHERE a.question.id = :questionId " +
           "AND a.deletedAt IS NULL")
    Optional<Answer> findByQuestionIdWithAnswerer(@Param("questionId") Long questionId);

    /**
     * 특정 답변을 질문, 답변자와 함께 조회
     */
    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.question q " +
           "JOIN FETCH q.auction " +
           "JOIN FETCH a.answerer " +
           "WHERE a.id = :answerId " +
           "AND a.deletedAt IS NULL")
    Optional<Answer> findByIdWithQuestionAndAnswerer(@Param("answerId") Long answerId);

    /**
     * 특정 경매의 모든 답변 조회 (질문과 함께)
     */
    @Query("SELECT a FROM Answer a " +
           "JOIN FETCH a.question q " +
           "JOIN FETCH a.answerer " +
           "WHERE q.auction.id = :auctionId " +
           "AND a.deletedAt IS NULL " +
           "AND q.deletedAt IS NULL " +
           "ORDER BY a.createdAt DESC")
    List<Answer> findByAuctionIdWithQuestionAndAnswerer(@Param("auctionId") Long auctionId);

    /**
     * 특정 판매자가 작성한 답변 수 조회
     */
    @Query("SELECT COUNT(a) FROM Answer a " +
           "WHERE a.answerer.id = :answererId " +
           "AND a.deletedAt IS NULL")
    Long countByAnswererId(@Param("answererId") Long answererId);

    /**
     * 특정 질문에 답변이 존재하는지 확인
     */
    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Answer a " +
           "WHERE a.question.id = :questionId " +
           "AND a.deletedAt IS NULL")
    boolean existsByQuestionId(@Param("questionId") Long questionId);
}