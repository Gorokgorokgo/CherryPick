package com.cherrypick.app.repository;

import com.cherrypick.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 사용자 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 전화번호로 사용자 조회
     */
    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * 전화번호 존재 여부 확인
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * 닉네임 존재 여부 확인
     */
    boolean existsByNickname(String nickname);

    /**
     * 활성 사용자만 조회
     */
    Optional<User> findByIdAndIsActiveTrue(Long id);

    /**
     * RefreshToken으로 사용자 조회
     */
    Optional<User> findByRefreshToken(String refreshToken);

    /**
     * 신뢰도(레벨) 순위 조회
     */
    @Query("SELECT u FROM User u WHERE u.isActive = true ORDER BY (u.buyerLevel + u.sellerLevel) DESC")
    java.util.List<User> findTopUsersByTrustScore(org.springframework.data.domain.Pageable pageable);

    /**
     * 구매자 레벨별 사용자 수 조회
     */
    @Query("SELECT u.buyerLevel, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.buyerLevel")
    java.util.List<Object[]> countUsersByBuyerLevel();

    /**
     * 판매자 레벨별 사용자 수 조회
     */
    @Query("SELECT u.sellerLevel, COUNT(u) FROM User u WHERE u.isActive = true GROUP BY u.sellerLevel")
    java.util.List<Object[]> countUsersBySellerLevel();
}