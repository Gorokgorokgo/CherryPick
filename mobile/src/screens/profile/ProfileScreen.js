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

const ProfileScreen = ({navigation}) => {
  const userInfo = {
    nickname: '체리피커',
    phoneNumber: '010-1234-5678',
    level: 3,
    points: 125000,
    auctionCount: 15,
    bidCount: 42,
  };

  const menuItems = [
    {id: 1, title: '내 경매', icon: '📝', screen: 'MyAuctions'},
    {id: 2, title: '입찰 내역', icon: '🔨', screen: 'BidHistory'},
    {id: 3, title: '포인트 관리', icon: '💳', screen: 'PointManagement'},
    {id: 4, title: '계좌 관리', icon: '🏦', screen: 'AccountManagement'},
    {id: 5, title: '알림 설정', icon: '🔔', screen: 'NotificationSettings'},
    {id: 6, title: '고객센터', icon: '📞', screen: 'CustomerService'},
    {id: 7, title: '설정', icon: '⚙️', screen: 'Settings'},
  ];

  const handleLogout = () => {
    // TODO: 로그아웃 처리
    navigation.replace('Login');
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      
      <ScrollView style={styles.scrollView}>
        {/* 헤더 */}
        <View style={styles.header}>
          <Text style={styles.headerTitle}>프로필</Text>
          <TouchableOpacity onPress={handleLogout}>
            <Text style={styles.logoutText}>로그아웃</Text>
          </TouchableOpacity>
        </View>

        {/* 사용자 정보 */}
        <View style={styles.userInfoSection}>
          <View style={styles.profileImageContainer}>
            <Text style={styles.profileImagePlaceholder}>👤</Text>
          </View>
          
          <View style={styles.userDetails}>
            <Text style={styles.nickname}>{userInfo.nickname}</Text>
            <Text style={styles.phoneNumber}>{userInfo.phoneNumber}</Text>
            <View style={styles.levelContainer}>
              <Text style={styles.levelText}>레벨 {userInfo.level}</Text>
              <View style={styles.levelBar}>
                <View style={[styles.levelProgress, {width: '60%'}]} />
              </View>
            </View>
          </View>
          
          <TouchableOpacity style={styles.editButton}>
            <Text style={styles.editButtonText}>수정</Text>
          </TouchableOpacity>
        </View>

        {/* 통계 정보 */}
        <View style={styles.statsSection}>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userInfo.points.toLocaleString()}</Text>
            <Text style={styles.statLabel}>보유 포인트</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userInfo.auctionCount}</Text>
            <Text style={styles.statLabel}>등록한 경매</Text>
          </View>
          <View style={styles.statDivider} />
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userInfo.bidCount}</Text>
            <Text style={styles.statLabel}>참여한 입찰</Text>
          </View>
        </View>

        {/* 메뉴 목록 */}
        <View style={styles.menuSection}>
          {menuItems.map((item) => (
            <TouchableOpacity 
              key={item.id} 
              style={styles.menuItem}
              onPress={() => {
                // TODO: 각 화면으로 네비게이션
                console.log(`Navigate to ${item.screen}`);
              }}>
              <View style={styles.menuItemLeft}>
                <Text style={styles.menuIcon}>{item.icon}</Text>
                <Text style={styles.menuTitle}>{item.title}</Text>
              </View>
              <Text style={styles.menuArrow}>›</Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* 앱 정보 */}
        <View style={styles.appInfoSection}>
          <Text style={styles.appInfoText}>CherryPick v1.0.0</Text>
          <TouchableOpacity>
            <Text style={styles.appInfoLink}>이용약관</Text>
          </TouchableOpacity>
          <TouchableOpacity>
            <Text style={styles.appInfoLink}>개인정보처리방침</Text>
          </TouchableOpacity>
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
    paddingVertical: 16,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333333',
  },
  logoutText: {
    fontSize: 14,
    color: '#e91e63',
    fontWeight: '500',
  },
  userInfoSection: {
    backgroundColor: '#ffffff',
    padding: 20,
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  profileImageContainer: {
    width: 60,
    height: 60,
    backgroundColor: '#e0e0e0',
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  profileImagePlaceholder: {
    fontSize: 24,
  },
  userDetails: {
    flex: 1,
  },
  nickname: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 4,
  },
  phoneNumber: {
    fontSize: 14,
    color: '#666666',
    marginBottom: 8,
  },
  levelContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  levelText: {
    fontSize: 12,
    color: '#e91e63',
    fontWeight: 'bold',
    marginRight: 8,
  },
  levelBar: {
    flex: 1,
    height: 4,
    backgroundColor: '#e0e0e0',
    borderRadius: 2,
  },
  levelProgress: {
    height: '100%',
    backgroundColor: '#e91e63',
    borderRadius: 2,
  },
  editButton: {
    backgroundColor: '#f0f0f0',
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 6,
  },
  editButtonText: {
    fontSize: 14,
    color: '#333333',
    fontWeight: '500',
  },
  statsSection: {
    backgroundColor: '#ffffff',
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 20,
    marginTop: 8,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statValue: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#e91e63',
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
    color: '#666666',
  },
  statDivider: {
    width: 1,
    height: 40,
    backgroundColor: '#e0e0e0',
  },
  menuSection: {
    backgroundColor: '#ffffff',
    marginTop: 8,
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 20,
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  menuItemLeft: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  menuIcon: {
    fontSize: 20,
    marginRight: 16,
  },
  menuTitle: {
    fontSize: 16,
    color: '#333333',
  },
  menuArrow: {
    fontSize: 18,
    color: '#ccc',
  },
  appInfoSection: {
    backgroundColor: '#ffffff',
    padding: 20,
    marginTop: 8,
    alignItems: 'center',
  },
  appInfoText: {
    fontSize: 12,
    color: '#666666',
    marginBottom: 8,
  },
  appInfoLink: {
    fontSize: 12,
    color: '#e91e63',
    marginBottom: 4,
  },
});

export default ProfileScreen;