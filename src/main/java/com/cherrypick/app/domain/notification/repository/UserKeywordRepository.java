package com.cherrypick.app.domain.notification.repository;

import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.notification.entity.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {

    /**
     * 사용자의 모든 활성 키워드 조회
     */
    List<UserKeyword> findByUserIdAndIsActiveTrue(Long userId);

    /**
     * 사용자의 모든 키워드 조회
     */
    List<UserKeyword> findByUserId(Long userId);

    /**
     * 특정 키워드와 매칭되는 모든 활성 사용자 키워드 조회 (경매 제목 매칭용)
     * 대소문자 무시, 부분 매칭
     */
    @Query("SELECT uk FROM UserKeyword uk " +
           "WHERE uk.isActive = true " +
           "AND LOWER(:auctionTitle) LIKE CONCAT('%', uk.keyword, '%') " +
           "AND (uk.category IS NULL OR uk.category = :category)")
    List<UserKeyword> findMatchingKeywords(
            @Param("auctionTitle") String auctionTitle,
            @Param("category") Category category);

    /**
     * 특정 키워드가 포함된 활성 사용자 키워드 조회 (정확한 키워드 매칭)
     */
    @Query("SELECT uk FROM UserKeyword uk " +
           "WHERE uk.isActive = true " +
           "AND uk.keyword = LOWER(:keyword) " +
           "AND (uk.category IS NULL OR uk.category = :category)")
    List<UserKeyword> findByKeywordAndCategory(
            @Param("keyword") String keyword,
            @Param("category") Category category);

    /**
     * 사용자가 이미 동일한 키워드를 등록했는지 확인
     */
    Optional<UserKeyword> findByUserIdAndKeyword(Long userId, String keyword);

    /**
     * 사용자의 키워드 수 카운트
     */
    long countByUserIdAndIsActiveTrue(Long userId);

    /**
     * 키워드 삭제
     */
    void deleteByUserIdAndId(Long userId, Long keywordId);
}
