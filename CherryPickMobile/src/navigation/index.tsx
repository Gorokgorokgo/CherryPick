import React, { useState, useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Text } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

import { COLORS, SCREENS } from '../constants';
import { RootStackParamList, AuthStackParamList, MainTabParamList, ChatStackParamList } from '../types';

// Screens
import { SplashScreen } from '../screens/auth/SplashScreen';
import { LoginScreen } from '../screens/auth/LoginScreen';
import { HomeScreen } from '../screens/main/HomeScreen';
import { ChatRoomScreen } from '../screens/chat/ChatRoomScreen';
import { AuctionCreateScreen } from '../screens/auction/AuctionCreateScreen';
import { ProfileScreen } from '../screens/profile/ProfileScreen';
import { PointManageScreen } from '../screens/profile/PointManageScreen';
import { MyAuctionsScreen } from '../screens/profile/MyAuctionsScreen';

const RootStack = createStackNavigator<RootStackParamList>();
const AuthStack = createStackNavigator<AuthStackParamList>();
const MainTab = createBottomTabNavigator<MainTabParamList>();
const ChatStack = createStackNavigator<ChatStackParamList>();

// Auction Screen Component
const AuctionScreen = ({ navigation }: any) => (
  <AuctionCreateScreen
    onCreateSuccess={() => {
      // 경매 등록 성공 후 홈 화면으로 이동
      navigation.navigate(SCREENS.HOME);
    }}
    onBackPress={() => {
      // 뒤로 가기 시 홈 화면으로 이동
      navigation.navigate(SCREENS.HOME);
    }}
  />
);

// Profile Screen Component
const ProfileScreenWrapper = ({ navigation }: any) => (
  <ProfileScreen
    onNavigateToPoints={() => {
      // 포인트 관리 화면으로 네비게이션 (모달 형태)
      navigation.navigate('PointManage');
    }}
    onNavigateToMyAuctions={() => {
      // 내 경매 관리 화면으로 네비게이션
      navigation.navigate('MyAuctions');
    }}
    onNavigateToSettings={() => {
      // 설정 화면으로 네비게이션 (추후 구현)
      console.log('Navigate to Settings');
    }}
    onLogout={() => {
      // 로그아웃 시 Auth 화면으로 이동
      navigation.navigate('Auth');
    }}
  />
);

const ChatListScreen = () => (
  <Text style={{ flex: 1, textAlign: 'center', marginTop: 100 }}>
    채팅 목록 화면 (구현 중)
  </Text>
);

// Auth Stack Navigator
function AuthNavigator() {
  const [showLogin, setShowLogin] = useState(false);

  if (!showLogin) {
    return (
      <SplashScreen
        onAuthRequired={() => setShowLogin(true)}
        onMainReady={() => {}} // Will be handled by root navigation
      />
    );
  }

  return (
    <AuthStack.Navigator screenOptions={{ headerShown: false }}>
      <AuthStack.Screen name={SCREENS.LOGIN}>
        {() => (
          <LoginScreen
            onLoginSuccess={() => {}} // Will be handled by root navigation
            onSignupPress={() => {}} // Signup screen not implemented yet
          />
        )}
      </AuthStack.Screen>
    </AuthStack.Navigator>
  );
}

// Chat Stack Navigator
function ChatNavigator() {
  return (
    <ChatStack.Navigator screenOptions={{ headerShown: false }}>
      <ChatStack.Screen name="ChatList" component={ChatListScreen} />
      <ChatStack.Screen name="ChatRoom">
        {({ navigation, route }) => (
          <ChatRoomScreen
            roomId={(route.params as any)?.roomId || 1}
            roomTitle={(route.params as any)?.roomTitle || '채팅방'}
            onBackPress={() => navigation.goBack()}
          />
        )}
      </ChatStack.Screen>
    </ChatStack.Navigator>
  );
}

// Main Tab Navigator
function MainNavigator() {
  return (
    <MainTab.Navigator
      screenOptions={{
        headerShown: false,
        tabBarActiveTintColor: COLORS.PRIMARY,
        tabBarInactiveTintColor: COLORS.TEXT_SECONDARY,
        tabBarStyle: {
          height: 60,
          paddingBottom: 8,
          paddingTop: 8,
        },
      }}
    >
      <MainTab.Screen
        name={SCREENS.HOME}
        options={{
          title: '홈',
          tabBarIcon: () => <Text style={{ fontSize: 20 }}>🏠</Text>,
        }}
      >
        {() => (
          <HomeScreen
            onAuctionPress={(auction) => {
              console.log('Auction pressed:', auction.title);
              // Navigate to auction detail (to be implemented)
            }}
          />
        )}
      </MainTab.Screen>

      <MainTab.Screen
        name={SCREENS.AUCTION}
        component={AuctionScreen}
        options={{
          title: '경매등록',
          tabBarIcon: () => <Text style={{ fontSize: 20 }}>➕</Text>,
        }}
      />

      <MainTab.Screen
        name={SCREENS.CHAT}
        component={ChatNavigator}
        options={{
          title: '채팅',
          tabBarIcon: () => <Text style={{ fontSize: 20 }}>💬</Text>,
        }}
      />

      <MainTab.Screen
        name={SCREENS.PROFILE}
        component={ProfileScreenWrapper}
        options={{
          title: '내정보',
          tabBarIcon: () => <Text style={{ fontSize: 20 }}>👤</Text>,
        }}
      />
    </MainTab.Navigator>
  );
}

// Root Navigator
export default function RootNavigator() {
  const [initialRoute, setInitialRoute] = useState<keyof RootStackParamList | null>(null);

  useEffect(() => {
    checkInitialRoute();
  }, []);

  const checkInitialRoute = async () => {
    try {
      const token = await AsyncStorage.getItem('jwt_token');
      setInitialRoute(token ? 'Main' : 'Auth');
    } catch (error) {
      console.error('Error checking initial route:', error);
      setInitialRoute('Auth');
    }
  };

  if (!initialRoute) {
    return (
      <SplashScreen
        onAuthRequired={() => setInitialRoute('Auth')}
        onMainReady={() => setInitialRoute('Main')}
      />
    );
  }

  return (
    <NavigationContainer>
      <RootStack.Navigator
        initialRouteName={initialRoute}
        screenOptions={{ headerShown: false }}
      >
        <RootStack.Screen name="Splash">
          {() => (
            <SplashScreen
              onAuthRequired={() => setInitialRoute('Auth')}
              onMainReady={() => setInitialRoute('Main')}
            />
          )}
        </RootStack.Screen>

        <RootStack.Screen name="Auth" component={AuthNavigator} />
        <RootStack.Screen name="Main" component={MainNavigator} />
        
        {/* Profile Stack Screens */}
        <RootStack.Screen 
          name="PointManage" 
          options={{ presentation: 'modal' }}
        >
          {({ navigation }) => (
            <PointManageScreen
              onBackPress={() => navigation.goBack()}
            />
          )}
        </RootStack.Screen>
        
        <RootStack.Screen 
          name="MyAuctions"
          options={{ presentation: 'modal' }}
        >
          {({ navigation }) => (
            <MyAuctionsScreen
              onBackPress={() => navigation.goBack()}
              onAuctionPress={(auction) => {
                console.log('Navigate to auction detail:', auction.title);
                // TODO: 경매 상세 화면으로 네비게이션
                navigation.goBack();
              }}
            />
          )}
        </RootStack.Screen>
      </RootStack.Navigator>
    </NavigationContainer>
  );
}