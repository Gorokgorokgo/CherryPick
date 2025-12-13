-- ChatRoom 반정규화 컬럼 추가 (미리보기 최적화)
ALTER TABLE chat_rooms ADD COLUMN last_message_content VARCHAR(255);
ALTER TABLE chat_rooms ADD COLUMN last_message_type VARCHAR(20);

-- ChatRoomParticipant 반정규화 컬럼 추가 (읽지 않은 메시지 수 최적화)
ALTER TABLE chat_room_participants ADD COLUMN unread_count INTEGER NOT NULL DEFAULT 0;
