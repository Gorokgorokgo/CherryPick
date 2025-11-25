package com.cherrypick.app.domain.user.repository;

import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 기본 조회 메서드 (탈퇴한 사용자 포함)
    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findUserById(@Param("userId") Long userId);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByNickname(String nickname);

    boolean existsByEmail(String email);

    // Soft Delete 고려 조회 메서드 (탈퇴하지 않은 사용자만)
    @Query("SELECT u FROM User u WHERE u.phoneNumber = :phoneNumber AND u.deletedAt IS NULL")
    Optional<User> findByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT u FROM User u WHERE u.nickname = :nickname AND u.deletedAt IS NULL")
    Optional<User> findByNicknameAndNotDeleted(@Param("nickname") String nickname);

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailAndNotDeleted(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.id = :userId AND u.deletedAt IS NULL")
    Optional<User> findByIdAndNotDeleted(@Param("userId") Long userId);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.phoneNumber = :phoneNumber AND u.deletedAt IS NULL")
    boolean existsByPhoneNumberAndNotDeleted(@Param("phoneNumber") String phoneNumber);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.nickname = :nickname AND u.deletedAt IS NULL")
    boolean existsByNicknameAndNotDeleted(@Param("nickname") String nickname);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    boolean existsByEmailAndNotDeleted(@Param("email") String email);
}