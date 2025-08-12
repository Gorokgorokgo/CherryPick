import axios, { AxiosInstance, AxiosResponse } from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_CONFIG } from '../constants';
import { ApiResponse } from '../types';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: API_CONFIG.BASE_URL,
      timeout: API_CONFIG.TIMEOUT,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // Request interceptor - Add JWT token
    this.api.interceptors.request.use(
      async (config) => {
        const token = await AsyncStorage.getItem('jwt_token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor - Handle common responses
    this.api.interceptors.response.use(
      (response: AxiosResponse) => response,
      async (error) => {
        if (error.response?.status === 401) {
          // Token expired, remove it
          await AsyncStorage.removeItem('jwt_token');
          // Could trigger navigation to login here
        }
        return Promise.reject(error);
      }
    );
  }

  // Auth APIs
  async login(phoneNumber: string, password: string) {
    const response = await this.api.post('/auth/login', {
      phoneNumber,
      password,
    });
    return response.data;
  }

  async signup(userData: {
    phoneNumber: string;
    nickname: string;
    verificationCode: string;
  }) {
    const response = await this.api.post('/auth/signup', userData);
    return response.data;
  }

  // Auction APIs
  async getAuctions(page = 0, size = 20) {
    const response = await this.api.get(`/auctions?page=${page}&size=${size}`);
    return response.data;
  }

  async getAuctionDetail(auctionId: number) {
    const response = await this.api.get(`/auctions/${auctionId}`);
    return response.data;
  }

  // Chat APIs
  async getChatRooms(userId: number) {
    const response = await this.api.get(`/chat/rooms/user/${userId}`);
    return response.data;
  }

  async getChatMessages(roomId: number, page = 0) {
    const response = await this.api.get(`/chat/rooms/${roomId}/messages?page=${page}`);
    return response.data;
  }

  async sendMessage(roomId: number, message: string) {
    const response = await this.api.post(`/chat/rooms/${roomId}/messages`, {
      message,
      messageType: 'TEXT',
    });
    return response.data;
  }

  // Connection Service APIs
  async getConnectionFee(connectionId: number) {
    const response = await this.api.get(`/connection/${connectionId}/fee`);
    return response.data;
  }

  async payConnectionFee(connectionId: number, expectedFee: number) {
    const response = await this.api.post(`/connection/${connectionId}/pay`, {
      connectionId,
      expectedFee,
    });
    return response.data;
  }

  // User APIs
  async getProfile() {
    const response = await this.api.get('/users/profile');
    return response.data;
  }

  // Notification APIs
  async updateFcmToken(fcmToken: string) {
    const response = await this.api.post('/notifications/fcm-token', {
      fcmToken,
    });
    return response.data;
  }
}

export default new ApiService();