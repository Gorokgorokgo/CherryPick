package com.cherrypick.app.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 생성 요청 DTO
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CreateChatRoomRequest {

    /**
     * 경매 ID
     */
    @NotNull(message = "경매 ID는 필수입니다")
    private Long auctionId;

    /**
     * 판매자 ID
     */
    @NotNull(message = "판매자 ID는 필수입니다")
    private Long sellerId;

    /**
     * 구매자 ID
     */
    @NotNull(message = "구매자 ID는 필수입니다")
    private Long buyerId;
}
