package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 거리 제한 기능 디버그 테스트
 * regionRadiusKm이 제대로 저장되고 있는지 확인
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.show-sql=true",
    "logging.level.org.hibernate.SQL=DEBUG",
    "logging.level.org.hibernate.type.descriptor.sql.BasicBinder=TRACE"
})
class AuctionRegionRadiusDebugTest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("regionRadiusKm이 DB에 제대로 저장되는지 확인")
    void testRegionRadiusKmPersistence() {
        // Given: 판매자 생성
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000);
        User seller = User.builder()
            .phoneNumber("010" + timestamp.substring(0, 8))
            .nickname("판매자_" + timestamp)
            .email("seller_" + timestamp + "@test.com")
            .password("password")
            .latitude(37.4979)
            .longitude(127.0276)
            .build();
        seller = userRepository.save(seller);

        // When: 5km 제한 경매 생성
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setTitle("디버그 테스트 경매");
        request.setDescription("regionRadiusKm 저장 확인용");
        request.setCategory(Category.ELECTRONICS);
        request.setStartPrice(new BigDecimal("100000"));
        request.setHopePrice(new BigDecimal("800000"));
        request.setAuctionTimeHours(24);
        request.setRegionScope(RegionScope.NATIONWIDE);
        request.setImageUrls(List.of("https://example.com/image1.jpg"));
        request.setProductCondition(8);
        request.setRegionRadiusKm(5); // *** 5km 제한 설정 ***

        AuctionResponse response = auctionService.createAuction(seller.getId(), request);

        // Then: DB에서 직접 조회하여 확인
        Auction savedAuction = auctionRepository.findById(response.getId()).orElseThrow();

        assertThat(savedAuction.getRegionRadiusKm()).isNotNull();
        assertThat(savedAuction.getRegionRadiusKm()).isEqualTo(5);
        assertThat(savedAuction.getLatitude()).isNotNull();
        assertThat(savedAuction.getLongitude()).isNotNull();
    }

    @Test
    @DisplayName("여러 거리 제한 값이 제대로 저장되는지 확인")
    void testMultipleRegionRadiusValues() {
        // Given: 판매자 생성
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000);
        User seller = User.builder()
            .phoneNumber("010" + timestamp.substring(0, 8))
            .nickname("판매자_" + timestamp)
            .email("seller_" + timestamp + "@test.com")
            .password("password")
            .latitude(37.4979)
            .longitude(127.0276)
            .build();
        seller = userRepository.save(seller);

        // When: 3km, 10km, null 제한 경매 생성
        Integer[] radiusValues = {3, 10, null};
        for (Integer radius : radiusValues) {
            CreateAuctionRequest request = new CreateAuctionRequest();
            request.setTitle("테스트 경매 " + radius + "km");
            request.setDescription("테스트");
            request.setCategory(Category.ELECTRONICS);
            request.setStartPrice(new BigDecimal("100000"));
            request.setHopePrice(new BigDecimal("800000"));
            request.setAuctionTimeHours(24);
            request.setRegionScope(RegionScope.NATIONWIDE);
            request.setImageUrls(List.of("https://example.com/image1.jpg"));
            request.setProductCondition(8);
            request.setRegionRadiusKm(radius);

            AuctionResponse response = auctionService.createAuction(seller.getId(), request);
            Auction savedAuction = auctionRepository.findById(response.getId()).orElseThrow();

            if (radius == null) {
                assertThat(savedAuction.getRegionRadiusKm()).isNull();
            } else {
                assertThat(savedAuction.getRegionRadiusKm()).isEqualTo(radius);
            }
        }
    }
}
