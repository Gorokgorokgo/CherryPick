package com.cherrypick.app.domain.chat.dto.request;

import com.cherrypick.app.domain.chat.enums.MessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 채팅 메시지 전송 요청 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "채팅 메시지 전송 요청")
public class SendMessageRequest {

    @NotBlank(message = "메시지 내용은 필수입니다")
    @Size(max = 1000, message = "메시지는 1000자를 초과할 수 없습니다")
    @Schema(description = "메시지 내용", example = "안녕하세요! 거래 관련 문의드립니다.")
    private String content;

    @NotNull(message = "메시지 타입은 필수입니다")
    @Schema(description = "메시지 타입", example = "TEXT")
    private MessageType messageType = MessageType.TEXT;

    @Schema(description = "답장할 메시지 ID (답장인 경우)", example = "123")
    private Long replyToMessageId;

    public SendMessageRequest(String content) {
        this.content = content;
        this.messageType = MessageType.TEXT;
    }

    public SendMessageRequest(String content, MessageType messageType) {
        this.content = content;
        this.messageType = messageType;
    }
}