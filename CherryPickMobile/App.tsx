import React from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  Text,
  View,
} from 'react-native';

/**
 * CherryPick Mobile App
 * 중고물품 경매 앱
 */
function App(): React.JSX.Element {
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      <View style={styles.content}>
        <Text style={styles.title}>🍒 CherryPick</Text>
        <Text style={styles.subtitle}>중고물품 경매 앱</Text>
        
        <View style={styles.featureList}>
          <Text style={styles.featureTitle}>4주차 구현 완료 기능:</Text>
          <Text style={styles.feature}>✅ 연결 서비스 수수료 결제 시스템</Text>
          <Text style={styles.feature}>✅ 판매자 레벨별 할인 (최대 50%)</Text>
          <Text style={styles.feature}>✅ 결제 완료시 채팅방 자동 활성화</Text>
          <Text style={styles.feature}>✅ FCM 푸시 알림 시스템</Text>
          <Text style={styles.feature}>✅ 알림 설정 관리</Text>
        </View>
        
        <View style={styles.promoSection}>
          <Text style={styles.promoTitle}>🎉 현재 무료 프로모션</Text>
          <Text style={styles.promoText}>연결 서비스 수수료 0%</Text>
          <Text style={styles.promoText}>모든 기능 무료 이용</Text>
        </View>
        
        <View style={styles.nextSteps}>
          <Text style={styles.nextTitle}>다음 구현 예정:</Text>
          <Text style={styles.nextItem}>• 실시간 채팅 시스템</Text>
          <Text style={styles.nextItem}>• 사용자 인증 화면</Text>
          <Text style={styles.nextItem}>• 경매 목록/상세 화면</Text>
          <Text style={styles.nextItem}>• 연결 서비스 결제 화면</Text>
        </View>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  content: {
    flex: 1,
    padding: 20,
    justifyContent: 'center',
    alignItems: 'center',
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#e91e63',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 18,
    color: '#666666',
    marginBottom: 40,
  },
  featureList: {
    backgroundColor: '#f5f5f5',
    padding: 20,
    borderRadius: 12,
    marginBottom: 30,
    width: '100%',
  },
  featureTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 12,
  },
  feature: {
    fontSize: 14,
    color: '#555555',
    marginBottom: 6,
    paddingLeft: 10,
  },
  promoSection: {
    backgroundColor: '#e8f5e8',
    padding: 20,
    borderRadius: 12,
    marginBottom: 30,
    width: '100%',
    alignItems: 'center',
  },
  promoTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#2e7d32',
    marginBottom: 8,
  },
  promoText: {
    fontSize: 14,
    color: '#388e3c',
    marginBottom: 4,
  },
  nextSteps: {
    backgroundColor: '#fff3e0',
    padding: 20,
    borderRadius: 12,
    width: '100%',
  },
  nextTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#ef6c00',
    marginBottom: 12,
  },
  nextItem: {
    fontSize: 14,
    color: '#f57c00',
    marginBottom: 6,
    paddingLeft: 10,
  },
});

export default App;
