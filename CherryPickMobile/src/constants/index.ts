// API Configuration
export const API_CONFIG = {
  BASE_URL: __DEV__ ? 'http://localhost:8080/api' : 'https://api.cherrypick.com/api',
  SOCKET_URL: __DEV__ ? 'http://localhost:8080' : 'https://api.cherrypick.com',
  TIMEOUT: 10000,
} as const;

// Screen Names
export const SCREENS = {
  // Auth Stack
  SPLASH: 'Splash',
  LOGIN: 'Login',
  SIGNUP: 'Signup',
  
  // Main Tab
  HOME: 'Home',
  AUCTION: 'Auction',
  CHAT: 'Chat',
  PROFILE: 'Profile',
  
  // Auction Stack
  AUCTION_DETAIL: 'AuctionDetail',
  AUCTION_CREATE: 'AuctionCreate',
  
  // Chat Stack
  CHAT_ROOM: 'ChatRoom',
  CHAT_LIST: 'ChatList',
} as const;

// Colors (CherryPick Theme)
export const COLORS = {
  PRIMARY: '#e91e63', // Cherry Pink
  SECONDARY: '#f8bbd9', // Light Pink
  SUCCESS: '#2e7d32', // Green
  WARNING: '#ef6c00', // Orange
  ERROR: '#d32f2f', // Red
  
  BACKGROUND: '#ffffff',
  SURFACE: '#f5f5f5',
  TEXT_PRIMARY: '#333333',
  TEXT_SECONDARY: '#666666',
  BORDER: '#e0e0e0',
  
  CHAT_BUBBLE_SENT: '#e91e63',
  CHAT_BUBBLE_RECEIVED: '#f0f0f0',
} as const;

// Sizes
export const SIZES = {
  PADDING: 16,
  MARGIN: 16,
  BORDER_RADIUS: 8,
  HEADER_HEIGHT: 60,
  TAB_BAR_HEIGHT: 80,
} as const;

// Animation Durations
export const ANIMATIONS = {
  FAST: 200,
  NORMAL: 300,
  SLOW: 500,
} as const;