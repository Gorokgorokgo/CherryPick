package com.cherrypick.app.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    
    List<UserAccount> findByUserIdOrderByIsPrimaryDescCreatedAtDesc(Long userId);
    
    Optional<UserAccount> findByUserIdAndIsPrimaryTrue(Long userId);
    
    boolean existsByUserIdAndAccountNumber(Long userId, String accountNumber);
    
    long countByUserId(Long userId);
}