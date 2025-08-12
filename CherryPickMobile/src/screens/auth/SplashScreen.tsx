import React, { useEffect } from 'react';
import { View, Text, StyleSheet, Animated, Dimensions } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { COLORS, SIZES, ANIMATIONS } from '../../constants';
import SocketService from '../../services/socket';

interface SplashScreenProps {
  onAuthRequired: () => void;
  onMainReady: () => void;
}

export const SplashScreen: React.FC<SplashScreenProps> = ({
  onAuthRequired,
  onMainReady,
}) => {
  const fadeAnim = new Animated.Value(0);
  const scaleAnim = new Animated.Value(0.8);

  useEffect(() => {
    // Start animations
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: ANIMATIONS.SLOW,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnim, {
        toValue: 1,
        tension: 50,
        friction: 5,
        useNativeDriver: true,
      }),
    ]).start();

    // Check authentication status
    checkAuthStatus();
  }, []);

  const checkAuthStatus = async () => {
    try {
      // Simulate loading time
      await new Promise(resolve => setTimeout(resolve, 2000));
      
      const token = await AsyncStorage.getItem('jwt_token');
      
      if (token) {
        // Connect socket and go to main
        await SocketService.connect();
        onMainReady();
      } else {
        // Go to auth
        onAuthRequired();
      }
    } catch (error) {
      console.error('Error checking auth status:', error);
      onAuthRequired();
    }
  };

  return (
    <View style={styles.container}>
      <Animated.View
        style={[
          styles.content,
          {
            opacity: fadeAnim,
            transform: [{ scale: scaleAnim }],
          },
        ]}
      >
        <Text style={styles.logo}>🍒</Text>
        <Text style={styles.title}>CherryPick</Text>
        <Text style={styles.subtitle}>중고물품 경매 앱</Text>
        
        <View style={styles.versionInfo}>
          <Text style={styles.version}>MVP 5주차 버전</Text>
          <Text style={styles.features}>실시간 채팅 시스템 추가</Text>
        </View>
      </Animated.View>
      
      <View style={styles.footer}>
        <Text style={styles.footerText}>
          안전하고 투명한 중고거래 플랫폼
        </Text>
      </View>
    </View>
  );
};

const { width, height } = Dimensions.get('window');

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.PRIMARY,
    justifyContent: 'center',
    alignItems: 'center',
  },
  content: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  logo: {
    fontSize: 80,
    marginBottom: SIZES.MARGIN,
  },
  title: {
    fontSize: 36,
    fontWeight: 'bold',
    color: COLORS.BACKGROUND,
    marginBottom: SIZES.MARGIN / 2,
  },
  subtitle: {
    fontSize: 18,
    color: COLORS.SECONDARY,
    marginBottom: SIZES.MARGIN * 2,
  },
  versionInfo: {
    alignItems: 'center',
    marginTop: SIZES.MARGIN * 2,
  },
  version: {
    fontSize: 14,
    color: COLORS.BACKGROUND,
    opacity: 0.8,
    marginBottom: 4,
  },
  features: {
    fontSize: 12,
    color: COLORS.SECONDARY,
    opacity: 0.9,
  },
  footer: {
    position: 'absolute',
    bottom: SIZES.MARGIN * 3,
    alignItems: 'center',
  },
  footerText: {
    fontSize: 14,
    color: COLORS.BACKGROUND,
    opacity: 0.7,
  },
});