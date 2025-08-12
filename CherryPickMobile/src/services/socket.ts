import { io, Socket } from 'socket.io-client';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_CONFIG } from '../constants';
import { ChatMessage } from '../types';

class SocketService {
  private socket: Socket | null = null;
  private isConnected = false;

  async connect(): Promise<void> {
    if (this.socket && this.isConnected) {
      return;
    }

    try {
      const token = await AsyncStorage.getItem('jwt_token');
      
      this.socket = io(API_CONFIG.SOCKET_URL, {
        auth: {
          token: token ? `Bearer ${token}` : undefined,
        },
        autoConnect: true,
        reconnection: true,
        reconnectionAttempts: 5,
        reconnectionDelay: 1000,
      });

      this.socket.on('connect', () => {
        console.log('Socket connected:', this.socket?.id);
        this.isConnected = true;
      });

      this.socket.on('disconnect', (reason) => {
        console.log('Socket disconnected:', reason);
        this.isConnected = false;
      });

      this.socket.on('connect_error', (error) => {
        console.error('Socket connection error:', error);
        this.isConnected = false;
      });

    } catch (error) {
      console.error('Failed to connect socket:', error);
    }
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
      this.isConnected = false;
    }
  }

  // Chat Events
  joinChatRoom(roomId: number): void {
    if (this.socket && this.isConnected) {
      this.socket.emit('join_chat_room', { roomId });
    }
  }

  leaveChatRoom(roomId: number): void {
    if (this.socket && this.isConnected) {
      this.socket.emit('leave_chat_room', { roomId });
    }
  }

  sendChatMessage(roomId: number, message: string): void {
    if (this.socket && this.isConnected) {
      this.socket.emit('send_message', {
        roomId,
        message,
        messageType: 'TEXT',
      });
    }
  }

  onNewMessage(callback: (message: ChatMessage) => void): void {
    if (this.socket) {
      this.socket.on('new_message', callback);
    }
  }

  onMessageRead(callback: (data: { messageId: number; roomId: number }) => void): void {
    if (this.socket) {
      this.socket.on('message_read', callback);
    }
  }

  // Auction Events (for future use)
  joinAuctionRoom(auctionId: number): void {
    if (this.socket && this.isConnected) {
      this.socket.emit('join_auction_room', { auctionId });
    }
  }

  onNewBid(callback: (data: any) => void): void {
    if (this.socket) {
      this.socket.on('new_bid', callback);
    }
  }

  // Utility Methods
  isSocketConnected(): boolean {
    return this.isConnected && this.socket?.connected === true;
  }

  getSocketId(): string | undefined {
    return this.socket?.id;
  }

  // Remove event listeners
  removeListener(eventName: string): void {
    if (this.socket) {
      this.socket.off(eventName);
    }
  }

  removeAllListeners(): void {
    if (this.socket) {
      this.socket.removeAllListeners();
    }
  }
}

export default new SocketService();