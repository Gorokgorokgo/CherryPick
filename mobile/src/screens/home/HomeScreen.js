import React from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  ScrollView,
  TouchableOpacity,
  StatusBar,
} from 'react-native';

const HomeScreen = ({navigation}) => {
  const featuredAuctions = [
    {id: 1, title: 'iPhone 14 Pro', currentPrice: 850000, timeLeft: '2시간 30분'},
    {id: 2, title: '삼성 갤럭시 S23', currentPrice: 720000, timeLeft: '1시간 15분'},
    {id: 3, title: '맥북 프로 M2', currentPrice: 1200000, timeLeft: '4시간 45분'},
  ];

  const categories = [
    {id: 1, name: '전자기기', icon: '📱'},
    {id: 2, name: '패션', icon: '👗'},
    {id: 3, name: '생활용품', icon: '🏠'},
    {id: 4, name: '스포츠', icon: '⚽'},
  ];

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      
      <ScrollView style={styles.scrollView}>
        {/* 헤더 */}
        <View style={styles.header}>
          <View>
            <Text style={styles.welcomeText}>안녕하세요! 👋</Text>
            <Text style={styles.headerTitle}>오늘의 경매를 확인해보세요</Text>
          </View>
          <TouchableOpacity style={styles.notificationButton}>
            <Text style={styles.notificationIcon}>🔔</Text>
          </TouchableOpacity>
        </View>

        {/* 카테고리 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>카테고리</Text>
          <View style={styles.categoriesContainer}>
            {categories.map(category => (
              <TouchableOpacity 
                key={category.id} 
                style={styles.categoryCard}>
                <Text style={styles.categoryIcon}>{category.icon}</Text>
                <Text style={styles.categoryName}>{category.name}</Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>

        {/* 인기 경매 */}
        <View style={styles.section}>
          <View style={styles.sectionHeader}>
            <Text style={styles.sectionTitle}>🔥 인기 경매</Text>
            <TouchableOpacity>
              <Text style={styles.seeAllText}>전체보기</Text>
            </TouchableOpacity>
          </View>
          
          {featuredAuctions.map(auction => (
            <TouchableOpacity 
              key={auction.id} 
              style={styles.auctionCard}>
              <View style={styles.auctionImage}>
                <Text style={styles.auctionImagePlaceholder}>📷</Text>
              </View>
              <View style={styles.auctionInfo}>
                <Text style={styles.auctionTitle}>{auction.title}</Text>
                <Text style={styles.currentPrice}>
                  현재가: {auction.currentPrice.toLocaleString()}원
                </Text>
                <Text style={styles.timeLeft}>⏰ {auction.timeLeft} 남음</Text>
              </View>
              <TouchableOpacity style={styles.bidButton}>
                <Text style={styles.bidButtonText}>입찰</Text>
              </TouchableOpacity>
            </TouchableOpacity>
          ))}
        </View>

        {/* 빠른 액션 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>빠른 액션</Text>
          <View style={styles.quickActionsContainer}>
            <TouchableOpacity style={styles.quickActionCard}>
              <Text style={styles.quickActionIcon}>📝</Text>
              <Text style={styles.quickActionText}>경매 등록</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.quickActionCard}>
              <Text style={styles.quickActionIcon}>💳</Text>
              <Text style={styles.quickActionText}>포인트 충전</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.quickActionCard}>
              <Text style={styles.quickActionIcon}>📊</Text>
              <Text style={styles.quickActionText}>내 경매</Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingVertical: 20,
    backgroundColor: '#ffffff',
  },
  welcomeText: {
    fontSize: 16,
    color: '#666666',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333333',
    marginTop: 4,
  },
  notificationButton: {
    padding: 8,
  },
  notificationIcon: {
    fontSize: 24,
  },
  section: {
    backgroundColor: '#ffffff',
    marginTop: 8,
    paddingHorizontal: 20,
    paddingVertical: 20,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 16,
  },
  seeAllText: {
    fontSize: 14,
    color: '#e91e63',
    fontWeight: '500',
  },
  categoriesContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  categoryCard: {
    alignItems: 'center',
    padding: 16,
    backgroundColor: '#f8f9fa',
    borderRadius: 12,
    width: '22%',
  },
  categoryIcon: {
    fontSize: 24,
    marginBottom: 8,
  },
  categoryName: {
    fontSize: 12,
    color: '#333333',
    textAlign: 'center',
  },
  auctionCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#f8f9fa',
    padding: 16,
    borderRadius: 12,
    marginBottom: 12,
  },
  auctionImage: {
    width: 60,
    height: 60,
    backgroundColor: '#e0e0e0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  auctionImagePlaceholder: {
    fontSize: 24,
  },
  auctionInfo: {
    flex: 1,
  },
  auctionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 4,
  },
  currentPrice: {
    fontSize: 14,
    color: '#e91e63',
    fontWeight: 'bold',
    marginBottom: 4,
  },
  timeLeft: {
    fontSize: 12,
    color: '#666666',
  },
  bidButton: {
    backgroundColor: '#e91e63',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  bidButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  quickActionsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  quickActionCard: {
    alignItems: 'center',
    padding: 20,
    backgroundColor: '#f8f9fa',
    borderRadius: 12,
    width: '30%',
  },
  quickActionIcon: {
    fontSize: 28,
    marginBottom: 8,
  },
  quickActionText: {
    fontSize: 14,
    color: '#333333',
    textAlign: 'center',
  },
});

export default HomeScreen;