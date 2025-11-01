package com.cherrypick.app.domain.transaction.service;

import com.cherrypick.app.config.BusinessConfig;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.transaction.dto.response.TransactionConfirmResponse;
import com.cherrypick.app.domain.transaction.dto.response.TransactionResponse;
import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import com.cherrypick.app.domain.transaction.repository.TransactionRepository;
import com.cherrypick.app.domain.transaction.repository.ReviewRepository;
import com.cherrypick.app.domain.notification.service.NotificationEventPublisher;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ReviewRepository reviewRepository;
    private final BusinessConfig businessConfig;
    private final ExperienceService experienceService;
    private final NotificationEventPublisher notificationEventPublisher;

    /**
     * 경매 종료 후 거래 생성
     * 
     * @param auction 종료된 경매
     * @param winningBid 낙찰 입찰
     * @return 생성된 거래
     */
    @Transactional
    public Transaction createTransactionFromAuction(Auction auction, Bid winningBid) {
        User seller = auction.getSeller();
        User buyer = winningBid.getBidder();
        BigDecimal finalPrice = winningBid.getBidAmount();

        // 판매자별 최종 수수료율 계산 (레벨 할인 + 마이너스 방지 적용)
        BigDecimal commissionRate = businessConfig.getFinalCommissionRateForSeller(
            seller.getCreatedAt().toLocalDate(),
            seller.getSellerLevel()
        );

        // 수수료 계산
        BigDecimal commissionFee = finalPrice.multiply(commissionRate);
        
        // 판매자 수령 금액 계산 (낙찰가 - 수수료)
        BigDecimal sellerAmount = finalPrice.subtract(commissionFee);

        // 거래 생성
        Transaction transaction = Transaction.builder()
                .auction(auction)
                .seller(seller)
                .buyer(buyer)
                .finalPrice(finalPrice)
                .commissionFee(commissionFee)
                .sellerAmount(sellerAmount)
                .status(TransactionStatus.PENDING)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * 거래 완료 처리 (양방향 확인 완료 시)
     * 
     * @param transactionId 거래 ID
     * @return 완료된 거래
     */
    @Transactional
    public Transaction completeTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        // 양방향 확인 완료 체크
        if (!transaction.getSellerConfirmed() || !transaction.getBuyerConfirmed()) {
            throw new IllegalArgumentException("양방향 확인이 완료되지 않았습니다.");
        }

        // 거래 완료 처리
        transaction.setStatus(TransactionStatus.COMPLETED);
        LocalDateTime completedAt = LocalDateTime.now();
        transaction.setCompletedAt(completedAt);

        // 판매자에게 수령 금액 지급
        User seller = transaction.getSeller();
        seller.setPointBalance(seller.getPointBalance() + transaction.getSellerAmount().longValue());

        // 경험치 지급 (구매자/판매자)
        try {
            experienceService.awardTransactionExperience(
                transaction.getBuyer().getId(),
                transaction.getSeller().getId(),
                transaction.getFinalPrice(),
                completedAt,
                transaction.getAuction()
            );
            log.info("거래 완료 경험치 지급 완료 - 거래 ID: {}", transactionId);
        } catch (Exception e) {
            log.error("경험치 지급 중 오류 발생 - 거래 ID: {}, 오류: {}", transactionId, e.getMessage());
            // 경험치 지급 실패가 거래 완료를 막지 않도록 예외를 잡음
        }

        return transactionRepository.save(transaction);
    }

    /**
     * 판매자 거래 확인
     */
    @Transactional
    public Transaction confirmBySeller(Long transactionId, Long sellerId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        if (!transaction.getSeller().getId().equals(sellerId)) {
            throw new IllegalArgumentException("판매자만 확인할 수 있습니다.");
        }

        transaction.setSellerConfirmed(true);
        transaction.setSellerConfirmedAt(LocalDateTime.now());

        // 양방향 확인 완료 시 자동 거래 완료
        if (transaction.getBuyerConfirmed()) {
            return completeTransaction(transactionId);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * 구매자 거래 확인
     */
    @Transactional
    public Transaction confirmByBuyer(Long transactionId, Long buyerId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        if (!transaction.getBuyer().getId().equals(buyerId)) {
            throw new IllegalArgumentException("구매자만 확인할 수 있습니다.");
        }

        transaction.setBuyerConfirmed(true);
        transaction.setBuyerConfirmedAt(LocalDateTime.now());

        // 양방향 확인 완료 시 자동 거래 완료
        if (transaction.getSellerConfirmed()) {
            return completeTransaction(transactionId);
        }

        return transactionRepository.save(transaction);
    }

    /**
     * 현재 적용 중인 수수료율 조회 (관리용)
     */
    public BigDecimal getCurrentCommissionRate() {
        return businessConfig.getCurrentCommissionRate();
    }

    /**
     * 특정 사용자의 수수료율 조회 (미리보기용)
     */
    public BigDecimal getCommissionRateForUser(User user) {
        return businessConfig.getCommissionRateForUser(user.getCreatedAt().toLocalDate());
    }

    /**
     * 경매 ID로 거래 조회
     *
     * @param auctionId 경매 ID
     * @return 거래 응답
     */
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionByAuction(Long auctionId) {
        Transaction transaction = transactionRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        return TransactionResponse.from(transaction);
    }

    /**
     * 거래 확인 (판매자/구매자 공통)
     *
     * @param transactionId 거래 ID
     * @param userId 사용자 ID
     * @return 거래 확인 응답
     */
    @Transactional
    public TransactionConfirmResponse confirmTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        // 거래 당사자 확인
        boolean isSeller = transaction.getSeller().getId().equals(userId);
        boolean isBuyer = transaction.getBuyer().getId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("거래 당사자만 확인할 수 있습니다.");
        }

        // 이미 완료된 거래인지 확인
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            return TransactionConfirmResponse.of(
                    transactionId,
                    TransactionStatus.COMPLETED,
                    transaction.getSellerConfirmed(),
                    transaction.getBuyerConfirmed(),
                    transaction.getCompletedAt(),
                    "이미 완료된 거래입니다."
            );
        }

        // 본인이 이미 확인했는지 체크
        if (isSeller && transaction.getSellerConfirmed()) {
            return TransactionConfirmResponse.of(
                    transactionId,
                    transaction.getStatus(),
                    true,
                    transaction.getBuyerConfirmed(),
                    null,
                    "이미 확인한 거래입니다. 상대방의 확인을 기다리는 중입니다."
            );
        }

        if (isBuyer && transaction.getBuyerConfirmed()) {
            return TransactionConfirmResponse.of(
                    transactionId,
                    transaction.getStatus(),
                    transaction.getSellerConfirmed(),
                    true,
                    null,
                    "이미 확인한 거래입니다. 상대방의 확인을 기다리는 중입니다."
            );
        }

        // 확인 처리
        Transaction confirmedTransaction;
        if (isSeller) {
            confirmedTransaction = confirmBySeller(transactionId, userId);
        } else {
            confirmedTransaction = confirmByBuyer(transactionId, userId);
        }

        // 상대방에게 알림 발송
        User otherUser = isSeller ? transaction.getBuyer() : transaction.getSeller();
        if (confirmedTransaction.getStatus() != TransactionStatus.COMPLETED) {
            // 단일 확인 시 상대방에게 알림
            notificationEventPublisher.publishTransactionConfirmedNotification(
                    otherUser.getId(),
                    transaction.getAuction().getTitle(),
                    isSeller ? "판매자" : "구매자"
            );
        }

        // 응답 생성
        String message;
        if (confirmedTransaction.getStatus() == TransactionStatus.COMPLETED) {
            message = "거래가 완료되었습니다! 경험치가 지급되었습니다. 후기를 작성해주세요.";
        } else {
            message = "거래 확인이 완료되었습니다. 상대방의 확인을 기다리는 중입니다.";
        }

        return TransactionConfirmResponse.of(
                transactionId,
                confirmedTransaction.getStatus(),
                confirmedTransaction.getSellerConfirmed(),
                confirmedTransaction.getBuyerConfirmed(),
                confirmedTransaction.getCompletedAt(),
                message
        );
    }

    /**
     * 내 거래 내역 조회
     *
     * @param userId 사용자 ID
     * @param status 거래 상태 필터 (선택)
     * @param pageable 페이징 정보
     * @return 거래 내역 페이지
     */
    public Page<TransactionResponse> getMyTransactions(Long userId, TransactionStatus status, Pageable pageable) {
        Page<Transaction> transactions;

        if (status != null) {
            transactions = transactionRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            transactions = transactionRepository.findByUserId(userId, pageable);
        }

        // 각 거래에 대해 후기 작성 여부 확인
        return transactions.map(transaction -> {
            boolean hasWrittenReview = reviewRepository.existsByTransactionIdAndReviewerId(
                    transaction.getId(), userId);
            return TransactionResponse.from(transaction, hasWrittenReview);
        });
    }

    /**
     * 거래 상세 조회
     *
     * @param transactionId 거래 ID
     * @param userId 사용자 ID
     * @return 거래 상세 정보
     */
    public TransactionResponse getTransactionDetail(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        // 권한 확인
        if (!transaction.getSeller().getId().equals(userId) &&
            !transaction.getBuyer().getId().equals(userId)) {
            throw new IllegalArgumentException("거래 당사자만 조회할 수 있습니다.");
        }

        return TransactionResponse.from(transaction);
    }
}