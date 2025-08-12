package com.cherrypick.app.domain.notification.repository;

import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 알림 설정 리포지토리
 */
@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {

    /**
     * 사용자별 알림 설정 조회
     */
    Optional<NotificationSetting> findByUserId(Long userId);
    
    /**
     * FCM 토큰으로 알림 설정 조회
     */
    Optional<NotificationSetting> findByFcmToken(String fcmToken);
}