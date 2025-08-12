package com.cherrypick.app.domain.chat.service;

import com.cherrypick.app.domain.chat.entity.ChatRoom;
import com.cherrypick.app.domain.chat.repository.ChatRoomRepository;
import com.cherrypick.app.domain.connection.entity.ConnectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 채팅 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;

    /**
     * 연결 서비스 기반 채팅방 생성
     * 
     * @param connectionService 연결 서비스
     * @return 생성된 채팅방
     */
    @Transactional
    public ChatRoom createChatRoom(ConnectionService connectionService) {
        // 기존 채팅방 확인
        Optional<ChatRoom> existingRoom = chatRoomRepository
                .findByConnectionServiceId(connectionService.getId());
        
        if (existingRoom.isPresent()) {
            log.info("채팅방이 이미 존재합니다. connectionId: {}", connectionService.getId());
            return existingRoom.get();
        }
        
        // 새 채팅방 생성
        ChatRoom chatRoom = ChatRoom.createChatRoom(
                connectionService.getAuction(),
                connectionService.getSeller(),
                connectionService.getBuyer(),
                connectionService
        );
        
        ChatRoom savedRoom = chatRoomRepository.save(chatRoom);
        
        log.info("채팅방이 생성되었습니다. roomId: {}, connectionId: {}", 
                savedRoom.getId(), connectionService.getId());
        
        return savedRoom;
    }
    
    /**
     * 채팅방 활성화 (연결 서비스 결제 완료 시)
     * 
     * @param connectionService 연결 서비스
     * @return 활성화된 채팅방
     */
    @Transactional
    public ChatRoom activateChatRoom(ConnectionService connectionService) {
        // 채팅방 조회 또는 생성
        ChatRoom chatRoom = chatRoomRepository
                .findByConnectionServiceId(connectionService.getId())
                .orElseGet(() -> createChatRoom(connectionService));
        
        // 채팅방 활성화 (불변 객체 패턴)
        if (!chatRoom.isActive()) {
            ChatRoom activatedRoom = chatRoom.activateChatRoom();
            chatRoom = chatRoomRepository.save(activatedRoom);
            
            log.info("채팅방이 활성화되었습니다. roomId: {}, connectionId: {}", 
                    chatRoom.getId(), connectionService.getId());
        }
        
        return chatRoom;
    }
    
    /**
     * 채팅방 조회 (연결 서비스 기반)
     * 
     * @param connectionServiceId 연결 서비스 ID
     * @return 채팅방 (옵셔널)
     */
    public Optional<ChatRoom> getChatRoomByConnectionId(Long connectionServiceId) {
        return chatRoomRepository.findByConnectionServiceId(connectionServiceId);
    }
    
    /**
     * 채팅방 종료 (거래 완료 시)
     * 
     * @param connectionService 연결 서비스
     */
    @Transactional
    public void closeChatRoom(ConnectionService connectionService) {
        chatRoomRepository.findByConnectionServiceId(connectionService.getId())
                .ifPresent(chatRoom -> {
                    ChatRoom closedRoom = chatRoom.closeChatRoom();
                    chatRoomRepository.save(closedRoom);
                    
                    log.info("채팅방이 종료되었습니다. roomId: {}, connectionId: {}", 
                            closedRoom.getId(), connectionService.getId());
                });
    }
}