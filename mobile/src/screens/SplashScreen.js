import React, {useEffect} from 'react';
import {
  View,
  Text,
  StyleSheet,
  StatusBar,
  Animated,
} from 'react-native';

const SplashScreen = ({navigation}) => {
  const fadeAnim = new Animated.Value(0);
  const scaleAnim = new Animated.Value(0.8);

  useEffect(() => {
    // 애니메이션 실행
    Animated.parallel([
      Animated.timing(fadeAnim, {
        toValue: 1,
        duration: 1000,
        useNativeDriver: true,
      }),
      Animated.spring(scaleAnim, {
        toValue: 1,
        tension: 100,
        friction: 8,
        useNativeDriver: true,
      }),
    ]).start();

    // 3초 후 로그인 화면으로 이동
    const timer = setTimeout(() => {
      navigation.replace('Login');
    }, 3000);

    return () => clearTimeout(timer);
  }, [navigation, fadeAnim, scaleAnim]);

  return (
    <View style={styles.container}>
      <StatusBar barStyle="light-content" backgroundColor="#e91e63" />
      <Animated.View
        style={[
          styles.logoContainer,
          {
            opacity: fadeAnim,
            transform: [{scale: scaleAnim}],
          },
        ]}>
        <Text style={styles.logo}>🍒</Text>
        <Text style={styles.appName}>CherryPick</Text>
        <Text style={styles.tagline}>중고물품 경매의 새로운 경험</Text>
      </Animated.View>
      
      <Animated.View style={[styles.loadingContainer, {opacity: fadeAnim}]}>
        <Text style={styles.loadingText}>로딩 중...</Text>
      </Animated.View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#e91e63',
    justifyContent: 'center',
    alignItems: 'center',
  },
  logoContainer: {
    alignItems: 'center',
    marginBottom: 50,
  },
  logo: {
    fontSize: 80,
    marginBottom: 20,
  },
  appName: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#ffffff',
    marginBottom: 8,
  },
  tagline: {
    fontSize: 16,
    color: '#ffffff',
    opacity: 0.9,
  },
  loadingContainer: {
    position: 'absolute',
    bottom: 100,
  },
  loadingText: {
    fontSize: 14,
    color: '#ffffff',
    opacity: 0.8,
  },
});

export default SplashScreen;