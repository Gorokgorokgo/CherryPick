package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.auction.entity.Auction;
import com.cherrypick.app.domain.auction.enums.Category;
import com.cherrypick.app.domain.auction.enums.RegionScope;
import com.cherrypick.app.domain.auction.repository.AuctionRepository;
import com.cherrypick.app.domain.chat.dto.request.SendMessageRequest;
import com.cherrypick.app.domain.chat.dto.response.ChatMessageResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomListResponse;
import com.cherrypick.app.domain.chat.dto.response.ChatRoomResponse;
import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.entity.ChatRoomParticipant;
import com.cherrypick.app.domain.chat.repository.ChatRoomParticipantRepository;
import com.cherrypick.app.domain.user.entity.User;
import com.cherrypick.app.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("채팅방 나가기/재입장 테스트")
class ChatServiceLeaveRejoinTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private ChatRoomParticipantRepository chatRoomParticipantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuctionRepository auctionRepository;

    private User seller;
    private User buyer;
    private Auction auction;
    private ChatRoom chatRoom;

    @BeforeEach
    void setUp() {
        // 판매자 생성
        seller = User.builder()
                .email("seller_leave_test@test.com")
                .nickname("판매자")
                .password("password")
                .phoneNumber("01012345678")
                .build();
        seller = userRepository.save(seller);

        // 구매자 생성
        buyer = User.builder()
                .email("buyer_leave_test@test.com")
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

        // 채팅방 생성 (낙찰 후 시나리오)
        chatRoom = chatService.createAuctionChatRoom(auction, seller, buyer);
    }

    // ==================== 채팅방 나가기 테스트 ====================

    @Test
    @DisplayName("판매자가 채팅방을 나가면 참여자 상태가 isLeft=true로 변경된다")
    void leaveChatRoom_Success() {
        // when
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // then
        Optional<ChatRoomParticipant> participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), seller.getId());

        assertThat(participant).isPresent();
        assertThat(participant.get().getIsLeft()).isTrue();
        assertThat(participant.get().getLeftAt()).isNotNull();
    }

    @Test
    @DisplayName("채팅방을 나간 사용자의 채팅방 목록에서 해당 채팅방이 보이지 않는다")
    void leaveChatRoom_NotVisibleInList() {
        // given
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // when
        List<ChatRoomListResponse> sellerChatRooms = chatService.getMyChatRooms(seller.getId(), null);

        // then
        assertThat(sellerChatRooms).isEmpty();

        // 구매자의 목록에는 여전히 보여야 함
        List<ChatRoomListResponse> buyerChatRooms = chatService.getMyChatRooms(buyer.getId(), null);
        assertThat(buyerChatRooms).hasSize(1);
    }

    @Test
    @DisplayName("이미 나간 채팅방을 다시 나가려고 하면 아무 일도 일어나지 않는다")
    void leaveChatRoom_AlreadyLeft() {
        // given
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // when - 다시 나가기 시도
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // then - 에러 없이 정상 처리
        Optional<ChatRoomParticipant> participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), seller.getId());

        assertThat(participant).isPresent();
        assertThat(participant.get().getIsLeft()).isTrue();
    }

    // ==================== 메시지 전송 시 자동 재입장 테스트 ====================

    @Test
    @DisplayName("판매자가 나간 상태에서 구매자가 메시지를 보내면 판매자가 자동으로 재입장한다")
    void sendMessage_AutoRejoinLeftParticipant() {
        // given - 판매자가 채팅방 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // 판매자가 나간 상태 확인
        assertThat(chatRoomParticipantRepository.hasUserLeftChatRoom(chatRoom.getId(), seller.getId())).isTrue();

        // when - 구매자가 메시지 전송
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("안녕하세요, 물건 받을 수 있을까요?");

        ChatMessageResponse response = chatService.sendMessage(chatRoom.getId(), buyer.getId(), request);

        // then - 메시지가 정상적으로 전송됨
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("안녕하세요, 물건 받을 수 있을까요?");

        // 판매자가 자동으로 재입장됨
        Optional<ChatRoomParticipant> sellerParticipant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), seller.getId());

        assertThat(sellerParticipant).isPresent();
        assertThat(sellerParticipant.get().getIsLeft()).isFalse();
        assertThat(sellerParticipant.get().getLeftAt()).isNull();
    }

    @Test
    @DisplayName("판매자가 재입장한 후 채팅방 목록에 다시 표시된다")
    void sendMessage_RejoinedRoomVisibleInList() {
        // given - 판매자가 채팅방 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // 목록에서 안 보이는 것 확인
        List<ChatRoomListResponse> beforeList = chatService.getMyChatRooms(seller.getId(), null);
        assertThat(beforeList).isEmpty();

        // when - 구매자가 메시지 전송 (판매자 자동 재입장)
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("테스트 메시지");
        chatService.sendMessage(chatRoom.getId(), buyer.getId(), request);

        // then - 판매자 목록에 다시 표시됨
        List<ChatRoomListResponse> afterList = chatService.getMyChatRooms(seller.getId(), null);
        assertThat(afterList).hasSize(1);
        assertThat(afterList.get(0).getId()).isEqualTo(chatRoom.getId());
    }

    @Test
    @DisplayName("발신자(구매자)도 나간 상태였다면 메시지 전송 시 자동 재입장한다")
    void sendMessage_SenderAlsoRejoins() {
        // given - 구매자도 나간 상태
        chatService.leaveChatRoom(chatRoom.getId(), buyer.getId());

        assertThat(chatRoomParticipantRepository.hasUserLeftChatRoom(chatRoom.getId(), buyer.getId())).isTrue();

        // when - 구매자가 메시지 전송
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("다시 연락드립니다");
        chatService.sendMessage(chatRoom.getId(), buyer.getId(), request);

        // then - 구매자도 재입장됨
        Optional<ChatRoomParticipant> buyerParticipant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), buyer.getId());

        assertThat(buyerParticipant).isPresent();
        assertThat(buyerParticipant.get().getIsLeft()).isFalse();
    }

    @Test
    @DisplayName("양쪽 모두 나간 상태에서 한쪽이 메시지를 보내면 양쪽 모두 재입장한다")
    void sendMessage_BothRejoin() {
        // given - 양쪽 모두 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());
        chatService.leaveChatRoom(chatRoom.getId(), buyer.getId());

        // when - 구매자가 메시지 전송
        SendMessageRequest request = new SendMessageRequest();
        request.setContent("다시 거래하고 싶습니다");
        chatService.sendMessage(chatRoom.getId(), buyer.getId(), request);

        // then - 양쪽 모두 재입장
        assertThat(chatRoomParticipantRepository.hasUserLeftChatRoom(chatRoom.getId(), seller.getId())).isFalse();
        assertThat(chatRoomParticipantRepository.hasUserLeftChatRoom(chatRoom.getId(), buyer.getId())).isFalse();
    }

    // ==================== 수동 재입장 테스트 ====================

    @Test
    @DisplayName("판매자가 낙찰 알림을 통해 채팅방에 수동으로 재입장할 수 있다")
    void rejoinChatRoom_Success() {
        // given - 판매자가 채팅방 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // when - 낙찰 알림 클릭으로 재입장
        ChatRoomResponse response = chatService.rejoinChatRoom(chatRoom.getId(), seller.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(chatRoom.getId());

        // 참여자 상태 확인
        Optional<ChatRoomParticipant> participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserId(chatRoom.getId(), seller.getId());

        assertThat(participant).isPresent();
        assertThat(participant.get().getIsLeft()).isFalse();
    }

    @Test
    @DisplayName("경매 ID로 채팅방을 조회하면서 자동으로 재입장한다")
    void getChatRoomByAuctionIdAndRejoin_Success() {
        // given - 판매자가 채팅방 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // when - 경매 ID로 채팅방 조회/재입장
        ChatRoomResponse response = chatService.getChatRoomByAuctionIdAndRejoin(auction.getId(), seller.getId());

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAuctionId()).isEqualTo(auction.getId());

        // 재입장됨
        assertThat(chatRoomParticipantRepository.hasUserLeftChatRoom(chatRoom.getId(), seller.getId())).isFalse();
    }

    @Test
    @DisplayName("나가지 않은 상태에서 재입장 API를 호출해도 정상 동작한다")
    void rejoinChatRoom_NotLeft() {
        // when - 나가지 않은 상태에서 재입장 시도
        ChatRoomResponse response = chatService.rejoinChatRoom(chatRoom.getId(), seller.getId());

        // then - 정상 동작
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(chatRoom.getId());
    }

    // ==================== 채팅방 상세 조회 테스트 ====================

    @Test
    @DisplayName("나간 채팅방도 roomId로 직접 조회는 가능하다")
    void getChatRoomDetails_AfterLeave() {
        // given
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // when
        ChatRoomResponse response = chatService.getChatRoomDetails(chatRoom.getId(), seller.getId());

        // then - 조회는 가능
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(chatRoom.getId());
    }

    // ==================== 연속 시나리오 테스트 ====================

    @Test
    @DisplayName("시나리오: 판매자 나감 → 구매자 메시지 → 판매자 재입장 → 대화 계속")
    void fullScenario() {
        // Step 1: 판매자가 실수로 채팅방 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());
        assertThat(chatService.getMyChatRooms(seller.getId(), null)).isEmpty();

        // Step 2: 구매자가 메시지 전송
        SendMessageRequest msg1 = new SendMessageRequest();
        msg1.setContent("물건 언제 받을 수 있나요?");
        ChatMessageResponse response1 = chatService.sendMessage(chatRoom.getId(), buyer.getId(), msg1);
        assertThat(response1).isNotNull();

        // Step 3: 판매자 자동 재입장 확인
        assertThat(chatService.getMyChatRooms(seller.getId(), null)).hasSize(1);

        // Step 4: 판매자가 답장
        SendMessageRequest msg2 = new SendMessageRequest();
        msg2.setContent("내일 오후에 가능합니다!");
        ChatMessageResponse response2 = chatService.sendMessage(chatRoom.getId(), seller.getId(), msg2);
        assertThat(response2).isNotNull();

        // Step 5: 대화 계속 가능
        SendMessageRequest msg3 = new SendMessageRequest();
        msg3.setContent("좋습니다, 내일 뵙겠습니다");
        ChatMessageResponse response3 = chatService.sendMessage(chatRoom.getId(), buyer.getId(), msg3);
        assertThat(response3).isNotNull();
    }

    @Test
    @DisplayName("시나리오: 판매자 나감 → 낙찰알림으로 재입장 → 대화")
    void rejoinViaNotification() {
        // Step 1: 판매자가 실수로 채팅방 나감
        chatService.leaveChatRoom(chatRoom.getId(), seller.getId());

        // Step 2: 낙찰 알림 클릭 (경매 ID로 채팅방 조회)
        ChatRoomResponse rejoined = chatService.getChatRoomByAuctionIdAndRejoin(auction.getId(), seller.getId());
        assertThat(rejoined).isNotNull();

        // Step 3: 채팅방 목록에 다시 표시됨
        assertThat(chatService.getMyChatRooms(seller.getId(), null)).hasSize(1);

        // Step 4: 메시지 전송 가능
        SendMessageRequest msg = new SendMessageRequest();
        msg.setContent("다시 들어왔습니다!");
        ChatMessageResponse response = chatService.sendMessage(chatRoom.getId(), seller.getId(), msg);
        assertThat(response).isNotNull();
    }
}
