package com.cherrypick.app.domain.transaction.service;

import com.cherrypick.app.config.BusinessConfig;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.bid.repository.BidRepository;
import com.cherrypick.app.domain.transaction.dto.response.TransactionConfirmResponse;
import com.cherrypick.app.domain.transaction.dto.response.TransactionResponse;
import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import com.cherrypick.app.domain.transaction.repository.TransactionRepository;
import com.cherrypick.app.domain.transaction.repository.ReviewRepository;
import com.cherrypick.app.domain.notification.service.NotificationEventPublisher;
import com.cherrypick.app.domain.user.dto.response.ExperienceGainResponse;
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
import java.util.Optional;

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
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    /**
     * 경매 종료 후 거래 생성
     * 
     * @param auction 종료된 경매
     * @param winningBid 낙찰 입찰
     * @return 생성된 거래
     */
    @Transactional
    public Transaction createTransactionFromAuction(Auction auction, Bid winningBid) {
        // 중복 생성 방지: 이미 Transaction이 존재하면 기존 것을 반환
        Optional<Transaction> existingTransaction = transactionRepository.findByAuctionId(auction.getId());
        if (existingTransaction.isPresent()) {
            log.info("경매 {} Transaction이 이미 존재합니다. 기존 Transaction 반환", auction.getId());
            return existingTransaction.get();
        }

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
     * 거래 완료 결과 (거래 + 경험치 정보)
     */
    public static class TransactionCompletionResult {
        private final Transaction transaction;
        private final ExperienceService.TransactionExperienceResult experienceResult;

        public TransactionCompletionResult(Transaction transaction, ExperienceService.TransactionExperienceResult experienceResult) {
            this.transaction = transaction;
            this.experienceResult = experienceResult;
        }

        public Transaction getTransaction() {
            return transaction;
        }

        public ExperienceService.TransactionExperienceResult getExperienceResult() {
            return experienceResult;
        }
    }

    /**
     * 거래 완료 처리 (양방향 확인 완료 시)
     *
     * @param transactionId 거래 ID
     * @return 완료된 거래 및 경험치 정보
     */
    @Transactional
    public TransactionCompletionResult completeTransaction(Long transactionId) {
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

        Transaction savedTransaction = transactionRepository.save(transaction);

        // 경험치 지급 (구매자/판매자) 및 결과 수집
        ExperienceService.TransactionExperienceResult experienceResult = null;
        try {
            log.info("💎 경험치 지급 시작 - 거래 ID: {}, 구매자 ID: {}, 판매자 ID: {}, 금액: {}",
                transactionId, transaction.getBuyer().getId(), transaction.getSeller().getId(), transaction.getFinalPrice());

            experienceResult = experienceService.awardTransactionExperience(
                transaction.getBuyer().getId(),
                transaction.getSeller().getId(),
                transaction.getFinalPrice(),
                completedAt,
                transaction.getAuction()
            );

            if (experienceResult != null) {
                log.info("✅ 거래 완료 경험치 지급 완료 - 거래 ID: {}, 구매자 EXP: {}, 판매자 EXP: {}",
                    transactionId,
                    experienceResult.getBuyerExperience() != null ? experienceResult.getBuyerExperience().getExpGained() : "null",
                    experienceResult.getSellerExperience() != null ? experienceResult.getSellerExperience().getExpGained() : "null");
            } else {
                log.warn("⚠️ 경험치 지급 결과가 null - 거래 ID: {}", transactionId);
            }
        } catch (Exception e) {
            log.error("❌ 경험치 지급 중 오류 발생 - 거래 ID: {}, 오류: {}", transactionId, e.getMessage(), e);
            // 경험치 지급 실패가 거래 완료를 막지 않도록 예외를 잡음
        }

        return new TransactionCompletionResult(savedTransaction, experienceResult);
    }

    /**
     * 판매자 거래 확인
     */
    @Transactional
    public TransactionCompletionResult confirmBySeller(Long transactionId, Long sellerId) {
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

        Transaction saved = transactionRepository.save(transaction);
        return new TransactionCompletionResult(saved, null);
    }

    /**
     * 구매자 거래 확인
     */
    @Transactional
    public TransactionCompletionResult confirmByBuyer(Long transactionId, Long buyerId) {
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

        Transaction saved = transactionRepository.save(transaction);
        return new TransactionCompletionResult(saved, null);
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
                .orElseThrow(() -> new IllegalArgumentException("거래 정보를 찾을 수 없습니다."));

        return TransactionResponse.from(transaction);
    }

    /**
     * 경매 ID로 거래 조회 또는 생성 (유찰 경매 직거래용)
     *
     * @param auctionId 경매 ID
     * @return 거래
     */
    @Transactional
    public Transaction getOrCreateTransactionByAuction(Long auctionId) {
        // 먼저 조회 시도
        return transactionRepository.findByAuctionId(auctionId)
                .orElseGet(() -> {
                    try {
                        // Transaction이 없으면 자동 생성 (유찰 경매 직거래)
                        log.info("경매 {}에 Transaction이 없음 - 자동 생성 시작", auctionId);

                        Auction auction = auctionRepository.findById(auctionId)
                                .orElseThrow(() -> new IllegalArgumentException("경매를 찾을 수 없습니다."));

                        // 최고 입찰 조회
                        Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)
                                .orElseThrow(() -> new IllegalArgumentException("입찰 내역이 없습니다."));

                        // Transaction 생성
                        Transaction transaction = createTransactionFromAuction(auction, highestBid);

                        log.info("경매 {} Transaction 자동 생성 완료 - 판매자: {}, 구매자: {}, 금액: {}",
                                auctionId,
                                transaction.getSeller().getId(),
                                transaction.getBuyer().getId(),
                                transaction.getFinalPrice());

                        return transaction;
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // 동시성 문제로 이미 생성된 경우 다시 조회
                        log.warn("⚠️ 경매 {} Transaction 중복 생성 시도 - 다시 조회", auctionId);
                        return transactionRepository.findByAuctionId(auctionId)
                                .orElseThrow(() -> new IllegalStateException("Transaction 조회/생성 실패"));
                    }
                });
    }

    /**
     * 경매 ID로 거래 확인 (유찰 경매 직거래용)
     *
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 거래 확인 응답
     */
    @Transactional
    public TransactionConfirmResponse confirmTransactionByAuction(Long auctionId, Long userId) {
        // Transaction 조회 또는 생성 (유찰 경매는 자동 생성)
        Transaction transaction = getOrCreateTransactionByAuction(auctionId);

        // 기존 confirmTransaction 로직 호출
        return confirmTransaction(transaction.getId(), userId);
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
            // 후기 작성 여부 확인
            boolean hasReview = reviewRepository.existsByTransactionIdAndReviewerId(transactionId, userId);

            return TransactionConfirmResponse.of(
                    transactionId,
                    TransactionStatus.COMPLETED,
                    transaction.getSellerConfirmed(),
                    transaction.getBuyerConfirmed(),
                    transaction.getCompletedAt(),
                    "이미 완료된 거래입니다.",
                    !hasReview  // 후기를 작성하지 않았으면 true
            );
        }

        // 취소된 거래인지 확인
        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new IllegalArgumentException("취소된 거래입니다. 거래를 완료할 수 없습니다.");
        }

        // 본인이 이미 확인했는지 체크
        if (isSeller && transaction.getSellerConfirmed()) {
            return TransactionConfirmResponse.of(
                    transactionId,
                    transaction.getStatus(),
                    true,
                    transaction.getBuyerConfirmed(),
                    null,
                    "이미 확인한 거래입니다. 상대방의 확인을 기다리는 중입니다.",
                    false  // 거래가 완료되지 않았으므로 후기 작성 불가
            );
        }

        if (isBuyer && transaction.getBuyerConfirmed()) {
            return TransactionConfirmResponse.of(
                    transactionId,
                    transaction.getStatus(),
                    transaction.getSellerConfirmed(),
                    true,
                    null,
                    "이미 확인한 거래입니다. 상대방의 확인을 기다리는 중입니다.",
                    false  // 거래가 완료되지 않았으므로 후기 작성 불가
            );
        }

        // 확인 처리
        TransactionCompletionResult completionResult;
        if (isSeller) {
            completionResult = confirmBySeller(transactionId, userId);
        } else {
            completionResult = confirmByBuyer(transactionId, userId);
        }

        Transaction confirmedTransaction = completionResult.getTransaction();
        ExperienceService.TransactionExperienceResult experienceResult = completionResult.getExperienceResult();

        log.info("📋 confirmTransaction - 거래 ID: {}, 상태: {}, experienceResult: {}",
            transactionId, confirmedTransaction.getStatus(), experienceResult != null ? "존재" : "null");

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
        boolean canWriteReview = false;
        if (confirmedTransaction.getStatus() == TransactionStatus.COMPLETED) {
            // 후기 작성 여부 확인
            boolean hasReview = reviewRepository.existsByTransactionIdAndReviewerId(transactionId, userId);
            canWriteReview = !hasReview;  // 후기를 작성하지 않았으면 true

            message = "거래가 완료되었습니다! 경험치가 지급되었습니다. 후기를 작성해주세요.";

            // 경험치 정보 포함 (본인의 경험치만 반환)
            ExperienceGainResponse buyerExp = null;
            ExperienceGainResponse sellerExp = null;

            if (experienceResult != null) {
                buyerExp = experienceResult.getBuyerExperience();
                sellerExp = experienceResult.getSellerExperience();
            }

            return TransactionConfirmResponse.ofWithExperience(
                    transactionId,
                    confirmedTransaction.getStatus(),
                    confirmedTransaction.getSellerConfirmed(),
                    confirmedTransaction.getBuyerConfirmed(),
                    confirmedTransaction.getCompletedAt(),
                    message,
                    canWriteReview,
                    buyerExp,
                    sellerExp
            );
        } else {
            message = "거래 확인이 완료되었습니다. 상대방의 확인을 기다리는 중입니다.";
        }

        return TransactionConfirmResponse.of(
                transactionId,
                confirmedTransaction.getStatus(),
                confirmedTransaction.getSellerConfirmed(),
                confirmedTransaction.getBuyerConfirmed(),
                confirmedTransaction.getCompletedAt(),
                message,
                canWriteReview
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

    /**
     * 거래 취소
     * - 거래가 완료되지 않은 상태(PENDING, SELLER_CONFIRMED, BUYER_CONFIRMED)에서만 취소 가능
     * - 판매자 또는 구매자 모두 취소 가능
     *
     * @param transactionId 거래 ID
     * @param userId 사용자 ID
     * @return 취소된 거래 응답
     */
    @Transactional
    public TransactionResponse cancelTransaction(Long transactionId, Long userId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        // 권한 확인
        boolean isSeller = transaction.getSeller().getId().equals(userId);
        boolean isBuyer = transaction.getBuyer().getId().equals(userId);

        if (!isSeller && !isBuyer) {
            throw new IllegalArgumentException("거래 당사자만 취소할 수 있습니다.");
        }

        // 이미 완료된 거래는 취소 불가
        if (transaction.getStatus() == TransactionStatus.COMPLETED) {
            throw new IllegalArgumentException("이미 완료된 거래는 취소할 수 없습니다.");
        }

        // 이미 취소된 거래
        if (transaction.getStatus() == TransactionStatus.CANCELLED) {
            throw new IllegalArgumentException("이미 취소된 거래입니다.");
        }

        // 거래 취소 처리
        transaction.setStatus(TransactionStatus.CANCELLED);

        Transaction savedTransaction = transactionRepository.save(transaction);

        log.info("거래 취소 완료: transactionId={}, userId={}, cancelledBy={}",
                transactionId, userId, isSeller ? "판매자" : "구매자");

        return TransactionResponse.from(savedTransaction);
    }

    /**
     * 경매 ID로 거래 취소
     *
     * @param auctionId 경매 ID
     * @param userId 사용자 ID
     * @return 취소된 거래 응답
     */
    @Transactional
    public TransactionResponse cancelTransactionByAuction(Long auctionId, Long userId) {
        Transaction transaction = transactionRepository.findByAuctionId(auctionId)
                .orElseThrow(() -> new IllegalArgumentException("거래를 찾을 수 없습니다."));

        return cancelTransaction(transaction.getId(), userId);
    }
}