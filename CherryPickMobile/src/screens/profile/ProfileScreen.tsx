import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  Image,
  StyleSheet,
  Alert,
  RefreshControl,
  Dimensions,
} from 'react-native';
import { launchImageLibrary, MediaType } from 'react-native-image-picker';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { COLORS, SIZES } from '../../constants';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import ApiService from '../../services/api';
import { User, UserStats, Point } from '../../types';
import { formatPrice } from '../../utils/validation';

const { width } = Dimensions.get('window');

interface ProfileScreenProps {
  onNavigateToPoints: () => void;
  onNavigateToMyAuctions: () => void;
  onNavigateToSettings: () => void;
  onLogout: () => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({
  onNavigateToPoints,
  onNavigateToMyAuctions,
  onNavigateToSettings,
  onLogout,
}) => {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [userStats, setUserStats] = useState<UserStats | null>(null);
  const [pointBalance, setPointBalance] = useState<number>(0);
  const [isEditingProfile, setIsEditingProfile] = useState(false);

  useEffect(() => {
    loadUserProfile();
  }, []);

  const loadUserProfile = async (isRefresh = false) => {
    try {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      // 병렬로 사용자 정보, 통계, 포인트 정보 로드
      const [userResponse, statsResponse, pointResponse] = await Promise.all([
        ApiService.getProfile(),
        ApiService.getUserStats(),
        ApiService.getPointBalance(),
      ]);

      setUser(userResponse.data);
      setUserStats(statsResponse.data);
      setPointBalance(pointResponse.data.balance);
    } catch (error) {
      console.error('Failed to load profile:', error);
      Alert.alert('오류', '프로필 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleRefresh = () => {
    loadUserProfile(true);
  };

  const handleProfileImageChange = () => {
    const options = {
      mediaType: 'photo' as MediaType,
      quality: 0.8,
      maxWidth: 500,
      maxHeight: 500,
    };

    launchImageLibrary(options, async (response) => {
      if (response.didCancel || response.errorMessage) {
        return;
      }

      if (response.assets && response.assets[0]) {
        try {
          setLoading(true);
          const imageUri = response.assets[0].uri!;
          
          // 이미지 업로드
          const uploadResult = await ApiService.uploadImage(imageUri);
          
          // 프로필 업데이트
          await ApiService.updateProfileImage(uploadResult.imageUrl);
          
          // 사용자 정보 새로고침
          await loadUserProfile();
          
          Alert.alert('성공', '프로필 사진이 변경되었습니다.');
        } catch (error) {
          console.error('Failed to update profile image:', error);
          Alert.alert('오류', '프로필 사진 변경에 실패했습니다.');
        } finally {
          setLoading(false);
        }
      }
    });
  };

  const handleLogout = () => {
    Alert.alert(
      '로그아웃',
      '로그아웃 하시겠습니까?',
      [
        {
          text: '취소',
          style: 'cancel',
        },
        {
          text: '로그아웃',
          style: 'destructive',
          onPress: async () => {
            try {
              await AsyncStorage.removeItem('jwt_token');
              onLogout();
            } catch (error) {
              console.error('Logout failed:', error);
            }
          },
        },
      ]
    );
  };

  const getLevelInfo = (level: number, experiencePoints: number) => {
    const levelThreshold = level * 100; // 레벨당 100 경험치 필요
    const nextLevelThreshold = (level + 1) * 100;
    const progressPercentage = (experiencePoints / nextLevelThreshold) * 100;
    
    return {
      currentLevel: level,
      progress: Math.min(progressPercentage, 100),
      nextLevelExp: nextLevelThreshold - experiencePoints,
    };
  };

  if (loading && !user) {
    return <LoadingSpinner message="프로필 정보를 불러오는 중..." />;
  }

  if (!user || !userStats) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>프로필 정보를 불러올 수 없습니다</Text>
        <Button
          title="다시 시도"
          onPress={() => loadUserProfile()}
          variant="outline"
        />
      </View>
    );
  }

  const levelInfo = getLevelInfo(user.level, user.experiencePoints);

  return (
    <ScrollView
      style={styles.container}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={handleRefresh}
          colors={[COLORS.PRIMARY]}
        />
      }
    >
      {/* 프로필 헤더 */}
      <View style={styles.header}>
        <View style={styles.profileSection}>
          <TouchableOpacity
            style={styles.profileImageContainer}
            onPress={handleProfileImageChange}
            disabled={loading}
          >
            {user.profileImage ? (
              <Image source={{ uri: user.profileImage }} style={styles.profileImage} />
            ) : (
              <View style={[styles.profileImage, styles.defaultProfileImage]}>
                <Text style={styles.defaultProfileText}>👤</Text>
              </View>
            )}
            <View style={styles.editImageBadge}>
              <Text style={styles.editImageText}>✏️</Text>
            </View>
          </TouchableOpacity>

          <View style={styles.profileInfo}>
            <Text style={styles.nickname}>{user.nickname}</Text>
            <Text style={styles.phoneNumber}>
              {user.phoneNumber.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3')}
            </Text>
            <Text style={styles.joinDate}>
              {new Date(user.createdAt).toLocaleDateString()} 가입
            </Text>
          </View>
        </View>

        {/* 레벨 정보 */}
        <View style={styles.levelSection}>
          <View style={styles.levelHeader}>
            <Text style={styles.levelText}>레벨 {levelInfo.currentLevel}</Text>
            <Text style={styles.expText}>
              {user.experiencePoints} EXP
            </Text>
          </View>
          <View style={styles.progressBarContainer}>
            <View style={styles.progressBarBackground}>
              <View
                style={[
                  styles.progressBarFill,
                  { width: `${levelInfo.progress}%` },
                ]}
              />
            </View>
            <Text style={styles.nextLevelText}>
              다음 레벨까지 {levelInfo.nextLevelExp} EXP
            </Text>
          </View>
        </View>
      </View>

      {/* 포인트 정보 */}
      <TouchableOpacity style={styles.pointCard} onPress={onNavigateToPoints}>
        <View style={styles.pointHeader}>
          <Text style={styles.pointTitle}>💰 체리 포인트</Text>
          <Text style={styles.pointBalance}>
            {formatPrice(pointBalance)}원
          </Text>
        </View>
        <Text style={styles.pointSubtext}>
          포인트로 연결 서비스 이용 가능 →
        </Text>
      </TouchableOpacity>

      {/* 활동 통계 */}
      <View style={styles.statsSection}>
        <Text style={styles.sectionTitle}>활동 통계</Text>
        <View style={styles.statsGrid}>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userStats.totalAuctions}</Text>
            <Text style={styles.statLabel}>등록한 경매</Text>
          </View>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userStats.totalBids}</Text>
            <Text style={styles.statLabel}>참여한 입찰</Text>
          </View>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userStats.successfulSales}</Text>
            <Text style={styles.statLabel}>성공한 판매</Text>
          </View>
          <View style={styles.statItem}>
            <Text style={styles.statValue}>{userStats.successfulPurchases}</Text>
            <Text style={styles.statLabel}>성공한 구매</Text>
          </View>
        </View>
      </View>

      {/* 메뉴 목록 */}
      <View style={styles.menuSection}>
        <TouchableOpacity style={styles.menuItem} onPress={onNavigateToMyAuctions}>
          <Text style={styles.menuIcon}>📦</Text>
          <Text style={styles.menuText}>내 경매 관리</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={onNavigateToPoints}>
          <Text style={styles.menuIcon}>💳</Text>
          <Text style={styles.menuText}>포인트 관리</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem} onPress={onNavigateToSettings}>
          <Text style={styles.menuIcon}>⚙️</Text>
          <Text style={styles.menuText}>설정</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem}>
          <Text style={styles.menuIcon}>📞</Text>
          <Text style={styles.menuText}>고객센터</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.menuItem}>
          <Text style={styles.menuIcon}>❓</Text>
          <Text style={styles.menuText}>자주 묻는 질문</Text>
          <Text style={styles.menuArrow}>›</Text>
        </TouchableOpacity>
      </View>

      {/* 로그아웃 버튼 */}
      <View style={styles.logoutSection}>
        <Button
          title="로그아웃"
          onPress={handleLogout}
          variant="outline"
          size="large"
        />
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.SURFACE,
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: SIZES.PADDING * 2,
  },
  errorText: {
    fontSize: 16,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: SIZES.MARGIN * 2,
    textAlign: 'center',
  },
  header: {
    backgroundColor: COLORS.BACKGROUND,
    padding: SIZES.PADDING,
    marginBottom: SIZES.MARGIN / 2,
  },
  profileSection: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: SIZES.MARGIN,
  },
  profileImageContainer: {
    position: 'relative',
    marginRight: SIZES.MARGIN,
  },
  profileImage: {
    width: 80,
    height: 80,
    borderRadius: 40,
    backgroundColor: COLORS.SURFACE,
  },
  defaultProfileImage: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  defaultProfileText: {
    fontSize: 32,
    color: COLORS.TEXT_SECONDARY,
  },
  editImageBadge: {
    position: 'absolute',
    bottom: 0,
    right: 0,
    backgroundColor: COLORS.PRIMARY,
    width: 28,
    height: 28,
    borderRadius: 14,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 2,
    borderColor: COLORS.BACKGROUND,
  },
  editImageText: {
    fontSize: 12,
  },
  profileInfo: {
    flex: 1,
  },
  nickname: {
    fontSize: 20,
    fontWeight: 'bold',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 4,
  },
  phoneNumber: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 2,
  },
  joinDate: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  levelSection: {
    backgroundColor: COLORS.SURFACE,
    padding: SIZES.PADDING,
    borderRadius: SIZES.BORDER_RADIUS,
  },
  levelHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  levelText: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.PRIMARY,
  },
  expText: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
  },
  progressBarContainer: {
    alignItems: 'center',
  },
  progressBarBackground: {
    width: '100%',
    height: 8,
    backgroundColor: COLORS.BORDER,
    borderRadius: 4,
    overflow: 'hidden',
    marginBottom: 4,
  },
  progressBarFill: {
    height: '100%',
    backgroundColor: COLORS.PRIMARY,
  },
  nextLevelText: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  pointCard: {
    backgroundColor: COLORS.BACKGROUND,
    margin: SIZES.MARGIN / 2,
    marginHorizontal: SIZES.MARGIN,
    padding: SIZES.PADDING,
    borderRadius: SIZES.BORDER_RADIUS,
    borderWidth: 1,
    borderColor: COLORS.PRIMARY,
  },
  pointHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  pointTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
  },
  pointBalance: {
    fontSize: 20,
    fontWeight: 'bold',
    color: COLORS.PRIMARY,
  },
  pointSubtext: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  statsSection: {
    backgroundColor: COLORS.BACKGROUND,
    margin: SIZES.MARGIN / 2,
    marginHorizontal: SIZES.MARGIN,
    padding: SIZES.PADDING,
    borderRadius: SIZES.BORDER_RADIUS,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: SIZES.MARGIN,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  statItem: {
    width: '48%',
    alignItems: 'center',
    marginBottom: SIZES.MARGIN,
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: COLORS.PRIMARY,
    marginBottom: 4,
  },
  statLabel: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    textAlign: 'center',
  },
  menuSection: {
    backgroundColor: COLORS.BACKGROUND,
    margin: SIZES.MARGIN / 2,
    marginHorizontal: SIZES.MARGIN,
    borderRadius: SIZES.BORDER_RADIUS,
    overflow: 'hidden',
  },
  menuItem: {
    flexDirection: 'row',
    alignItems: 'center',
    padding: SIZES.PADDING,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.BORDER,
  },
  menuIcon: {
    fontSize: 20,
    marginRight: SIZES.MARGIN,
    width: 24,
    textAlign: 'center',
  },
  menuText: {
    flex: 1,
    fontSize: 16,
    color: COLORS.TEXT_PRIMARY,
  },
  menuArrow: {
    fontSize: 18,
    color: COLORS.TEXT_SECONDARY,
  },
  logoutSection: {
    padding: SIZES.PADDING,
    paddingBottom: SIZES.PADDING * 2,
  },
});