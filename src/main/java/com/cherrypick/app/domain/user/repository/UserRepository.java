package com.cherrypick.app.domain.user.repository;

import com.cherrypick.app.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByPhoneNumber(String phoneNumber);
    
    Optional<User> findByNickname(String nickname);
    
    Optional<User> findByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.id = :userId")
    Optional<User> findUserById(@Param("userId") Long userId);
    
    boolean existsByPhoneNumber(String phoneNumber);
    
    boolean existsByNickname(String nickname);
    
    boolean existsByEmail(String email);
}