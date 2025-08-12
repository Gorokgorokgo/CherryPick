/**
 * CherryPick 앱 상수 정의
 */

// 색상 팔레트
export const COLORS = {
  // 주요 색상 (체리 테마)
  PRIMARY: '#e91e63',       // 체리 핑크
  PRIMARY_DARK: '#c2185b',  // 진한 핑크
  PRIMARY_LIGHT: '#f8bbd9', // 연한 핑크
  
  // 보조 색상
  SECONDARY: '#ff9800',     // 주황색 (경매/입찰)
  ACCENT: '#4caf50',        // 초록색 (성공/완료)
  WARNING: '#ff5722',       // 빨간색 (경고/중요)
  
  // 중성 색상
  WHITE: '#ffffff',
  BLACK: '#000000',
  GRAY_LIGHT: '#f5f5f5',
  GRAY_MEDIUM: '#cccccc',
  GRAY_DARK: '#666666',
  
  // 상태 색상
  SUCCESS: '#4caf50',       // 성공
  ERROR: '#f44336',         // 오류
  INFO: '#2196f3',          // 정보
  WARNING_BG: '#fff3e0',    // 경고 배경
  SUCCESS_BG: '#e8f5e8',    // 성공 배경
};

// 폰트 크기
export const FONT_SIZES = {
  SMALL: 12,
  MEDIUM: 14,
  LARGE: 16,
  XLARGE: 18,
  XXLARGE: 24,
  TITLE: 32,
};

// 간격/여백
export const SPACING = {
  SMALL: 8,
  MEDIUM: 16,
  LARGE: 24,
  XLARGE: 32,
};

// 연결 서비스 상태
export const CONNECTION_STATUS = {
  PENDING: 'PENDING',       // 결제 대기
  ACTIVE: 'ACTIVE',         // 활성화
  COMPLETED: 'COMPLETED',   // 완료
  CANCELLED: 'CANCELLED',   // 취소
};

// 연결 서비스 상태 설명
export const CONNECTION_STATUS_LABELS = {
  [CONNECTION_STATUS.PENDING]: '결제 대기',
  [CONNECTION_STATUS.ACTIVE]: '채팅 가능',
  [CONNECTION_STATUS.COMPLETED]: '거래 완료',
  [CONNECTION_STATUS.CANCELLED]: '취소됨',
};

// 연결 서비스 상태 색상
export const CONNECTION_STATUS_COLORS = {
  [CONNECTION_STATUS.PENDING]: COLORS.WARNING,
  [CONNECTION_STATUS.ACTIVE]: COLORS.SUCCESS,
  [CONNECTION_STATUS.COMPLETED]: COLORS.GRAY_DARK,
  [CONNECTION_STATUS.CANCELLED]: COLORS.ERROR,
};

// 알림 타입
export const NOTIFICATION_TYPES = {
  NEW_BID: 'NEW_BID',                               // 새로운 입찰
  AUCTION_WON: 'AUCTION_WON',                       // 낙찰
  CONNECTION_PAYMENT_REQUEST: 'CONNECTION_PAYMENT_REQUEST', // 연결 서비스 결제 요청
  CHAT_ACTIVATED: 'CHAT_ACTIVATED',                 // 채팅 활성화
  NEW_MESSAGE: 'NEW_MESSAGE',                       // 새 메시지
  TRANSACTION_COMPLETED: 'TRANSACTION_COMPLETED',   // 거래 완료
  PROMOTION: 'PROMOTION',                           // 프로모션
};

// 알림 타입 아이콘
export const NOTIFICATION_ICONS = {
  [NOTIFICATION_TYPES.NEW_BID]: '💰',
  [NOTIFICATION_TYPES.AUCTION_WON]: '🎉',
  [NOTIFICATION_TYPES.CONNECTION_PAYMENT_REQUEST]: '💳',
  [NOTIFICATION_TYPES.CHAT_ACTIVATED]: '💬',
  [NOTIFICATION_TYPES.NEW_MESSAGE]: '📨',
  [NOTIFICATION_TYPES.TRANSACTION_COMPLETED]: '✅',
  [NOTIFICATION_TYPES.PROMOTION]: '🎁',
};

// 프로모션 정보
export const PROMOTION = {
  FREE_CONNECTION_FEE: true,    // 연결 서비스 수수료 무료
  BASE_FEE_RATE: 0,            // 현재 수수료율 0%
  FUTURE_FEE_RATE: 3,          // 추후 수수료율 3%
};

// 레벨별 할인 정보
export const SELLER_LEVEL_DISCOUNTS = [
  { level: 1, discount: 0 },   // 0% 할인
  { level: 2, discount: 5 },   // 5% 할인
  { level: 3, discount: 10 },  // 10% 할인
  { level: 4, discount: 15 },  // 15% 할인
  { level: 5, discount: 20 },  // 20% 할인
  { level: 6, discount: 25 },  // 25% 할인
  { level: 7, discount: 30 },  // 30% 할인
  { level: 8, discount: 35 },  // 35% 할인
  { level: 9, discount: 40 },  // 40% 할인
  { level: 10, discount: 50 }, // 50% 할인
];

// API 상수
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api',
  TIMEOUT: 10000,
  DEFAULT_PAGE_SIZE: 20,
};

// 스크린 이름 (네비게이션용)
export const SCREEN_NAMES = {
  // 인증
  LOGIN: 'Login',
  SIGNUP: 'Signup',
  PHONE_VERIFICATION: 'PhoneVerification',
  
  // 메인
  HOME: 'Home',
  AUCTION_LIST: 'AuctionList',
  AUCTION_DETAIL: 'AuctionDetail',
  
  // 연결 서비스
  CONNECTION_LIST: 'ConnectionList',
  CONNECTION_DETAIL: 'ConnectionDetail',
  CONNECTION_PAYMENT: 'ConnectionPayment',
  
  // 채팅
  CHAT_LIST: 'ChatList',
  CHAT_ROOM: 'ChatRoom',
  
  // 알림
  NOTIFICATION_LIST: 'NotificationList',
  NOTIFICATION_SETTINGS: 'NotificationSettings',
  
  // 프로필
  PROFILE: 'Profile',
  POINT_MANAGEMENT: 'PointManagement',
  SETTINGS: 'Settings',
};

export default {
  COLORS,
  FONT_SIZES,
  SPACING,
  CONNECTION_STATUS,
  CONNECTION_STATUS_LABELS,
  CONNECTION_STATUS_COLORS,
  NOTIFICATION_TYPES,
  NOTIFICATION_ICONS,
  PROMOTION,
  SELLER_LEVEL_DISCOUNTS,
  API_CONFIG,
  SCREEN_NAMES,
};