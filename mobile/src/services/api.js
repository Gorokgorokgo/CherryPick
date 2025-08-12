import axios from 'axios';

/**
 * CherryPick API 클라이언트
 */

// API 기본 설정
const API_BASE_URL = 'http://localhost:8080/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터 (JWT 토큰 자동 추가)
apiClient.interceptors.request.use(
  (config) => {
    // TODO: AsyncStorage에서 JWT 토큰 가져와서 헤더에 추가
    // const token = await AsyncStorage.getItem('jwt_token');
    // if (token) {
    //   config.headers.Authorization = `Bearer ${token}`;
    // }
    
    // TODO: AsyncStorage에서 사용자 ID 가져와서 헤더에 추가
    // const userId = await AsyncStorage.getItem('user_id');
    // if (userId) {
    //   config.headers['User-Id'] = userId;
    // }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 응답 인터셉터 (에러 처리)
apiClient.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    if (error.response?.status === 401) {
      // JWT 토큰 만료 등 인증 오류 처리
      // TODO: 로그인 화면으로 리다이렉트
      console.log('인증 오류 - 로그인이 필요합니다.');
    }
    return Promise.reject(error);
  }
);

/**
 * 연결 서비스 API
 */
export const connectionApi = {
  // 수수료 정보 조회
  getFeeInfo: (connectionId) => 
    apiClient.get(`/connection/${connectionId}/fee`),
  
  // 연결 서비스 결제
  payConnectionFee: (connectionId, expectedFee) => 
    apiClient.post(`/connection/${connectionId}/pay`, {
      connectionId,
      expectedFee
    }),
  
  // 판매자 연결 목록 조회
  getSellerConnections: (page = 0, size = 20) => 
    apiClient.get('/connection/seller/my', {
      params: { page, size }
    }),
  
  // 구매자 연결 목록 조회
  getBuyerConnections: (page = 0, size = 20) => 
    apiClient.get('/connection/buyer/my', {
      params: { page, size }
    }),
  
  // 연결 서비스 상세 조회
  getConnectionDetail: (connectionId) => 
    apiClient.get(`/connection/${connectionId}`),
  
  // 거래 완료 처리
  completeTransaction: (connectionId) => 
    apiClient.post(`/connection/${connectionId}/complete`)
};

/**
 * 알림 API
 */
export const notificationApi = {
  // FCM 토큰 등록/업데이트
  updateFcmToken: (fcmToken) => 
    apiClient.post('/notifications/fcm-token', {
      fcmToken
    }),
  
  // 알림 설정 조회
  getSettings: () => 
    apiClient.get('/notifications/settings'),
  
  // 알림 설정 업데이트
  updateSettings: (settings) => 
    apiClient.patch('/notifications/settings', settings),
  
  // 알림 목록 조회
  getHistory: (page = 0, size = 20) => 
    apiClient.get('/notifications/history', {
      params: { page, size }
    }),
  
  // 특정 타입 알림 조회
  getHistoryByType: (type, page = 0, size = 20) => 
    apiClient.get(`/notifications/history/type/${type}`, {
      params: { page, size }
    }),
  
  // 읽지 않은 알림 개수
  getUnreadCount: () => 
    apiClient.get('/notifications/unread-count'),
  
  // 특정 알림 읽음 처리
  markAsRead: (notificationId) => 
    apiClient.post(`/notifications/${notificationId}/read`),
  
  // 모든 알림 읽음 처리
  markAllAsRead: () => 
    apiClient.post('/notifications/read-all'),
  
  // 모든 알림 끄기
  disableAll: () => 
    apiClient.post('/notifications/disable-all'),
  
  // 필수 알림만 켜기
  enableEssentialOnly: () => 
    apiClient.post('/notifications/essential-only')
};

/**
 * 인증 API (추후 구현)
 */
export const authApi = {
  // 휴대폰 인증 요청
  requestPhoneVerification: (phoneNumber) => 
    apiClient.post('/auth/phone/request', { phoneNumber }),
  
  // 인증 코드 확인
  verifyCode: (phoneNumber, code) => 
    apiClient.post('/auth/phone/verify', { phoneNumber, code }),
  
  // 회원가입
  signup: (userData) => 
    apiClient.post('/auth/signup', userData),
  
  // 로그인
  login: (phoneNumber, password) => 
    apiClient.post('/auth/login', { phoneNumber, password })
};

export default apiClient;