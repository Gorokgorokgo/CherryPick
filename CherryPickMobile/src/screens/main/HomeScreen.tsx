import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  RefreshControl,
  TouchableOpacity,
  Image,
  Alert,
} from 'react-native';
import { COLORS, SIZES } from '../../constants';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Button } from '../../components/common/Button';
import ApiService from '../../services/api';
import { Auction, PaginatedResponse } from '../../types';

interface HomeScreenProps {
  onAuctionPress: (auction: Auction) => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({ onAuctionPress }) => {
  const [auctions, setAuctions] = useState<Auction[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(true);

  useEffect(() => {
    loadAuctions();
  }, []);

  const loadAuctions = async (refresh = false) => {
    if (refresh) {
      setRefreshing(true);
      setPage(0);
    } else if (loading && page > 0) {
      return;
    }

    try {
      const currentPage = refresh ? 0 : page;
      const response: PaginatedResponse<Auction> = await ApiService.getAuctions(currentPage);
      
      if (refresh) {
        setAuctions(response.content);
      } else {
        setAuctions(prev => [...prev, ...response.content]);
      }
      
      setHasMore(currentPage < response.totalPages - 1);
      setPage(currentPage + 1);
    } catch (error) {
      console.error('Failed to load auctions:', error);
      Alert.alert('오류', '경매 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleRefresh = () => {
    loadAuctions(true);
  };

  const handleLoadMore = () => {
    if (hasMore && !loading) {
      loadAuctions();
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('ko-KR').format(price);
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

  const renderAuctionItem = ({ item }: { item: Auction }) => (
    <TouchableOpacity
      style={styles.auctionCard}
      onPress={() => onAuctionPress(item)}
      activeOpacity={0.7}
    >
      <View style={styles.imageContainer}>
        {item.images && item.images.length > 0 ? (
          <Image source={{ uri: item.images[0] }} style={styles.auctionImage} />
        ) : (
          <View style={[styles.auctionImage, styles.placeholderImage]}>
            <Text style={styles.placeholderText}>🖼️</Text>
          </View>
        )}
        <View style={styles.statusBadge}>
          <Text style={styles.statusText}>
            {item.status === 'ACTIVE' ? '진행중' : '종료'}
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
          <Text style={styles.bidCount}>
            입찰 {item.bidCount}회
          </Text>
        </View>
        
        <View style={styles.metaInfo}>
          <Text style={styles.seller}>{item.sellerNickname}</Text>
          <Text style={styles.region}>{item.region}</Text>
        </View>
        
        <Text style={[
          styles.timeRemaining,
          item.status === 'ENDED' && styles.endedText
        ]}>
          {formatTimeRemaining(item.endTime)}
        </Text>
      </View>
    </TouchableOpacity>
  );

  if (loading && auctions.length === 0) {
    return <LoadingSpinner message="경매 목록을 불러오는 중..." />;
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.headerTitle}>🍒 CherryPick</Text>
        <Text style={styles.headerSubtitle}>실시간 중고물품 경매</Text>
      </View>
      
      {auctions.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyTitle}>등록된 경매가 없습니다</Text>
          <Text style={styles.emptySubtitle}>
            첫 번째 경매를 등록해보세요!
          </Text>
          <Button
            title="새로고침"
            onPress={handleRefresh}
            variant="outline"
            style={styles.refreshButton}
          />
        </View>
      ) : (
        <FlatList
          data={auctions}
          renderItem={renderAuctionItem}
          keyExtractor={(item) => item.id.toString()}
          numColumns={2}
          contentContainerStyle={styles.listContainer}
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
            loading && auctions.length > 0 ? (
              <LoadingSpinner message="더 많은 경매 불러오는 중..." />
            ) : null
          }
        />
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.BACKGROUND,
  },
  header: {
    backgroundColor: COLORS.PRIMARY,
    paddingTop: 60,
    paddingBottom: SIZES.PADDING,
    paddingHorizontal: SIZES.PADDING,
  },
  headerTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: COLORS.BACKGROUND,
    marginBottom: 4,
  },
  headerSubtitle: {
    fontSize: 14,
    color: COLORS.SECONDARY,
  },
  listContainer: {
    padding: SIZES.PADDING / 2,
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
    backgroundColor: COLORS.PRIMARY,
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
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 8,
    lineHeight: 20,
  },
  priceContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  currentPrice: {
    fontSize: 18,
    fontWeight: 'bold',
    color: COLORS.PRIMARY,
  },
  bidCount: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  metaInfo: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  seller: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  region: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  timeRemaining: {
    fontSize: 12,
    color: COLORS.WARNING,
    fontWeight: '600',
  },
  endedText: {
    color: COLORS.ERROR,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: SIZES.PADDING * 2,
  },
  emptyTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 8,
  },
  emptySubtitle: {
    fontSize: 16,
    color: COLORS.TEXT_SECONDARY,
    textAlign: 'center',
    marginBottom: SIZES.MARGIN * 2,
  },
  refreshButton: {
    minWidth: 120,
  },
});