package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.entity.AuctionBookmark;
import com.cherrypick.app.domain.auction.repository.AuctionBookmarkRepository;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.common.exception.BusinessException;
import com.cherrypick.app.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionBookmarkService {

    private final AuctionBookmarkRepository bookmarkRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;

    /**
     * 북마크 토글 (추가/삭제)
     */
    @Transactional
    public Map<String, Object> toggleBookmark(Long auctionId, Long userId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isBookmarked = bookmarkRepository.existsByAuctionAndUser(auction, user);

        if (isBookmarked) {
            // 북마크 제거
            bookmarkRepository.deleteByAuctionAndUser(auction, user);
            log.info("북마크 제거: 사용자 ID = {}, 경매 ID = {}", userId, auctionId);
        } else {
            // 북마크 추가
            AuctionBookmark bookmark = AuctionBookmark.create(auction, user);
            bookmarkRepository.save(bookmark);
            log.info("북마크 추가: 사용자 ID = {}, 경매 ID = {}", userId, auctionId);
        }

        // 현재 상태 반환
        boolean currentStatus = !isBookmarked;
        long bookmarkCount = bookmarkRepository.countByAuction(auction);

        return Map.of(
                "isBookmarked", currentStatus,
                "bookmarkCount", bookmarkCount
        );
    }

    /**
     * 특정 경매의 북마크 수 조회
     */
    public long getBookmarkCount(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        return bookmarkRepository.countByAuction(auction);
    }

    /**
     * 사용자의 특정 경매 북마크 상태 조회
     */
    public boolean isBookmarked(Long auctionId, Long userId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return bookmarkRepository.existsByAuctionAndUser(auction, user);
    }

    /**
     * 사용자의 북마크된 경매 목록 조회
     */
    public List<Auction> getUserBookmarks(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return bookmarkRepository.findBookmarkedAuctionsByUser(user);
    }

    /**
     * 북마크 상태와 수를 함께 조회
     */
    public Map<String, Object> getBookmarkInfo(Long auctionId, Long userId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isBookmarked = bookmarkRepository.existsByAuctionAndUser(auction, user);
        long bookmarkCount = bookmarkRepository.countByAuction(auction);

        return Map.of(
                "isBookmarked", isBookmarked,
                "bookmarkCount", bookmarkCount
        );
    }
}