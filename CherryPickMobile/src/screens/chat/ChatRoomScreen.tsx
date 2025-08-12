import React, { useState, useEffect, useRef } from 'react';
import {
  View,
  Text,
  FlatList,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { COLORS, SIZES } from '../../constants';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import ApiService from '../../services/api';
import SocketService from '../../services/socket';
import { ChatMessage, User } from '../../types';

interface ChatRoomScreenProps {
  roomId: number;
  roomTitle: string;
  onBackPress: () => void;
}

export const ChatRoomScreen: React.FC<ChatRoomScreenProps> = ({
  roomId,
  roomTitle,
  onBackPress,
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [currentUser, setCurrentUser] = useState<User | null>(null);
  const flatListRef = useRef<FlatList>(null);

  useEffect(() => {
    initializeChat();
    return () => {
      // Leave chat room when component unmounts
      SocketService.leaveChatRoom(roomId);
      SocketService.removeListener('new_message');
      SocketService.removeListener('message_read');
    };
  }, [roomId]);

  const initializeChat = async () => {
    try {
      // Get current user info
      const userInfo = await AsyncStorage.getItem('user_info');
      if (userInfo) {
        setCurrentUser(JSON.parse(userInfo));
      }

      // Load chat messages
      await loadMessages();
      
      // Join chat room for real-time updates
      SocketService.joinChatRoom(roomId);
      
      // Listen for new messages
      SocketService.onNewMessage((message: ChatMessage) => {
        if (message.chatRoomId === roomId) {
          setMessages(prev => [...prev, message]);
          scrollToBottom();
        }
      });

      // Listen for message read updates
      SocketService.onMessageRead((data) => {
        if (data.roomId === roomId) {
          setMessages(prev =>
            prev.map(msg =>
              msg.id === data.messageId ? { ...msg, isRead: true } : msg
            )
          );
        }
      });

    } catch (error) {
      console.error('Failed to initialize chat:', error);
      Alert.alert('오류', '채팅을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const loadMessages = async () => {
    try {
      const response = await ApiService.getChatMessages(roomId);
      setMessages(response.data || []);
      setTimeout(scrollToBottom, 100);
    } catch (error) {
      console.error('Failed to load messages:', error);
    }
  };

  const sendMessage = async () => {
    const trimmedMessage = inputMessage.trim();
    if (!trimmedMessage || sending) return;

    setSending(true);
    const tempMessage: ChatMessage = {
      id: Date.now(), // Temporary ID
      chatRoomId: roomId,
      senderId: currentUser?.id || 0,
      senderNickname: currentUser?.nickname || '',
      message: trimmedMessage,
      messageType: 'TEXT',
      isRead: false,
      createdAt: new Date().toISOString(),
    };

    // Add message optimistically
    setMessages(prev => [...prev, tempMessage]);
    setInputMessage('');
    scrollToBottom();

    try {
      if (SocketService.isSocketConnected()) {
        // Send via socket for real-time delivery
        SocketService.sendChatMessage(roomId, trimmedMessage);
      } else {
        // Fallback to API
        await ApiService.sendMessage(roomId, trimmedMessage);
        await loadMessages(); // Reload to get the actual message
      }
    } catch (error) {
      console.error('Failed to send message:', error);
      // Remove the temporary message
      setMessages(prev => prev.filter(msg => msg.id !== tempMessage.id));
      Alert.alert('오류', '메시지 전송에 실패했습니다.');
    } finally {
      setSending(false);
    }
  };

  const scrollToBottom = () => {
    if (flatListRef.current && messages.length > 0) {
      flatListRef.current.scrollToEnd({ animated: true });
    }
  };

  const formatMessageTime = (createdAt: string) => {
    const date = new Date(createdAt);
    const now = new Date();
    const diff = now.getTime() - date.getTime();
    
    if (diff < 60000) { // Less than 1 minute
      return '방금';
    } else if (diff < 3600000) { // Less than 1 hour
      return `${Math.floor(diff / 60000)}분 전`;
    } else if (date.toDateString() === now.toDateString()) { // Same day
      return date.toLocaleTimeString('ko-KR', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: false 
      });
    } else {
      return date.toLocaleDateString('ko-KR', { 
        month: 'short', 
        day: 'numeric' 
      });
    }
  };

  const renderMessage = ({ item, index }: { item: ChatMessage; index: number }) => {
    const isMyMessage = item.senderId === currentUser?.id;
    const showSender = !isMyMessage && 
      (index === 0 || messages[index - 1].senderId !== item.senderId);

    return (
      <View style={[
        styles.messageContainer,
        isMyMessage ? styles.myMessageContainer : styles.otherMessageContainer
      ]}>
        {showSender && (
          <Text style={styles.senderName}>{item.senderNickname}</Text>
        )}
        
        <View style={[
          styles.messageBubble,
          isMyMessage ? styles.myMessageBubble : styles.otherMessageBubble
        ]}>
          <Text style={[
            styles.messageText,
            isMyMessage ? styles.myMessageText : styles.otherMessageText
          ]}>
            {item.message}
          </Text>
        </View>
        
        <View style={styles.messageInfo}>
          <Text style={styles.messageTime}>
            {formatMessageTime(item.createdAt)}
          </Text>
          {isMyMessage && (
            <Text style={styles.readStatus}>
              {item.isRead ? '읽음' : ''}
            </Text>
          )}
        </View>
      </View>
    );
  };

  if (loading) {
    return <LoadingSpinner message="채팅방을 불러오는 중..." />;
  }

  return (
    <KeyboardAvoidingView 
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity onPress={onBackPress} style={styles.backButton}>
          <Text style={styles.backButtonText}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle} numberOfLines={1}>
          {roomTitle}
        </Text>
        <View style={styles.headerRight} />
      </View>

      {/* Messages */}
      <FlatList
        ref={flatListRef}
        data={messages}
        renderItem={renderMessage}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.messagesContainer}
        onContentSizeChange={scrollToBottom}
      />

      {/* Input */}
      <View style={styles.inputContainer}>
        <TextInput
          style={styles.textInput}
          value={inputMessage}
          onChangeText={setInputMessage}
          placeholder="메시지를 입력하세요..."
          multiline
          maxLength={500}
          editable={!sending}
        />
        <TouchableOpacity
          style={[
            styles.sendButton,
            (!inputMessage.trim() || sending) && styles.sendButtonDisabled
          ]}
          onPress={sendMessage}
          disabled={!inputMessage.trim() || sending}
        >
          <Text style={[
            styles.sendButtonText,
            (!inputMessage.trim() || sending) && styles.sendButtonTextDisabled
          ]}>
            {sending ? '전송중' : '전송'}
          </Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.BACKGROUND,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: COLORS.PRIMARY,
    paddingTop: 50,
    paddingBottom: SIZES.PADDING,
    paddingHorizontal: SIZES.PADDING,
  },
  backButton: {
    minWidth: 60,
  },
  backButtonText: {
    color: COLORS.BACKGROUND,
    fontSize: 16,
    fontWeight: '600',
  },
  headerTitle: {
    flex: 1,
    fontSize: 18,
    fontWeight: 'bold',
    color: COLORS.BACKGROUND,
    textAlign: 'center',
  },
  headerRight: {
    minWidth: 60,
  },
  messagesContainer: {
    padding: SIZES.PADDING,
    paddingBottom: SIZES.PADDING * 2,
  },
  messageContainer: {
    marginBottom: SIZES.MARGIN,
  },
  myMessageContainer: {
    alignItems: 'flex-end',
  },
  otherMessageContainer: {
    alignItems: 'flex-start',
  },
  senderName: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 4,
    marginHorizontal: SIZES.PADDING,
  },
  messageBubble: {
    maxWidth: '80%',
    paddingHorizontal: SIZES.PADDING,
    paddingVertical: SIZES.PADDING * 0.75,
    borderRadius: 18,
  },
  myMessageBubble: {
    backgroundColor: COLORS.CHAT_BUBBLE_SENT,
  },
  otherMessageBubble: {
    backgroundColor: COLORS.CHAT_BUBBLE_RECEIVED,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 20,
  },
  myMessageText: {
    color: COLORS.BACKGROUND,
  },
  otherMessageText: {
    color: COLORS.TEXT_PRIMARY,
  },
  messageInfo: {
    flexDirection: 'row',
    marginTop: 4,
    marginHorizontal: SIZES.PADDING,
  },
  messageTime: {
    fontSize: 11,
    color: COLORS.TEXT_SECONDARY,
  },
  readStatus: {
    fontSize: 11,
    color: COLORS.TEXT_SECONDARY,
    marginLeft: 8,
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'flex-end',
    backgroundColor: COLORS.BACKGROUND,
    borderTopWidth: 1,
    borderTopColor: COLORS.BORDER,
    paddingHorizontal: SIZES.PADDING,
    paddingVertical: SIZES.PADDING,
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: COLORS.BORDER,
    borderRadius: 20,
    paddingHorizontal: SIZES.PADDING,
    paddingVertical: SIZES.PADDING * 0.75,
    fontSize: 16,
    maxHeight: 100,
    marginRight: SIZES.MARGIN,
  },
  sendButton: {
    backgroundColor: COLORS.PRIMARY,
    paddingHorizontal: SIZES.PADDING * 1.5,
    paddingVertical: SIZES.PADDING * 0.75,
    borderRadius: 20,
    minWidth: 60,
    alignItems: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: COLORS.SURFACE,
  },
  sendButtonText: {
    color: COLORS.BACKGROUND,
    fontSize: 14,
    fontWeight: '600',
  },
  sendButtonTextDisabled: {
    color: COLORS.TEXT_SECONDARY,
  },
});