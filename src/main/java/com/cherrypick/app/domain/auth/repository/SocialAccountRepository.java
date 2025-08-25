package com.cherrypick.app.domain.auth.repository;

import com.cherrypick.app.domain.auth.entity.SocialAccount;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {
    
    Optional<SocialAccount> findByProviderAndProviderId(SocialAccount.SocialProvider provider, String providerId);
    
    List<SocialAccount> findByUserAndIsActiveTrue(User user);
    
    List<SocialAccount> findByUser(User user);
    
    boolean existsByProviderAndProviderId(SocialAccount.SocialProvider provider, String providerId);
    
    Optional<SocialAccount> findByProviderAndProviderIdAndIsActiveTrue(SocialAccount.SocialProvider provider, String providerId);
}