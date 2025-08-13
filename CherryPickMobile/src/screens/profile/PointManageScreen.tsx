import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  Alert,
  RefreshControl,
  TextInput,
} from 'react-native';
import { COLORS, SIZES } from '../../constants';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import ApiService from '../../services/api';
import { PointTransaction, PaginatedResponse } from '../../types';
import { formatPrice } from '../../utils/validation';

interface PointManageScreenProps {
  onBackPress: () => void;
}

const CHARGE_AMOUNTS = [10000, 30000, 50000, 100000, 300000, 500000];

export const PointManageScreen: React.FC<PointManageScreenProps> = ({
  onBackPress,
}) => {
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [balance, setBalance] = useState(0);
  const [transactions, setTransactions] = useState<PointTransaction[]>([]);
  const [hasMore, setHasMore] = useState(true);
  const [page, setPage] = useState(0);
  const [selectedAmount, setSelectedAmount] = useState<number | null>(null);
  const [customAmount, setCustomAmount] = useState('');
  const [activeTab, setActiveTab] = useState<'charge' | 'history'>('charge');

  useEffect(() => {
    loadPointData();
  }, []);

  const loadPointData = async (refresh = false) => {
    try {
      if (refresh) {
        setRefreshing(true);
        setPage(0);
      } else if (loading && page > 0) {
        return;
      }

      const currentPage = refresh ? 0 : page;
      
      const [balanceResponse, transactionsResponse] = await Promise.all([
        ApiService.getPointBalance(),
        ApiService.getPointTransactions(currentPage),
      ]);

      setBalance(balanceResponse.data.balance);

      if (refresh) {
        setTransactions(transactionsResponse.content);
      } else {
        setTransactions(prev => [...prev, ...transactionsResponse.content]);
      }

      setHasMore(currentPage < transactionsResponse.totalPages - 1);
      setPage(currentPage + 1);
    } catch (error) {
      console.error('Failed to load point data:', error);
      Alert.alert('오류', '포인트 정보를 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const handleRefresh = () => {
    loadPointData(true);
  };

  const handleLoadMore = () => {
    if (hasMore && !loading) {
      loadPointData();
    }
  };

  const handleChargePoint = async () => {
    const amount = selectedAmount || (customAmount ? Number(customAmount) : 0);
    
    if (!amount || amount < 1000) {
      Alert.alert('알림', '최소 1,000원 이상 충전 가능합니다.');
      return;
    }

    if (amount > 1000000) {
      Alert.alert('알림', '최대 1,000,000원까지 충전 가능합니다.');
      return;
    }

    if (amount % 1000 !== 0) {
      Alert.alert('알림', '1,000원 단위로 충전 가능합니다.');
      return;
    }

    Alert.alert(
      '포인트 충전',
      `${formatPrice(amount)}원을 충전하시겠습니까?`,
      [
        {
          text: '취소',
          style: 'cancel',
        },
        {
          text: '충전',
          onPress: async () => {
            try {
              setLoading(true);
              await ApiService.chargePoints(amount);
              
              Alert.alert('성공', '포인트 충전이 완료되었습니다.', [
                {
                  text: '확인',
                  onPress: () => {
                    setSelectedAmount(null);
                    setCustomAmount('');
                    loadPointData(true);
                  },
                },
              ]);
            } catch (error) {
              console.error('Point charge failed:', error);
              Alert.alert('오류', '포인트 충전에 실패했습니다. 다시 시도해주세요.');
            } finally {
              setLoading(false);
            }
          },
        },
      ]
    );
  };

  const getTransactionTypeText = (type: string) => {
    switch (type) {
      case 'CHARGE':
        return '충전';
      case 'USE':
        return '사용';
      case 'REFUND':
        return '환불';
      case 'CONNECTION_FEE':
        return '연결수수료';
      default:
        return type;
    }
  };

  const getTransactionColor = (type: string) => {
    switch (type) {
      case 'CHARGE':
      case 'REFUND':
        return COLORS.SUCCESS;
      case 'USE':
      case 'CONNECTION_FEE':
        return COLORS.ERROR;
      default:
        return COLORS.TEXT_SECONDARY;
    }
  };

  const getTransactionSign = (type: string) => {
    switch (type) {
      case 'CHARGE':
      case 'REFUND':
        return '+';
      case 'USE':
      case 'CONNECTION_FEE':
        return '-';
      default:
        return '';
    }
  };

  const renderTransactionItem = ({ item }: { item: PointTransaction }) => (
    <View style={styles.transactionItem}>
      <View style={styles.transactionMain}>
        <View style={styles.transactionInfo}>
          <Text style={styles.transactionType}>
            {getTransactionTypeText(item.type)}
          </Text>
          <Text style={styles.transactionDescription}>
            {item.description}
          </Text>
          <Text style={styles.transactionDate}>
            {new Date(item.createdAt).toLocaleDateString()} {' '}
            {new Date(item.createdAt).toLocaleTimeString()}
          </Text>
        </View>
        <View style={styles.transactionAmounts}>
          <Text
            style={[
              styles.transactionAmount,
              { color: getTransactionColor(item.type) },
            ]}
          >
            {getTransactionSign(item.type)}{formatPrice(item.amount)}원
          </Text>
          <Text style={styles.balanceAfter}>
            잔액: {formatPrice(item.balanceAfter)}원
          </Text>
        </View>
      </View>
    </View>
  );

  const renderChargeTab = () => (
    <View style={styles.chargeContainer}>
      {/* 현재 잔액 */}
      <View style={styles.balanceCard}>
        <Text style={styles.balanceLabel}>현재 보유 포인트</Text>
        <Text style={styles.balanceAmount}>{formatPrice(balance)}원</Text>
      </View>

      {/* 충전 금액 선택 */}
      <View style={styles.chargeSection}>
        <Text style={styles.sectionTitle}>충전 금액 선택</Text>
        <View style={styles.amountGrid}>
          {CHARGE_AMOUNTS.map((amount) => (
            <TouchableOpacity
              key={amount}
              style={[
                styles.amountButton,
                selectedAmount === amount && styles.selectedAmountButton,
              ]}
              onPress={() => {
                setSelectedAmount(amount);
                setCustomAmount('');
              }}
            >
              <Text
                style={[
                  styles.amountButtonText,
                  selectedAmount === amount && styles.selectedAmountButtonText,
                ]}
              >
                {formatPrice(amount)}원
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {/* 직접 입력 */}
        <View style={styles.customAmountSection}>
          <Text style={styles.customAmountLabel}>직접 입력 (1,000원 단위)</Text>
          <View style={styles.customAmountInput}>
            <TextInput
              style={styles.input}
              placeholder="금액을 입력하세요"
              value={customAmount}
              onChangeText={(text) => {
                const numericText = text.replace(/[^0-9]/g, '');
                setCustomAmount(numericText);
                setSelectedAmount(null);
              }}
              keyboardType="numeric"
              maxLength={7}
            />
            <Text style={styles.won}>원</Text>
          </View>
        </View>

        {/* 충전 버튼 */}
        <Button
          title="포인트 충전하기"
          onPress={handleChargePoint}
          disabled={!selectedAmount && !customAmount}
          loading={loading}
          size="large"
        />

        {/* 안내 문구 */}
        <View style={styles.noticeSection}>
          <Text style={styles.noticeTitle}>💡 충전 안내</Text>
          <Text style={styles.noticeText}>• 최소 1,000원부터 충전 가능합니다</Text>
          <Text style={styles.noticeText}>• 1,000원 단위로만 충전 가능합니다</Text>
          <Text style={styles.noticeText}>• 포인트는 연결 서비스 이용 시 사용됩니다</Text>
          <Text style={styles.noticeText}>• 미사용 포인트는 본인 계좌로 출금 가능합니다</Text>
        </View>
      </View>
    </View>
  );

  const renderHistoryTab = () => (
    <View style={styles.historyContainer}>
      {transactions.length === 0 ? (
        <View style={styles.emptyContainer}>
          <Text style={styles.emptyTitle}>거래 내역이 없습니다</Text>
          <Text style={styles.emptySubtitle}>
            포인트를 충전하거나 사용하면 여기에 표시됩니다
          </Text>
        </View>
      ) : (
        <FlatList
          data={transactions}
          renderItem={renderTransactionItem}
          keyExtractor={(item) => item.id.toString()}
          onEndReached={handleLoadMore}
          onEndReachedThreshold={0.1}
          ListFooterComponent={
            loading && transactions.length > 0 ? (
              <LoadingSpinner message="더 많은 거래 내역 불러오는 중..." />
            ) : null
          }
        />
      )}
    </View>
  );

  if (loading && balance === 0) {
    return <LoadingSpinner message="포인트 정보를 불러오는 중..." />;
  }

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={onBackPress} style={styles.backButton}>
          <Text style={styles.backButtonText}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>포인트 관리</Text>
        <View style={styles.placeholder} />
      </View>

      {/* 탭 네비게이션 */}
      <View style={styles.tabContainer}>
        <TouchableOpacity
          style={[
            styles.tab,
            activeTab === 'charge' && styles.activeTab,
          ]}
          onPress={() => setActiveTab('charge')}
        >
          <Text
            style={[
              styles.tabText,
              activeTab === 'charge' && styles.activeTabText,
            ]}
          >
            포인트 충전
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.tab,
            activeTab === 'history' && styles.activeTab,
          ]}
          onPress={() => setActiveTab('history')}
        >
          <Text
            style={[
              styles.tabText,
              activeTab === 'history' && styles.activeTabText,
            ]}
          >
            거래 내역
          </Text>
        </TouchableOpacity>
      </View>

      {/* 탭 컨텐츠 */}
      <ScrollView
        style={styles.content}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={handleRefresh}
            colors={[COLORS.PRIMARY]}
          />
        }
      >
        {activeTab === 'charge' ? renderChargeTab() : renderHistoryTab()}
      </ScrollView>
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
  },
  tab: {
    flex: 1,
    paddingVertical: SIZES.PADDING,
    alignItems: 'center',
    borderBottomWidth: 2,
    borderBottomColor: 'transparent',
  },
  activeTab: {
    borderBottomColor: COLORS.PRIMARY,
  },
  tabText: {
    fontSize: 16,
    color: COLORS.TEXT_SECONDARY,
  },
  activeTabText: {
    color: COLORS.PRIMARY,
    fontWeight: '600',
  },
  content: {
    flex: 1,
  },
  chargeContainer: {
    padding: SIZES.PADDING,
  },
  balanceCard: {
    backgroundColor: COLORS.BACKGROUND,
    padding: SIZES.PADDING * 1.5,
    borderRadius: SIZES.BORDER_RADIUS,
    alignItems: 'center',
    marginBottom: SIZES.MARGIN,
    borderWidth: 1,
    borderColor: COLORS.PRIMARY,
  },
  balanceLabel: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 8,
  },
  balanceAmount: {
    fontSize: 28,
    fontWeight: 'bold',
    color: COLORS.PRIMARY,
  },
  chargeSection: {
    backgroundColor: COLORS.BACKGROUND,
    padding: SIZES.PADDING,
    borderRadius: SIZES.BORDER_RADIUS,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: SIZES.MARGIN,
  },
  amountGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    marginBottom: SIZES.MARGIN,
  },
  amountButton: {
    width: '30%',
    paddingVertical: 12,
    backgroundColor: COLORS.SURFACE,
    borderRadius: SIZES.BORDER_RADIUS,
    alignItems: 'center',
    marginBottom: 8,
    borderWidth: 1,
    borderColor: COLORS.BORDER,
  },
  selectedAmountButton: {
    backgroundColor: COLORS.PRIMARY,
    borderColor: COLORS.PRIMARY,
  },
  amountButtonText: {
    fontSize: 14,
    color: COLORS.TEXT_PRIMARY,
  },
  selectedAmountButtonText: {
    color: COLORS.BACKGROUND,
    fontWeight: '600',
  },
  customAmountSection: {
    marginBottom: SIZES.MARGIN * 2,
  },
  customAmountLabel: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 8,
  },
  customAmountInput: {
    position: 'relative',
  },
  input: {
    borderWidth: 1,
    borderColor: COLORS.BORDER,
    borderRadius: SIZES.BORDER_RADIUS,
    paddingHorizontal: SIZES.PADDING,
    paddingVertical: 12,
    fontSize: 16,
    color: COLORS.TEXT_PRIMARY,
    paddingRight: 40,
  },
  won: {
    position: 'absolute',
    right: 16,
    top: 14,
    fontSize: 16,
    color: COLORS.TEXT_SECONDARY,
  },
  noticeSection: {
    marginTop: SIZES.MARGIN * 2,
    padding: SIZES.PADDING,
    backgroundColor: COLORS.SURFACE,
    borderRadius: SIZES.BORDER_RADIUS,
  },
  noticeTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 8,
  },
  noticeText: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    lineHeight: 18,
    marginBottom: 2,
  },
  historyContainer: {
    flex: 1,
    padding: SIZES.PADDING,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingTop: 100,
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
  transactionItem: {
    backgroundColor: COLORS.BACKGROUND,
    padding: SIZES.PADDING,
    borderRadius: SIZES.BORDER_RADIUS,
    marginBottom: 8,
  },
  transactionMain: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  transactionInfo: {
    flex: 1,
    marginRight: SIZES.MARGIN,
  },
  transactionType: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: 4,
  },
  transactionDescription: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 2,
  },
  transactionDate: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  transactionAmounts: {
    alignItems: 'flex-end',
  },
  transactionAmount: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 2,
  },
  balanceAfter: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
});