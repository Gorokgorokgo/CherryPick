package com.cherrypick.app.domain.bid.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.AuctionStatus;
import com.cherrypick.app.domain.user.entity.User;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class BidValidationService {

    /**
     * 100원 단위 검증
     */
    public void validate100Unit(BigDecimal bidAmount) {
        if (bidAmount.remainder(new BigDecimal("100")).compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalArgumentException("입찰가는 100원 단위로 입력해주세요");
        }
    }

    /**
     * 최소 입찰 증가폭 계산
     */
    public BigDecimal calculateMinimumIncrement(BigDecimal currentPrice) {
        if (currentPrice.compareTo(new BigDecimal("10000")) < 0) {
            return new BigDecimal("500");
        } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            return new BigDecimal("1000");
        } else if (currentPrice.compareTo(new BigDecimal("10000000")) < 0) {
            return new BigDecimal("5000");
        } else {
            return new BigDecimal("10000");
        }
    }

    /**
     * 최소 입찰가 검증
     */
    public void validateMinimumBid(BigDecimal currentPrice, BigDecimal bidAmount) {
        BigDecimal minimumBid = currentPrice.add(calculateMinimumIncrement(currentPrice));
        if (bidAmount.compareTo(minimumBid) < 0) {
            throw new IllegalArgumentException(
                    String.format("최소 %s원 이상 입찰해야 합니다", minimumBid)
            );
        }
    }

    /**
     * 첫 입찰 검증 (시작가 이상)
     */
    public void validateFirstBid(BigDecimal startPrice, BigDecimal bidAmount) {
        if (bidAmount.compareTo(startPrice) < 0) {
            throw new IllegalArgumentException(
                    String.format("시작가(%s원) 이상 입찰해야 합니다", startPrice)
            );
        }
    }

    /**
     * 최대 입찰 한도 계산
     */
    public BigDecimal calculateMaximumBidLimit(BigDecimal currentPrice) {
        if (currentPrice.compareTo(new BigDecimal("10000")) < 0) {
            return new BigDecimal("50000"); // 5만원 고정
        } else if (currentPrice.compareTo(new BigDecimal("100000")) < 0) {
            return currentPrice.multiply(new BigDecimal("5")); // 5배
        } else if (currentPrice.compareTo(new BigDecimal("1000000")) < 0) {
            return currentPrice.multiply(new BigDecimal("4")); // 4배
        } else if (currentPrice.compareTo(new BigDecimal("10000000")) < 0) {
            return currentPrice.multiply(new BigDecimal("3")); // 3배
        } else {
            return currentPrice.multiply(new BigDecimal("2")); // 2배
        }
    }

    /**
     * 최대 입찰 한도 검증
     */
    public void validateMaximumBidLimit(BigDecimal currentPrice, BigDecimal bidAmount) {
        BigDecimal maximumBid = calculateMaximumBidLimit(currentPrice);
        if (bidAmount.compareTo(maximumBid) > 0) {
            throw new IllegalArgumentException(
                    String.format("최대 입찰 한도를 초과했습니다 (한도: %s원)", maximumBid)
            );
        }
    }

    /**
     * 경매 상태 검증
     */
    public void validateAuctionStatus(Auction auction) {
        if (auction.getStatus() != AuctionStatus.ACTIVE) {
            throw new IllegalArgumentException("진행 중인 경매가 아닙니다");
        }

        if (LocalDateTime.now().isAfter(auction.getEndAt())) {
            throw new IllegalArgumentException("경매가 종료되었습니다");
        }
    }

    /**
     * 본인 경매 입찰 차단
     */
    public void validateNotSelfBid(Auction auction, User bidder) {
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new IllegalArgumentException("자신의 경매에는 입찰할 수 없습니다");
        }
    }

    /**
     * 전체 입찰 검증 (첫 입찰)
     */
    public void validateBid(Auction auction, User bidder, BigDecimal bidAmount, boolean isFirstBid) {
        // 1. 100원 단위 검증
        validate100Unit(bidAmount);

        // 2. 경매 유효성 검증
        validateAuctionStatus(auction);

        // 3. 본인 경매 입찰 차단
        validateNotSelfBid(auction, bidder);

        // 4. 최소/최대 금액 검증
        if (isFirstBid) {
            validateFirstBid(auction.getStartPrice(), bidAmount);
        } else {
            validateMinimumBid(auction.getCurrentPrice(), bidAmount);
        }

        validateMaximumBidLimit(auction.getCurrentPrice(), bidAmount);
    }
}