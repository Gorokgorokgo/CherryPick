package com.cherrypick.app.domain.notification.controller;

import com.cherrypick.app.domain.notification.dto.request.UpdateFcmTokenRequest;
import com.cherrypick.app.domain.notification.dto.request.UpdateNotificationSettingRequest;
import com.cherrypick.app.domain.notification.dto.response.NotificationHistoryResponse;
import com.cherrypick.app.domain.notification.dto.response.NotificationSettingResponse;
import com.cherrypick.app.domain.notification.enums.NotificationType;
import com.cherrypick.app.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "10단계 - 알림 관리", description = """
    푸시 알림 및 알림 설정 관리 API
    
    **FCM 푸시 알림 시스템:**
    - Firebase Cloud Messaging 연동
    - 실시간 알림 발송 (입찰, 낙찰, 연결서비스, 채팅 등)
    - 사용자별 알림 설정 관리
    - 알림 히스토리 및 읽음 처리
    
    **알림 타입:**
    - NEW_BID: 새로운 입찰 (판매자용)
    - AUCTION_WON: 낙찰 알림 (구매자용)  
    - CONNECTION_PAYMENT_REQUEST: 연결 서비스 결제 요청 (판매자용)
    - CHAT_ACTIVATED: 채팅 활성화 (구매자용)
    - NEW_MESSAGE: 새 메시지
    - TRANSACTION_COMPLETED: 거래 완료
    - PROMOTION: 프로모션
    
    **현재 무료 프로모션:**
    - 연결 서비스 수수료 0%
    - 모든 알림 기능 무료 제공
    """)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
        summary = "FCM 토큰 등록/업데이트",
        description = """
            모바일 앱의 FCM 토큰을 서버에 등록하거나 업데이트합니다.
            
            **FCM 토큰이란:**
            - Firebase Cloud Messaging에서 기기를 식별하는 고유 문자열
            - 앱 설치시/업데이트시/복원시 새로 생성됨
            - 서버에서 특정 기기로 푸시 알림 발송시 사용
            
            **토큰 등록 시점:**
            - 앱 최초 실행시
            - 토큰 갱신시 (Firebase SDK가 자동 감지)
            - 로그인 완료 후
            
            **주의사항:**
            - 토큰은 주기적으로 갱신될 수 있음
            - 앱에서 토큰 변경을 감지하면 즉시 서버 업데이트 필요
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "FCM 토큰 등록/업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 - 토큰 형식 오류"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "FCM 토큰 정보") @Valid @RequestBody UpdateFcmTokenRequest request) {
        
        notificationService.updateFcmToken(userId, request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "알림 설정 조회",
        description = """
            사용자의 현재 알림 설정을 조회합니다.
            
            **조회 내용:**
            - 각 알림 타입별 수신 여부
            - FCM 토큰 등록 상태
            - 알림 설정 ID 및 사용자 정보
            
            **알림 타입별 설명:**
            - bidNotification: 새로운 입찰 알림 (판매자용)
            - winningNotification: 낙찰 알림 (구매자용)
            - connectionPaymentNotification: 연결 서비스 결제 요청 (판매자용)
            - chatActivationNotification: 채팅 활성화 (구매자용)
            - messageNotification: 새 메시지 알림
            - transactionCompletionNotification: 거래 완료 알림
            - promotionNotification: 프로모션 알림
            
            **최초 조회시:**
            - 설정이 없으면 기본 설정 자동 생성
            - 대부분 알림이 활성화 상태 (프로모션 제외)
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "알림 설정 조회 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationSettingResponse.class),
                examples = @ExampleObject(
                    name = "알림 설정 응답",
                    value = """
                    {
                      "id": 1,
                      "userId": 1,
                      "bidNotification": true,
                      "winningNotification": true,
                      "connectionPaymentNotification": true,
                      "chatActivationNotification": true,
                      "messageNotification": false,
                      "transactionCompletionNotification": true,
                      "promotionNotification": false,
                      "fcmTokenRegistered": true
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/settings")
    public ResponseEntity<NotificationSettingResponse> getNotificationSetting(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {
        
        NotificationSettingResponse response = notificationService.getNotificationSetting(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "알림 설정 업데이트",
        description = """
            사용자의 알림 설정을 업데이트합니다.
            
            **업데이트 방식:**
            - Partial Update 지원 (null이 아닌 값만 업데이트)
            - 개별 알림 타입별로 켜기/끄기 가능
            - 전체 설정을 한 번에 업데이트하거나 일부만 변경 가능
            
            **요청 예시:**
            ```json
            {
              "messageNotification": false,      // 채팅 알림만 끄기
              "promotionNotification": true      // 프로모션 알림만 켜기
            }
            ```
            
            **주의사항:**
            - null로 전송된 필드는 변경되지 않음
            - 필수 알림(입찰, 낙찰, 거래완료)을 끄는 것은 권장하지 않음
            - 설정 변경 즉시 적용됨
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "알림 설정 업데이트 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = NotificationSettingResponse.class)
            )
        ),
        @ApiResponse(responseCode = "400", description = "잘못된 요청"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/settings")
    public ResponseEntity<NotificationSettingResponse> updateNotificationSetting(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "알림 설정 업데이트 정보") @RequestBody UpdateNotificationSettingRequest request) {
        
        NotificationSettingResponse response = notificationService.updateNotificationSetting(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "알림 목록 조회",
        description = """
            사용자의 알림 히스토리를 최신순으로 조회합니다.
            
            **조회 내용:**
            - 모든 타입의 알림 목록 (최신순)
            - 읽음/안읽음 상태
            - FCM 발송 성공 여부
            - 관련 리소스 ID (경매 ID, 연결 서비스 ID 등)
            
            **페이징:**
            - 기본 20개씩 조회
            - 페이지 번호는 0부터 시작
            - 총 개수 및 페이지 정보 포함
            
            **정렬:**
            - createdAt DESC (생성시간 최신순)
            - 읽지 않은 알림이 먼저 표시되지는 않음
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "알림 목록 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "알림 목록 응답",
                    value = """
                    {
                      "content": [
                        {
                          "id": 1,
                          "type": "CHAT_ACTIVATED",
                          "typeDescription": "채팅 활성화",
                          "title": "채팅이 활성화되었습니다!",
                          "message": "'iPhone 14 Pro 판매' 경매의 판매자와 채팅을 시작할 수 있습니다.",
                          "resourceId": 123,
                          "isRead": false,
                          "fcmSent": true,
                          "createdAt": "2024-08-04T10:30:00",
                          "readAt": null
                        }
                      ],
                      "pageable": {
                        "page": 0,
                        "size": 20
                      },
                      "totalElements": 5,
                      "totalPages": 1
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/history")
    public ResponseEntity<Page<NotificationHistoryResponse>> getNotificationHistory(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "페이지 정보 (기본: 0페이지, 20개씩)") @PageableDefault(size = 20) Pageable pageable) {
        
        Page<NotificationHistoryResponse> response = notificationService.getNotificationHistory(userId, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "특정 타입 알림 목록 조회",
        description = """
            특정 타입의 알림만 필터링하여 조회합니다.
            
            **필터링 가능한 타입:**
            - NEW_BID: 새로운 입찰 알림
            - AUCTION_WON: 낙찰 알림
            - CONNECTION_PAYMENT_REQUEST: 연결 서비스 결제 요청
            - CHAT_ACTIVATED: 채팅 활성화
            - NEW_MESSAGE: 새 메시지
            - TRANSACTION_COMPLETED: 거래 완료
            - PROMOTION: 프로모션
            
            **활용 예시:**
            - 판매자: NEW_BID, CONNECTION_PAYMENT_REQUEST 조회
            - 구매자: AUCTION_WON, CHAT_ACTIVATED 조회
            - 거래 관련: TRANSACTION_COMPLETED 조회
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "특정 타입 알림 목록 조회 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 알림 타입"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/history/type/{type}")
    public ResponseEntity<Page<NotificationHistoryResponse>> getNotificationHistoryByType(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "알림 타입", example = "NEW_BID") @PathVariable NotificationType type,
            @Parameter(description = "페이지 정보") @PageableDefault(size = 20) Pageable pageable) {
        
        Page<NotificationHistoryResponse> response = notificationService.getNotificationHistoryByType(userId, type, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "읽지 않은 알림 개수 조회",
        description = """
            사용자의 읽지 않은 알림 개수를 조회합니다.
            
            **용도:**
            - 앱 아이콘 배지 개수 표시
            - 알림 탭의 빨간 점 표시
            - 메인 화면의 알림 카운터
            
            **주의사항:**
            - 실시간으로 업데이트되는 값
            - 알림을 읽으면 즉시 감소
            - 새 알림 수신시 즉시 증가
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "읽지 않은 알림 개수 조회 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "읽지 않은 알림 개수",
                    value = """
                    {
                      "unreadCount": 3
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationCount(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {
        
        long unreadCount = notificationService.getUnreadNotificationCount(userId);
        return ResponseEntity.ok(Map.of("unreadCount", unreadCount));
    }

    @Operation(
        summary = "특정 알림 읽음 처리",
        description = """
            특정 알림을 읽음 상태로 변경합니다.
            
            **처리 내용:**
            - isRead 상태를 true로 변경
            - readAt 시간을 현재 시간으로 설정
            - 이미 읽은 알림은 변경되지 않음
            
            **권한 확인:**
            - 본인의 알림만 읽음 처리 가능
            - 다른 사용자의 알림은 접근 불가
            
            **활용 시점:**
            - 알림 상세보기 진입시
            - 알림 리스트에서 클릭시
            - 관련 화면 진입시
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패"),
        @ApiResponse(responseCode = "403", description = "권한 없음 - 본인 알림만 처리 가능"),
        @ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId,
            @Parameter(description = "알림 ID", example = "1") @PathVariable Long notificationId) {
        
        notificationService.markNotificationAsRead(userId, notificationId);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "모든 알림 읽음 처리",
        description = """
            사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.
            
            **처리 내용:**
            - 모든 isRead=false 알림을 true로 변경
            - readAt 시간을 현재 시간으로 설정
            - 처리된 알림 개수 반환
            
            **활용 시점:**
            - "모두 읽음" 버튼 클릭시
            - 알림 센터 전체 클리어
            - 앱 배지 카운터 초기화
            
            **성능 고려:**
            - 대량 업데이트를 위한 벌크 연산 사용
            - 트랜잭션 처리로 일관성 보장
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200", 
            description = "모든 알림 읽음 처리 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "처리된 알림 개수",
                    value = """
                    {
                      "updatedCount": 5
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllNotificationsAsRead(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {
        
        int updatedCount = notificationService.markAllNotificationsAsRead(userId);
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }

    @Operation(
        summary = "모든 알림 끄기",
        description = """
            사용자의 모든 알림을 비활성화합니다.
            
            **처리 내용:**
            - 모든 알림 타입을 false로 설정
            - FCM 토큰은 유지 (추후 알림 재활성화 가능)
            - 즉시 적용 (새로운 알림 발송 중단)
            
            **주의사항:**
            - 중요한 거래 관련 알림도 함께 비활성화됨
            - 사용자가 중요한 정보를 놓칠 수 있음
            - 되돌리기 위해서는 개별 설정 필요
            
            **권장 사용:**
            - 임시 알림 중단이 필요한 경우
            - 사용자가 명시적으로 요청한 경우
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "모든 알림 비활성화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/disable-all")
    public ResponseEntity<NotificationSettingResponse> disableAllNotifications(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {
        
        NotificationSettingResponse response = notificationService.disableAllNotifications(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "필수 알림만 켜기",
        description = """
            중요한 알림만 활성화하고 나머지는 비활성화합니다.

            **활성화되는 알림:**
            - 새로운 입찰 알림 (판매자용)
            - 낙찰 알림 (구매자용)
            - 연결 서비스 결제 요청 (판매자용)
            - 채팅 활성화 알림 (구매자용)
            - 거래 완료 알림

            **비활성화되는 알림:**
            - 새 메시지 알림 (채팅은 선택사항)
            - 프로모션 알림

            **권장 사용:**
            - 알림이 너무 많다고 느끼는 사용자
            - 중요한 알림만 받고 싶은 사용자
            - 알림 설정 초기화시
            """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "필수 알림 활성화 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/essential-only")
    public ResponseEntity<NotificationSettingResponse> enableEssentialNotificationsOnly(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {

        NotificationSettingResponse response = notificationService.enableEssentialNotificationsOnly(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "모든 알림 삭제",
        description = """
            사용자의 모든 알림 히스토리를 삭제합니다.

            **처리 내용:**
            - 사용자의 모든 알림 히스토리를 DB에서 완전 삭제
            - 삭제된 알림 개수 반환
            - 복구 불가능 (영구 삭제)

            **활용 시점:**
            - "알림 전체 삭제" 버튼 클릭시
            - 알림 히스토리 초기화
            - 테스트 데이터 정리

            **주의사항:**
            - 삭제된 알림은 복구할 수 없음
            - 읽음/안읽음 상태 관계없이 모두 삭제
            - 트랜잭션 처리로 일관성 보장
            """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "모든 알림 삭제 성공",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(
                    name = "삭제된 알림 개수",
                    value = """
                    {
                      "deletedCount": 15
                    }
                    """
                )
            )
        ),
        @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Integer>> deleteAllNotifications(
            @Parameter(description = "사용자 ID", example = "1") @RequestHeader("User-Id") Long userId) {

        int deletedCount = notificationService.deleteAllNotifications(userId);
        return ResponseEntity.ok(Map.of("deletedCount", deletedCount));
    }
}