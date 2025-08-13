import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  RefreshControl,
  Image,
  Alert,
} from 'react-native';
import { COLORS, SIZES } from '../../constants';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import ApiService from '../../services/api';
import { Auction, ConnectionService, PaginatedResponse } from '../../types';
import { formatPrice } from '../../utils/validation';

interface MyAuctionsScreenProps {
  onBackPress: () => void;
  onAuctionPress: (auction: Auction) => void;
}

type TabType = 'selling' | 'bidding' | 'completed' | 'connections';

interface TabData {
  selling: Auction[];
  bidding: Auction[];
  completed: Auction[];
  connections: ConnectionService[];
}

export const MyAuctionsScreen: React.FC<MyAuctionsScreenProps> = ({
  onBackPress,
  onAuctionPress,
}) => {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [activeTab, setActiveTab] = useState<TabType>('selling');
  const [tabData, setTabData] = useState<TabData>({
    selling: [],
    bidding: [],
    completed: [],
    connections: [],
  });
  const [hasMore, setHasMore] = useState<Record<TabType, boolean>>({
    selling: true,
    bidding: true,
    completed: true,
    connections: true,
  });
  const [page, setPage] = useState<Record<TabType, number>>({
    selling: 0,
    bidding: 0,
    completed: 0,
    connections: 0,
  });

  useEffect(() => {
    loadTabData(activeTab);
  }, [activeTab]);

  const loadTabData = async (tab: TabType, refresh = false) => {
    try {
      if (refresh) {
        setRefreshing(true);
        setPage(prev => ({ ...prev, [tab]: 0 }));
      } else if (loading && page[tab] > 0) {
        return;
      }

      const currentPage = refresh ? 0 : page[tab];
      let response: PaginatedResponse<any>;

      switch (tab) {
        case 'selling':
          response = await ApiService.getMySellingAuctions(currentPage);
          break;
        case 'bidding':
          response = await ApiService.getMyBiddingAuctions(currentPage);
          break;
        case 'completed':
          response = await ApiService.getMyCompletedAuctions(currentPage);
          break;
        case 'connections':
          response = await ApiService.getMyConnections(currentPage);
          break;
        default:
          return;
      }

      if (refresh) {
        setTabData(prev => ({
          ...prev,
          [tab]: response.content,
        }));
      } else {
        setTabData(prev => ({
          ...prev,
          [tab]: [...prev[tab], ...response.content],
        }));
      }

      setHasMore(prev => ({
        ...prev,
        [tab]: currentPage < response.totalPages - 1,
      }));

      setPage(prev => ({
        ...prev,
        [tab]: currentPage + 1,
      }));
    } catch (error) {
      console.error(`Failed to load ${tab} data:`, error);
      Alert.alert('오류', '데이터를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleRefresh = () => {
    loadTabData(activeTab, true);
  };

  const handleLoadMore = () => {
    if (hasMore[activeTab] && !loading) {
      loadTabData(activeTab);
    }
  };

  const handleTabChange = (tab: TabType) => {
    setActiveTab(tab);
    if (tabData[tab].length === 0) {
      setLoading(true);
    }
  };

  const formatTimeRemaining = (endTime: string) => {
    const end = new Date(endTime);
    const now = new Date();
    const diff = end.getTime() - now.getTime();
    
    if (diff <= 0) return '종료됨';
    
    const hours = Math.floor(diff / (1000 * 60 * 60));
    const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
    
    if (hours > 24) {
      const days = Math.floor(hours / 24);
      return `${days}일 남음`;
    } else if (hours > 0) {
      return `${hours}시간 ${minutes}분 남음`;
    } else {
      return `${minutes}분 남음`;
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return COLORS.SUCCESS;
      case 'ENDED':
        return COLORS.WARNING;
      case 'CANCELLED':
        return COLORS.ERROR;
      case 'PENDING':
        return COLORS.WARNING;
      case 'COMPLETED':
        return COLORS.SUCCESS;
      default:
        return COLORS.TEXT_SECONDARY;
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case 'ACTIVE':
        return '진행중';
      case 'ENDED':
        return '종료됨';
      case 'CANCELLED':
        return '취소됨';
      case 'PENDING':
        return '결제대기';
      case 'COMPLETED':
        return '완료';
      default:
        return status;
    }
  };

  const renderAuctionItem = ({ item }: { item: Auction }) => (
    <TouchableOpacity
      style={styles.auctionCard}
      onPress={() => onAuctionPress(item)}
    >
      <View style={styles.imageContainer}>
        {item.images && item.images.length > 0 ? (
          <Image source={{ uri: item.images[0] }} style={styles.auctionImage} />
        ) : (
          <View style={[styles.auctionImage, styles.placeholderImage]}>
            <Text style={styles.placeholderText}>🖼️</Text>
          </View>
        )}
        <View
          style={[
            styles.statusBadge,
            { backgroundColor: getStatusColor(item.status) },
          ]}
        >
          <Text style={styles.statusText}>
            {getStatusText(item.status)}
          </Text>
        </View>
      </View>

      <View style={styles.auctionInfo}>
        <Text style={styles.auctionTitle} numberOfLines={2}>
          {item.title}
        </Text>

        <View style={styles.priceContainer}>
          <Text style={styles.currentPrice}>
            {formatPrice(item.currentPrice)}원
          </Text>
          <Text style={styles.bidCount}>입찰 {item.bidCount}회</Text>
        </View>

        <Text style={styles.region}>{item.region}</Text>

        <Text
          style={[
            styles.timeRemaining,
            item.status === 'ENDED' && styles.endedText,
          ]}
        >
          {formatTimeRemaining(item.endTime)}
        </Text>
      </View>
    </TouchableOpacity>
  );

  const renderConnectionItem = ({ item }: { item: ConnectionService }) => (
    <View style={styles.connectionCard}>
      <View style={styles.connectionHeader}>
        <Text style={styles.connectionTitle} numberOfLines={1}>
          {item.auctionTitle}
        </Text>
        <View
          style={[
            styles.connectionStatusBadge,
            { backgroundColor: getStatusColor(item.status) },
          ]}
        >
          <Text style={styles.connectionStatusText}>
            {getStatusText(item.status)}
          </Text>
        </View>
      </View>

      <View style={styles.connectionInfo}>
        <View style={styles.connectionDetail}>
          <Text style={styles.connectionLabel}>상대방</Text>
          <Text style={styles.connectionValue}>
            {item.sellerNickname === item.buyerNickname 
              ? item.buyerNickname 
              : item.sellerNickname}
          </Text>
        </View>

        <View style={styles.connectionDetail}>
          <Text style={styles.connectionLabel}>낙찰가</Text>
          <Text style={styles.connectionValue}>
            {formatPrice(item.finalPrice)}원
          </Text>
        </View>

        <View style={styles.connectionDetail}>
          <Text style={styles.connectionLabel}>연결수수료</Text>
          <Text style={[styles.connectionValue, { color: COLORS.PRIMARY }]}>
            {formatPrice(item.connectionFee)}원
          </Text>
        </View>

        {item.status === 'PENDING' && (
          <View style={styles.connectionActions}>
            <Button
              title="수수료 결제"
              onPress={() => {
                Alert.alert(
                  '연결 수수료 결제',
                  `${formatPrice(item.connectionFee)}원을 결제하시겠습니까?`,
                  [
                    { text: '취소', style: 'cancel' },
                    {
                      text: '결제',
                      onPress: async () => {
                        try {
                          await ApiService.payConnectionFee(
                            item.id,
                            item.connectionFee
                          );
                          Alert.alert('성공', '결제가 완료되었습니다. 채팅방이 활성화됩니다.');
                          handleRefresh();
                        } catch (error) {
                          Alert.alert('오류', '결제에 실패했습니다.');
                        }
                      },
                    },
                  ]
                );
              }}
              size="small"
              variant="primary"
            />
          </View>
        )}

        <Text style={styles.connectionDate}>
          {new Date(item.createdAt).toLocaleDateString()}
        </Text>
      </View>
    </View>
  );

  const renderEmptyState = () => {
    const messages = {
      selling: {
        title: '등록한 경매가 없습니다',
        subtitle: '첫 번째 경매를 등록해보세요!',
      },
      bidding: {
        title: '참여한 입찰이 없습니다',
        subtitle: '관심있는 경매에 입찰해보세요!',
      },
      completed: {
        title: '완료된 거래가 없습니다',
        subtitle: '거래를 완료하면 여기에 표시됩니다',
      },
      connections: {
        title: '연결 서비스 내역이 없습니다',
        subtitle: '낙찰 후 연결 서비스를 이용해보세요',
      },
    };

    const message = messages[activeTab];

    return (
      <View style={styles.emptyContainer}>
        <Text style={styles.emptyTitle}>{message.title}</Text>
        <Text style={styles.emptySubtitle}>{message.subtitle}</Text>
      </View>
    );
  };

  if (loading && tabData[activeTab].length === 0) {
    return <LoadingSpinner message="데이터를 불러오는 중..." />;
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onBackPress} style={styles.backButton}>
          <Text style={styles.backButtonText}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>내 경매 관리</Text>
        <View style={styles.placeholder} />
      </View>

      {/* 탭 네비게이션 */}
      <ScrollView horizontal showsHorizontalScrollIndicator={false}>
        <View style={styles.tabContainer}>
          {[
            { key: 'selling', label: '판매 중' },
            { key: 'bidding', label: '입찰 중' },
            { key: 'completed', label: '완료된 거래' },
            { key: 'connections', label: '연결 서비스' },
          ].map((tab) => (
            <TouchableOpacity
              key={tab.key}
              style={[
                styles.tab,
                activeTab === tab.key && styles.activeTab,
              ]}
              onPress={() => handleTabChange(tab.key as TabType)}
            >
              <Text
                style={[
                  styles.tabText,
                  activeTab === tab.key && styles.activeTabText,
                ]}
              >
                {tab.label}
              </Text>
            </TouchableOpacity>
          ))}
        </View>
      </ScrollView>

      {/* 컨텐츠 */}
      <View style={styles.content}>
        {tabData[activeTab].length === 0 ? (
          renderEmptyState()
        ) : (
          <FlatList
            data={tabData[activeTab]}
            renderItem={
              activeTab === 'connections' ? renderConnectionItem : renderAuctionItem
            }
            keyExtractor={(item) => item.id.toString()}
            numColumns={activeTab === 'connections' ? 1 : 2}
            key={activeTab === 'connections' ? 'single' : 'double'}
            contentContainerStyle={
              activeTab === 'connections' 
                ? styles.connectionsList 
                : styles.auctionsList
            }
            refreshControl={
              <RefreshControl
                refreshing={refreshing}
                onRefresh={handleRefresh}
                colors={[COLORS.PRIMARY]}
              />
            }
            onEndReached={handleLoadMore}
            onEndReachedThreshold={0.1}
            ListFooterComponent={
              loading && tabData[activeTab].length > 0 ? (
                <LoadingSpinner message="더 많은 데이터 불러오는 중..." />
              ) : null
            }
          />
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.SURFACE,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 50,
    paddingBottom: SIZES.PADDING,
    paddingHorizontal: SIZES.PADDING,
    backgroundColor: COLORS.PRIMARY,
  },
  backButton: {
    padding: 8,
  },
  backButtonText: {
    color: COLORS.BACKGROUND,
    fontSize: 16,
    fontWeight: '600',
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: COLORS.BACKGROUND,
  },
  placeholder: {
    width: 60,
  },
  tabContainer: {
    flexDirection: 'row',
    backgroundColor: COLORS.BACKGROUND,
    paddingHorizontal: SIZES.PADDING / 2,
  },
  tab: {
    paddingVertical: SIZES.PADDING,
    paddingHorizontal: SIZES.PADDING,
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTab: {
    borderBottomColor: COLORS.PRIMARY,
  },
  tabText: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    fontWeight: '500',
  },
  activeTabText: {
    color: COLORS.PRIMARY,
    fontWeight: '600',
  },
  content: {
    flex: 1,
  },
  auctionsList: {
    padding: SIZES.PADDING / 2,
  },
  connectionsList: {
    padding: SIZES.PADDING,
  },
  auctionCard: {
    flex: 1,
    backgroundColor: COLORS.BACKGROUND,
    borderRadius: SIZES.BORDER_RADIUS,
    margin: SIZES.PADDING / 2,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  imageContainer: {
    position: 'relative',
  },
  auctionImage: {
    width: '100%',
    height: 120,
    borderTopLeftRadius: SIZES.BORDER_RADIUS,
    borderTopRightRadius: SIZES.BORDER_RADIUS,
  },
  placeholderImage: {
    backgroundColor: COLORS.SURFACE,
    justifyContent: 'center',
    alignItems: 'center',
  },
  placeholderText: {
    fontSize: 30,
    opacity: 0.5,
  },
  statusBadge: {
    position: 'absolute',
    top: 8,
    right: 8,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    fontSize: 12,
    color: COLORS.BACKGROUND,
    fontWeight: '600',
  },
  auctionInfo: {
    padding: SIZES.PADDING,
  },
  auctionTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 8,
    lineHeight: 18,
  },
  priceContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 6,
  },
  currentPrice: {
    fontSize: 16,
    fontWeight: 'bold',
    color: COLORS.PRIMARY,
  },
  bidCount: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  region: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 6,
  },
  timeRemaining: {
    fontSize: 12,
    color: COLORS.WARNING,
    fontWeight: '600',
  },
  endedText: {
    color: COLORS.ERROR,
  },
  connectionCard: {
    backgroundColor: COLORS.BACKGROUND,
    borderRadius: SIZES.BORDER_RADIUS,
    padding: SIZES.PADDING,
    marginBottom: SIZES.MARGIN,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 4,
  },
  connectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: SIZES.MARGIN,
  },
  connectionTitle: {
    flex: 1,
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginRight: SIZES.MARGIN,
  },
  connectionStatusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  connectionStatusText: {
    fontSize: 12,
    color: COLORS.BACKGROUND,
    fontWeight: '600',
  },
  connectionInfo: {
    gap: 8,
  },
  connectionDetail: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  connectionLabel: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
  },
  connectionValue: {
    fontSize: 14,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
  },
  connectionActions: {
    marginTop: SIZES.MARGIN,
    alignItems: 'flex-end',
  },
  connectionDate: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    textAlign: 'right',
    marginTop: 4,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: SIZES.PADDING * 2,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    textAlign: 'center',
  },
});