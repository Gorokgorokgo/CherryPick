package com.cherrypick.app.domain.user.repository;

import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    @Query("SELECT ua FROM UserAccount ua " +
           "WHERE ua.user.id = :userId " +
           "AND ua.deletedAt IS NULL " +
           "ORDER BY ua.isPrimary DESC, ua.createdAt DESC")
    List<UserAccount> findByUserIdOrderByIsPrimaryDescCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT ua FROM UserAccount ua " +
           "WHERE ua.user.id = :userId " +
           "AND ua.isPrimary = true " +
           "AND ua.deletedAt IS NULL")
    Optional<UserAccount> findByUserIdAndIsPrimaryTrue(@Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(ua) > 0 THEN true ELSE false END FROM UserAccount ua " +
           "WHERE ua.user.id = :userId " +
           "AND ua.accountNumber = :accountNumber " +
           "AND ua.deletedAt IS NULL")
    boolean existsByUserIdAndAccountNumber(@Param("userId") Long userId, @Param("accountNumber") String accountNumber);

    @Query("SELECT COUNT(ua) FROM UserAccount ua " +
           "WHERE ua.user.id = :userId " +
           "AND ua.deletedAt IS NULL")
    long countByUserId(@Param("userId") Long userId);
}