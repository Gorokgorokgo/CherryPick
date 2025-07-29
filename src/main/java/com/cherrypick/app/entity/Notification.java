package com.cherrypick.app.entity;

import com.cherrypick.app.entity.enums.NotificationType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * 알림 엔티티
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @NotBlank
    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @NotBlank
    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auction_id")
    private Auction auction;

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Builder.Default
    @Column(name = "is_sent", nullable = false)
    private Boolean isSent = false;

    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /**
     * 알림 읽음 처리
     */
    public void markAsRead() {
        this.isRead = true;
    }

    /**
     * 알림 발송 완료 처리
     */
    public void markAsSent() {
        this.isSent = true;
    }

    /**
     * 새로운 입찰 알림 생성
     */
    public static Notification createNewBidNotification(User seller, Auction auction, 
                                                        String bidderNickname, String bidAmount) {
        return Notification.builder()
                .user(seller)
                .type(NotificationType.BID_NEW)
                .title("새로운 입찰이 있어요!")
                .message(bidderNickname + "님이 " + bidAmount + "원에 입찰했어요")
                .auction(auction)
                .linkUrl("/auctions/" + auction.getId())
                .build();
    }

    /**
     * 낙찰 성공 알림 생성
     */
    public static Notification createAuctionWinNotification(User winner, Auction auction) {
        return Notification.builder()
                .user(winner)
                .type(NotificationType.AUCTION_WIN)
                .title("축하합니다! 낙찰받으셨어요 🎉")
                .message(auction.getTitle() + " 경매에서 낙찰받으셨습니다")
                .auction(auction)
                .linkUrl("/auctions/" + auction.getId())
                .build();
    }

    /**
     * 경매 종료 임박 알림 생성
     */
    public static Notification createAuctionEndingSoonNotification(User user, Auction auction, 
                                                                   int remainingMinutes) {
        return Notification.builder()
                .user(user)
                .type(NotificationType.AUCTION_END_SOON)
                .title("경매가 곧 종료됩니다 ⏰")
                .message(auction.getTitle() + " 경매가 " + remainingMinutes + "분 후 종료됩니다")
                .auction(auction)
                .linkUrl("/auctions/" + auction.getId())
                .build();
    }

    /**
     * 입찰가 초과 알림 생성
     */
    public static Notification createOutbidNotification(User bidder, Auction auction, 
                                                        String newBidAmount) {
        return Notification.builder()
                .user(bidder)
                .type(NotificationType.BID_OUTBID)
                .title("다른 분이 더 높은 가격에 입찰했어요")
                .message(auction.getTitle() + " 경매에서 " + newBidAmount + "원으로 입찰되었습니다")
                .auction(auction)
                .linkUrl("/auctions/" + auction.getId())
                .build();
    }
}