package com.cherrypick.app.domain.chat.controller;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.chat.dto.request.CreateChatRoomRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.repository.ChatRoomRepository;
import com.cherrypick.app.domain.chat.service.ChatService;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import com.cherrypick.app.domain.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("채팅방 생성 API 통합 테스트")
class ChatControllerIntegrationTest {

    @Autowired
    private ChatController chatController;

    @Autowired
    private ChatService chatService;

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserService userService;

    private User seller;
    private User buyer;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // 판매자 생성
        seller = User.builder()
                .email("seller@test.com")
                .nickname("판매자")
                .password("password")
                .phoneNumber("01012345678")
                .build();
        seller = userRepository.save(seller);

        // 구매자 생성
        buyer = User.builder()
                .email("buyer@test.com")
                .nickname("구매자")
                .password("password")
                .phoneNumber("01087654321")
                .build();
        buyer = userRepository.save(buyer);

        // 경매 생성
        auction = Auction.createAuction(
                seller,
                "테스트 경매 상품",
                "테스트 경매 상품 설명",
                Category.ELECTRONICS,
                new BigDecimal("10000"),
                new BigDecimal("50000"),
                null,
                24,
                RegionScope.NATIONWIDE,
                null,
                null
        );
        auction = auctionRepository.save(auction);
    }

    @Test
    @DisplayName("채팅방 생성 성공 - 판매자가 요청")
    void createChatRoom_Success_BySeller() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(
                auction.getId(),
                seller.getId(),
                buyer.getId()
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(seller.getEmail())
                .password(seller.getPassword())
                .authorities("ROLE_USER")
                .build();

        // when
        ResponseEntity<ChatRoomResponse> response = chatController.createChatRoom(request, userDetails);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getAuctionId()).isEqualTo(auction.getId());
        assertThat(response.getBody().getSellerId()).isEqualTo(seller.getId());
        assertThat(response.getBody().getBuyerId()).isEqualTo(buyer.getId());

        // 데이터베이스 검증
        Optional<ChatRoom> savedChatRoom = chatRoomRepository
                .findByAuctionIdAndSellerIdAndBuyerId(auction.getId(), seller.getId(), buyer.getId());
        assertThat(savedChatRoom).isPresent();
        assertThat(savedChatRoom.get().getAuction().getId()).isEqualTo(auction.getId());
    }

    @Test
    @DisplayName("채팅방 생성 성공 - 구매자가 요청")
    void createChatRoom_Success_ByBuyer() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(
                auction.getId(),
                seller.getId(),
                buyer.getId()
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(buyer.getEmail())
                .password(buyer.getPassword())
                .authorities("ROLE_USER")
                .build();

        // when
        ResponseEntity<ChatRoomResponse> response = chatController.createChatRoom(request, userDetails);

        // then
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBuyerId()).isEqualTo(buyer.getId());
    }

    @Test
    @DisplayName("채팅방 중복 생성 방지 - 동일한 경매에 대한 채팅방이 이미 존재하는 경우")
    void createChatRoom_PreventDuplicate() {
        // given - 첫 번째 채팅방 생성
        CreateChatRoomRequest request = new CreateChatRoomRequest(
                auction.getId(),
                seller.getId(),
                buyer.getId()
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(seller.getEmail())
                .password(seller.getPassword())
                .authorities("ROLE_USER")
                .build();

        ResponseEntity<ChatRoomResponse> firstResponse = chatController.createChatRoom(request, userDetails);
        Long firstChatRoomId = firstResponse.getBody().getId();

        // when - 동일한 요청으로 두 번째 채팅방 생성 시도
        ResponseEntity<ChatRoomResponse> secondResponse = chatController.createChatRoom(request, userDetails);

        // then - 동일한 채팅방 반환
        assertThat(secondResponse.getBody().getId()).isEqualTo(firstChatRoomId);

        // 데이터베이스에 하나의 채팅방만 존재하는지 확인
        long chatRoomCount = chatRoomRepository
                .findByAuctionIdAndSellerIdAndBuyerId(auction.getId(), seller.getId(), buyer.getId())
                .stream()
                .count();
        assertThat(chatRoomCount).isEqualTo(1);
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 권한 없는 사용자가 요청")
    void createChatRoom_Fail_Unauthorized() {
        // given - 제3자 사용자 생성
        User unauthorized = User.builder()
                .email("unauthorized@test.com")
                .nickname("제3자")
                .password("password")
                .phoneNumber("01099999999")
                .build();
        unauthorized = userRepository.save(unauthorized);

        CreateChatRoomRequest request = new CreateChatRoomRequest(
                auction.getId(),
                seller.getId(),
                buyer.getId()
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(unauthorized.getEmail())
                .password(unauthorized.getPassword())
                .authorities("ROLE_USER")
                .build();

        // when & then - 권한 없음 예외 발생
        assertThatThrownBy(() -> chatController.createChatRoom(request, userDetails))
                .isInstanceOf(com.cherrypick.app.common.exception.BusinessException.class)
                .hasMessageContaining("접근이 금지되었습니다");
    }

    @Test
    @DisplayName("채팅방 생성 시 타입 호환성 검증 - Long 타입 올바르게 처리")
    void createChatRoom_TypeCompatibility() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(
                auction.getId(),  // Long 타입
                seller.getId(),   // Long 타입
                buyer.getId()     // Long 타입
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(seller.getEmail())
                .password(seller.getPassword())
                .authorities("ROLE_USER")
                .build();

        // when
        ResponseEntity<ChatRoomResponse> response = chatController.createChatRoom(request, userDetails);

        // then - 타입이 올바르게 변환되어 저장되었는지 확인
        assertThat(response.getBody().getAuctionId()).isInstanceOf(Long.class);
        assertThat(response.getBody().getSellerId()).isInstanceOf(Long.class);
        assertThat(response.getBody().getBuyerId()).isInstanceOf(Long.class);

        assertThat(response.getBody().getAuctionId()).isEqualTo(auction.getId());
        assertThat(response.getBody().getSellerId()).isEqualTo(seller.getId());
        assertThat(response.getBody().getBuyerId()).isEqualTo(buyer.getId());
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 존재하지 않는 경매")
    void createChatRoom_Fail_AuctionNotFound() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(
                99999L,  // 존재하지 않는 경매 ID
                seller.getId(),
                buyer.getId()
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(seller.getEmail())
                .password(seller.getPassword())
                .authorities("ROLE_USER")
                .build();

        // when & then
        assertThatThrownBy(() -> chatController.createChatRoom(request, userDetails))
                .hasMessageContaining("경매");
    }

    @Test
    @DisplayName("채팅방 생성 실패 - 존재하지 않는 사용자")
    void createChatRoom_Fail_UserNotFound() {
        // given
        CreateChatRoomRequest request = new CreateChatRoomRequest(
                auction.getId(),
                seller.getId(),
                99999L  // 존재하지 않는 구매자 ID
        );

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(seller.getEmail())
                .password(seller.getPassword())
                .authorities("ROLE_USER")
                .build();

        // when & then
        assertThatThrownBy(() -> chatController.createChatRoom(request, userDetails))
                .hasMessageContaining("사용자");
    }
}
