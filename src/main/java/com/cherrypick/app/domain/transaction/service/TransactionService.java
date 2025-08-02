package com.cherrypick.app.domain.transaction.service;

import com.cherrypick.app.config.BusinessConfig;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.bid.entity.Bid;
import com.cherrypick.app.domain.transaction.entity.Transaction;
import com.cherrypick.app.domain.transaction.enums.TransactionStatus;
import com.cherrypick.app.domain.transaction.repository.TransactionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.service.ExperienceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final BusinessConfig businessConfig;
    private final ExperienceService experienceService;

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
}