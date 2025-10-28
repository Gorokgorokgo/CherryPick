package com.cherrypick.app.domain.notification.service;

import com.cherrypick.app.domain.notification.dto.request.UpdateNotificationSettingRequest;
import com.cherrypick.app.domain.notification.dto.response.NotificationHistoryResponse;
import com.cherrypick.app.domain.notification.dto.response.NotificationSettingResponse;
import com.cherrypick.app.domain.notification.entity.NotificationHistory;
import com.cherrypick.app.domain.notification.entity.NotificationSetting;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.repository.NotificationHistoryRepository;
import com.cherrypick.app.domain.notification.repository.NotificationSettingRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationSettingRepository notificationSettingRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final UserRepository userRepository;

    /**
     * 사용자 알림 설정 조회
     */
    public NotificationSettingResponse getNotificationSetting(Long userId) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // 설정이 없으면 기본 설정 생성
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    NotificationSetting defaultSetting = NotificationSetting.createDefaultSetting(user);
                    return notificationSettingRepository.save(defaultSetting);
                });

        return NotificationSettingResponse.from(setting);
    }

    /**
     * 알림 설정 업데이트
     */
    @Transactional
    public NotificationSettingResponse updateNotificationSetting(Long userId, UpdateNotificationSettingRequest request) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    return NotificationSetting.createDefaultSetting(user);
                });

        // 불변 객체 패턴으로 설정 업데이트
        NotificationSetting updatedSetting = setting.updateSettings(
                request.getBidNotification(),
                request.getWinningNotification(),
                request.getConnectionPaymentNotification(),
                request.getChatActivationNotification(),
                request.getMessageNotification(),
                request.getTransactionCompletionNotification(),
                request.getPromotionNotification()
        );

        NotificationSetting savedSetting = notificationSettingRepository.save(updatedSetting);
        
        return NotificationSettingResponse.from(savedSetting);
    }

    /**
     * FCM 토큰 업데이트
     */
    @Transactional
    public void updateFcmToken(Long userId, String fcmToken) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    return NotificationSetting.createDefaultSetting(user);
                });

        NotificationSetting updatedSetting = setting.updateFcmToken(fcmToken);
        notificationSettingRepository.save(updatedSetting);
    }

    /**
     * 사용자 알림 목록 조회
     */
    public Page<NotificationHistoryResponse> getNotificationHistory(Long userId, Pageable pageable) {
        Page<NotificationHistory> notifications = notificationHistoryRepository
                .findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return notifications.map(NotificationHistoryResponse::from);
    }

    /**
     * 특정 타입 알림 목록 조회
     */
    public Page<NotificationHistoryResponse> getNotificationHistoryByType(Long userId, NotificationType type, Pageable pageable) {
        Page<NotificationHistory> notifications = notificationHistoryRepository
                .findByUserIdAndTypeOrderByCreatedAtDesc(userId, type, pageable);

        return notifications.map(NotificationHistoryResponse::from);
    }

    /**
     * 읽지 않은 알림 개수 조회
     */
    public long getUnreadNotificationCount(Long userId) {
        return notificationHistoryRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * 특정 알림 읽음 처리
     */
    @Transactional
    public void markNotificationAsRead(Long userId, Long notificationId) {
        NotificationHistory notification = notificationHistoryRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 권한 확인
        if (!notification.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("알림 읽음 처리 권한이 없습니다.");
        }

        if (!notification.getIsRead()) {
            NotificationHistory updatedNotification = notification.markAsRead();
            notificationHistoryRepository.save(updatedNotification);
        }
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public int markAllNotificationsAsRead(Long userId) {
        int updatedCount = notificationHistoryRepository.markAllAsReadByUserId(userId);
        
        return updatedCount;
    }

    /**
     * 모든 알림 끄기
     */
    @Transactional
    public NotificationSettingResponse disableAllNotifications(Long userId) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    return NotificationSetting.createDefaultSetting(user);
                });

        NotificationSetting updatedSetting = setting.disableAllNotifications();
        NotificationSetting savedSetting = notificationSettingRepository.save(updatedSetting);
        
        return NotificationSettingResponse.from(savedSetting);
    }

    /**
     * 필수 알림만 켜기
     */
    @Transactional
    public NotificationSettingResponse enableEssentialNotificationsOnly(Long userId) {
        NotificationSetting setting = notificationSettingRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
                    return NotificationSetting.createDefaultSetting(user);
                });

        NotificationSetting updatedSetting = setting.enableEssentialNotificationsOnly();
        NotificationSetting savedSetting = notificationSettingRepository.save(updatedSetting);

        return NotificationSettingResponse.from(savedSetting);
    }

    /**
     * 모든 알림 삭제
     */
    @Transactional
    public int deleteAllNotifications(Long userId) {
        int deletedCount = notificationHistoryRepository.deleteAllByUserId(userId);

        return deletedCount;
    }
}