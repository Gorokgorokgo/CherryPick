package com.cherrypick.app.domain.auction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {
    
    List<AuctionImage> findByAuctionIdOrderBySortOrder(Long auctionId);
    
    void deleteByAuctionId(Long auctionId);
}