// User Types
export interface User {
  id: number;
  phoneNumber: string;
  nickname: string;
  profileImage?: string;
  level: number;
  experiencePoints: number;
  createdAt: string;
}

// Auction Types
export interface Auction {
  id: number;
  title: string;
  description: string;
  category: string;
  startPrice: number;
  currentPrice: number;
  buyNowPrice?: number;
  status: 'ACTIVE' | 'ENDED' | 'CANCELLED';
  endTime: string;
  sellerId: number;
  sellerNickname: string;
  images: string[];
  bidCount: number;
  region: string;
  createdAt: string;
}

// Chat Types
export interface ChatRoom {
  id: number;
  auctionId: number;
  auctionTitle: string;
  sellerId: number;
  buyerId: number;
  sellerNickname: string;
  buyerNickname: string;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED';
  lastMessage?: ChatMessage;
  unreadCount: number;
  createdAt: string;
}

export interface ChatMessage {
  id: number;
  chatRoomId: number;
  senderId: number;
  senderNickname: string;
  message: string;
  messageType: 'TEXT' | 'IMAGE' | 'SYSTEM';
  isRead: boolean;
  createdAt: string;
}

// Connection Service Types
export interface ConnectionService {
  id: number;
  auctionId: number;
  auctionTitle: string;
  sellerId: number;
  buyerId: number;
  sellerNickname: string;
  buyerNickname: string;
  connectionFee: number;
  finalPrice: number;
  status: 'PENDING' | 'ACTIVE' | 'COMPLETED';
  connectedAt?: string;
  completedAt?: string;
  createdAt: string;
}

// Navigation Types
export type RootStackParamList = {
  Splash: undefined;
  Auth: undefined;
  Main: undefined;
};

export type AuthStackParamList = {
  Login: undefined;
  Signup: undefined;
};

export type MainTabParamList = {
  Home: undefined;
  Auction: undefined;
  Chat: undefined;
  Profile: undefined;
};

export type ChatStackParamList = {
  ChatList: undefined;
  ChatRoom: {
    roomId: number;
    roomTitle: string;
  };
};

// API Response Types
export interface ApiResponse<T> {
  success: boolean;
  data: T;
  message?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}