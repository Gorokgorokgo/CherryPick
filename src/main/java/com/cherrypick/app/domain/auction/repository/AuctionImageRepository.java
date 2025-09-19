package com.cherrypick.app.domain.auction.repository;

import com.cherrypick.app.domain.auction.entity.AuctionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {

    List<AuctionImage> findByAuctionIdOrderBySortOrder(Long auctionId);

    // N+1 문제 해결을 위한 벌크 조회 메서드
    @Query("SELECT ai FROM AuctionImage ai WHERE ai.auction.id IN :auctionIds ORDER BY ai.auction.id, ai.sortOrder")
    List<AuctionImage> findByAuctionIdInOrderByAuctionIdAndSortOrder(@Param("auctionIds") List<Long> auctionIds);

    void deleteByAuctionId(Long auctionId);
}