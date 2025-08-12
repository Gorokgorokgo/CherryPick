# CherryPick Mobile App

React Native 기반 중고물품 경매 앱

## 프로젝트 구조

```
mobile/
├── App.js                 # 메인 앱 컴포넌트
├── package.json            # 의존성 관리
├── src/
│   ├── screens/           # 화면 컴포넌트
│   │   ├── auth/         # 인증 관련 화면
│   │   ├── auction/      # 경매 관련 화면
│   │   ├── connection/   # 연결 서비스 화면
│   │   └── notification/ # 알림 관련 화면
│   ├── components/       # 재사용 컴포넌트
│   ├── services/        # API 서비스
│   ├── utils/           # 유틸리티 함수
│   └── constants/       # 상수 정의
└── android/             # Android 설정
└── ios/                 # iOS 설정
```

## 주요 기능

### 4주차 구현 완료 (백엔드)
- ✅ 연결 서비스 수수료 결제 API
- ✅ 레벨별 할인 시스템 (판매자 0-50% 할인)
- ✅ 결제 검증 및 실패 처리
- ✅ 채팅방 자동 활성화
- ✅ FCM 푸시 알림 시스템
- ✅ 알림 설정 관리 API

### 무료 프로모션 정책
- 현재 연결 서비스 수수료 0%
- 모든 알림 기능 무료 제공
- 추후 점진적 유료화 (1% → 2% → 3%)

## 설치 및 실행

```bash
# 의존성 설치
cd mobile
npm install

# iOS 실행
npx react-native run-ios

# Android 실행
npx react-native run-android
```

## API 연동

**백엔드 서버**: `http://localhost:8080/api`

### 주요 API 엔드포인트
- `POST /connection/{connectionId}/pay` - 연결 서비스 결제
- `GET /connection/{connectionId}/fee` - 수수료 정보 조회
- `POST /notifications/fcm-token` - FCM 토큰 등록
- `GET /notifications/settings` - 알림 설정 조회
- `PATCH /notifications/settings` - 알림 설정 업데이트

## 개발 계획

### 5주차 예정
- 실시간 채팅 시스템 구현
- 기본 화면 구성 (로그인, 메인, 경매상세)

### 6주차 예정  
- 경매 등록/상세 화면
- 프로필/포인트 관리 화면
- 연결 서비스 결제 화면

## 기술 스택

- **Framework**: React Native
- **네비게이션**: React Navigation
- **상태관리**: Redux Toolkit / Zustand
- **HTTP 클라이언트**: Axios
- **푸시 알림**: React Native Firebase
- **WebSocket**: Socket.io Client