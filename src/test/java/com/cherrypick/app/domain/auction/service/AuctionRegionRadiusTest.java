package com.cherrypick.app.domain.auction.service;

import com.cherrypick.app.domain.auction.dto.AuctionResponse;
import com.cherrypick.app.domain.auction.dto.AuctionSearchRequest;
import com.cherrypick.app.domain.auction.dto.CreateAuctionRequest;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.location.service.LocationService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 경매 거리 제한 기능 테스트
 *
 * 판매자가 경매 등록 시 설정한 regionRadiusKm 값에 따라
 * 일반 사용자가 조회할 때 거리 제한이 제대로 적용되는지 검증
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = {
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=WARN"
})
class AuctionRegionRadiusTest {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LocationService locationService;

    @Autowired
    private AuctionRepository auctionRepository;

    private User seller;
    private User nearbyUser;
    private User farUser;

    // 서울 강남역 좌표 (판매자 위치)
    private static final double SELLER_LATITUDE = 37.4979;
    private static final double SELLER_LONGITUDE = 127.0276;

    // 서울 홍대입구역 좌표 (약 12km 떨어진 위치)
    private static final double NEARBY_USER_LATITUDE = 37.5563;
    private static final double NEARBY_USER_LONGITUDE = 126.9236;

    // 인천 부평역 좌표 (약 30km 떨어진 위치)
    private static final double FAR_USER_LATITUDE = 37.4891;
    private static final double FAR_USER_LONGITUDE = 126.7235;

    @BeforeEach
    void setUp() {
        String timestamp = String.valueOf(System.currentTimeMillis() % 100000000);

        // 판매자 생성 (강남역 근처)
        seller = User.builder()
            .phoneNumber("010" + timestamp.substring(0, 8))
            .nickname("판매자_" + timestamp)
            .email("seller_" + timestamp + "@test.com")
            .password("password")
            .latitude(SELLER_LATITUDE)
            .longitude(SELLER_LONGITUDE)
            .build();
        seller = userRepository.save(seller);

        // 가까운 사용자 생성 (홍대 근처, 약 12km)
        nearbyUser = User.builder()
            .phoneNumber("010" + (Long.parseLong(timestamp) + 1))
            .nickname("가까운사용자_" + timestamp)
            .email("nearby_" + timestamp + "@test.com")
            .password("password")
            .latitude(NEARBY_USER_LATITUDE)
            .longitude(NEARBY_USER_LONGITUDE)
            .build();
        nearbyUser = userRepository.save(nearbyUser);

        // 먼 사용자 생성 (인천 부평 근처, 약 30km)
        farUser = User.builder()
            .phoneNumber("010" + (Long.parseLong(timestamp) + 2))
            .nickname("먼사용자_" + timestamp)
            .email("far_" + timestamp + "@test.com")
            .password("password")
            .latitude(FAR_USER_LATITUDE)
            .longitude(FAR_USER_LONGITUDE)
            .build();
        farUser = userRepository.save(farUser);

    }

    @Test
    @DisplayName("거리 제한 3km - 가까운 사용자도 조회 불가")
    void regionRadius_3km_nearbyUserCannotSee() {
        // Given: 3km 제한 경매 생성
        CreateAuctionRequest request = createAuctionRequest(3);
        AuctionResponse createdAuction = auctionService.createAuction(seller.getId(), request);

        // When: 가까운 사용자(약 12km)가 주변 경매 검색
        AuctionSearchRequest searchRequest = new AuctionSearchRequest();
        searchRequest.setLatitude(NEARBY_USER_LATITUDE);
        searchRequest.setLongitude(NEARBY_USER_LONGITUDE);
        searchRequest.setMaxDistanceKm(50.0); // 충분히 넓은 범위로 검색

        Page<AuctionResponse> results = auctionService.searchNearbyAuctions(
            searchRequest,
            PageRequest.of(0, 10),
            nearbyUser.getId()
        );

        // Then: 생성한 경매가 조회되지 않아야 함
        assertThat(results.getContent())
            .extracting("id")
            .doesNotContain(createdAuction.getId());
    }

    @Test
    @DisplayName("거리 제한 5km - 가까운 사용자도 조회 불가")
    void regionRadius_5km_nearbyUserCannotSee() {
        // Given: 5km 제한 경매 생성
        CreateAuctionRequest request = createAuctionRequest(5);
        AuctionResponse createdAuction = auctionService.createAuction(seller.getId(), request);

        // When: 가까운 사용자(약 12km)가 주변 경매 검색
        AuctionSearchRequest searchRequest = new AuctionSearchRequest();
        searchRequest.setLatitude(NEARBY_USER_LATITUDE);
        searchRequest.setLongitude(NEARBY_USER_LONGITUDE);
        searchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> results = auctionService.searchNearbyAuctions(
            searchRequest,
            PageRequest.of(0, 10),
            nearbyUser.getId()
        );

        // Then: 생성한 경매가 조회되지 않아야 함
        assertThat(results.getContent())
            .extracting("id")
            .doesNotContain(createdAuction.getId());
    }

    @Test
    @DisplayName("거리 제한 10km - 가까운 사용자도 조회 불가")
    void regionRadius_10km_nearbyUserCannotSee() {
        // Given: 10km 제한 경매 생성
        CreateAuctionRequest request = createAuctionRequest(10);
        AuctionResponse createdAuction = auctionService.createAuction(seller.getId(), request);

        // When: 가까운 사용자(약 12km)가 주변 경매 검색
        AuctionSearchRequest searchRequest = new AuctionSearchRequest();
        searchRequest.setLatitude(NEARBY_USER_LATITUDE);
        searchRequest.setLongitude(NEARBY_USER_LONGITUDE);
        searchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> results = auctionService.searchNearbyAuctions(
            searchRequest,
            PageRequest.of(0, 10),
            nearbyUser.getId()
        );

        // Then: 생성한 경매가 조회되지 않아야 함
        assertThat(results.getContent())
            .extracting("id")
            .doesNotContain(createdAuction.getId());
    }

    @Test
    @DisplayName("거리 제한 20km - 가까운 사용자는 조회 가능, 먼 사용자는 불가")
    void regionRadius_20km_nearbyUserCanSee_farUserCannot() {
        // Given: 20km 제한 경매 생성
        CreateAuctionRequest request = createAuctionRequest(20);
        AuctionResponse createdAuction = auctionService.createAuction(seller.getId(), request);

        // When 1: 가까운 사용자(약 12km)가 주변 경매 검색
        AuctionSearchRequest nearbySearchRequest = new AuctionSearchRequest();
        nearbySearchRequest.setLatitude(NEARBY_USER_LATITUDE);
        nearbySearchRequest.setLongitude(NEARBY_USER_LONGITUDE);
        nearbySearchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> nearbyResults = auctionService.searchNearbyAuctions(
            nearbySearchRequest,
            PageRequest.of(0, 10),
            nearbyUser.getId()
        );

        // Then 1: 가까운 사용자는 경매 조회 가능
        assertThat(nearbyResults.getContent()).isNotEmpty();
        assertThat(nearbyResults.getContent().get(0).getId()).isEqualTo(createdAuction.getId());

        // When 2: 먼 사용자(약 30km)가 주변 경매 검색
        AuctionSearchRequest farSearchRequest = new AuctionSearchRequest();
        farSearchRequest.setLatitude(FAR_USER_LATITUDE);
        farSearchRequest.setLongitude(FAR_USER_LONGITUDE);
        farSearchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> farResults = auctionService.searchNearbyAuctions(
            farSearchRequest,
            PageRequest.of(0, 10),
            farUser.getId()
        );

        // Then 2: 먼 사용자는 경매 조회 불가
        assertThat(farResults.getContent())
            .extracting("id")
            .doesNotContain(createdAuction.getId());
    }

    @Test
    @DisplayName("거리 제한 null - 모든 사용자가 조회 가능 (제한 없음)")
    void regionRadius_null_allUsersCanSee() {
        // Given: 거리 제한 없는 경매 생성
        CreateAuctionRequest request = createAuctionRequest(null);
        AuctionResponse createdAuction = auctionService.createAuction(seller.getId(), request);

        // When 1: 가까운 사용자가 검색
        AuctionSearchRequest nearbySearchRequest = new AuctionSearchRequest();
        nearbySearchRequest.setLatitude(NEARBY_USER_LATITUDE);
        nearbySearchRequest.setLongitude(NEARBY_USER_LONGITUDE);
        nearbySearchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> nearbyResults = auctionService.searchNearbyAuctions(
            nearbySearchRequest,
            PageRequest.of(0, 10),
            nearbyUser.getId()
        );

        // Then 1: 가까운 사용자 조회 가능
        assertThat(nearbyResults.getContent())
            .extracting("id")
            .contains(createdAuction.getId());

        // When 2: 먼 사용자가 검색
        AuctionSearchRequest farSearchRequest = new AuctionSearchRequest();
        farSearchRequest.setLatitude(FAR_USER_LATITUDE);
        farSearchRequest.setLongitude(FAR_USER_LONGITUDE);
        farSearchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> farResults = auctionService.searchNearbyAuctions(
            farSearchRequest,
            PageRequest.of(0, 10),
            farUser.getId()
        );

        // Then 2: 먼 사용자도 조회 가능
        assertThat(farResults.getContent())
            .extracting("id")
            .contains(createdAuction.getId());
    }

    @Test
    @DisplayName("여러 경매 동시 테스트 - 각기 다른 거리 제한")
    void multipleAuctions_differentRadiusLimits() {
        // Given: 다양한 거리 제한을 가진 경매 3개 생성
        AuctionResponse auction3km = auctionService.createAuction(seller.getId(), createAuctionRequest(3));
        AuctionResponse auction10km = auctionService.createAuction(seller.getId(), createAuctionRequest(10));
        AuctionResponse auction20km = auctionService.createAuction(seller.getId(), createAuctionRequest(20));
        AuctionResponse auctionNoLimit = auctionService.createAuction(seller.getId(), createAuctionRequest(null));

        // When: 가까운 사용자(약 12km)가 검색
        AuctionSearchRequest searchRequest = new AuctionSearchRequest();
        searchRequest.setLatitude(NEARBY_USER_LATITUDE);
        searchRequest.setLongitude(NEARBY_USER_LONGITUDE);
        searchRequest.setMaxDistanceKm(50.0);

        Page<AuctionResponse> results = auctionService.searchNearbyAuctions(
            searchRequest,
            PageRequest.of(0, 10),
            nearbyUser.getId()
        );

        // Then: 20km 제한 경매와 제한 없는 경매만 조회되어야 함
        List<Long> resultIds = results.getContent().stream()
            .map(AuctionResponse::getId)
            .toList();

        assertThat(resultIds).doesNotContain(auction3km.getId());
        assertThat(resultIds).doesNotContain(auction10km.getId());
        assertThat(resultIds).contains(auction20km.getId());
        assertThat(resultIds).contains(auctionNoLimit.getId());
    }

    @Test
    @DisplayName("실제 거리 계산 검증")
    void verifyDistanceCalculation() {
        // Given: LocationService를 통한 거리 계산
        double distanceToNearby = locationService.calculateDistance(
            SELLER_LATITUDE, SELLER_LONGITUDE,
            NEARBY_USER_LATITUDE, NEARBY_USER_LONGITUDE
        );

        double distanceToFar = locationService.calculateDistance(
            SELLER_LATITUDE, SELLER_LONGITUDE,
            FAR_USER_LATITUDE, FAR_USER_LONGITUDE
        );

        // Then: 거리 검증
        assertThat(distanceToNearby).isBetween(10.0, 15.0); // 약 12km 예상
        assertThat(distanceToFar).isBetween(25.0, 35.0);    // 약 30km 예상
    }

    /**
     * 경매 등록 요청 생성 헬퍼 메서드
     */
    private CreateAuctionRequest createAuctionRequest(Integer regionRadiusKm) {
        CreateAuctionRequest request = new CreateAuctionRequest();
        request.setTitle("아이폰 15 Pro 256GB");
        request.setDescription("테스트 경매 상품입니다.");
        request.setCategory(Category.ELECTRONICS);
        request.setStartPrice(new BigDecimal("100000"));
        request.setHopePrice(new BigDecimal("800000"));
        request.setAuctionTimeHours(24);
        request.setRegionScope(RegionScope.NATIONWIDE);
        request.setImageUrls(List.of("https://example.com/image1.jpg"));
        request.setProductCondition(8);
        request.setRegionRadiusKm(regionRadiusKm); // 거리 제한 설정
        return request;
    }
}
