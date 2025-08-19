import React, {useState} from 'react';
import {
  View,
  Text,
  StyleSheet,
  SafeAreaView,
  FlatList,
  TouchableOpacity,
  StatusBar,
  TextInput,
} from 'react-native';

const AuctionListScreen = () => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState('전체');

  const categories = ['전체', '전자기기', '패션', '생활용품', '스포츠'];
  
  const auctions = [
    {
      id: 1,
      title: 'iPhone 14 Pro 256GB',
      currentPrice: 850000,
      startPrice: 700000,
      timeLeft: '2시간 30분',
      bidCount: 15,
      category: '전자기기',
      status: 'ongoing',
    },
    {
      id: 2,
      title: '삼성 갤럭시 S23 Ultra',
      currentPrice: 720000,
      startPrice: 600000,
      timeLeft: '1시간 15분',
      bidCount: 23,
      category: '전자기기',
      status: 'ongoing',
    },
    {
      id: 3,
      title: '맥북 프로 M2 14인치',
      currentPrice: 1200000,
      startPrice: 1000000,
      timeLeft: '4시간 45분',
      bidCount: 8,
      category: '전자기기',
      status: 'ongoing',
    },
    {
      id: 4,
      title: '나이키 에어맥스 270',
      currentPrice: 85000,
      startPrice: 70000,
      timeLeft: '종료됨',
      bidCount: 12,
      category: '패션',
      status: 'ended',
    },
  ];

  const filteredAuctions = auctions.filter(auction => {
    const matchesSearch = auction.title.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === '전체' || auction.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const renderAuctionItem = ({item}) => (
    <TouchableOpacity style={styles.auctionCard}>
      <View style={styles.auctionImage}>
        <Text style={styles.auctionImagePlaceholder}>📷</Text>
      </View>
      
      <View style={styles.auctionContent}>
        <View style={styles.auctionHeader}>
          <Text style={styles.auctionTitle}>{item.title}</Text>
          <View style={[
            styles.statusBadge,
            {backgroundColor: item.status === 'ongoing' ? '#4caf50' : '#757575'}
          ]}>
            <Text style={styles.statusText}>
              {item.status === 'ongoing' ? '진행중' : '종료'}
            </Text>
          </View>
        </View>
        
        <Text style={styles.startPrice}>
          시작가: {item.startPrice.toLocaleString()}원
        </Text>
        <Text style={styles.currentPrice}>
          현재가: {item.currentPrice.toLocaleString()}원
        </Text>
        
        <View style={styles.auctionFooter}>
          <Text style={styles.bidCount}>입찰 {item.bidCount}회</Text>
          <Text style={[
            styles.timeLeft,
            {color: item.status === 'ongoing' ? '#e91e63' : '#757575'}
          ]}>
            ⏰ {item.timeLeft}
          </Text>
        </View>
      </View>
      
      {item.status === 'ongoing' && (
        <TouchableOpacity style={styles.bidButton}>
          <Text style={styles.bidButtonText}>입찰</Text>
        </TouchableOpacity>
      )}
    </TouchableOpacity>
  );

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      
      {/* 헤더 */}
      <View style={styles.header}>
        <Text style={styles.headerTitle}>경매 목록</Text>
        <TouchableOpacity style={styles.filterButton}>
          <Text style={styles.filterIcon}>🔍</Text>
        </TouchableOpacity>
      </View>

      {/* 검색바 */}
      <View style={styles.searchContainer}>
        <TextInput
          style={styles.searchInput}
          placeholder="상품명을 검색하세요"
          value={searchQuery}
          onChangeText={setSearchQuery}
        />
      </View>

      {/* 카테고리 필터 */}
      <View style={styles.categoryContainer}>
        <FlatList
          horizontal
          showsHorizontalScrollIndicator={false}
          data={categories}
          renderItem={({item}) => (
            <TouchableOpacity
              style={[
                styles.categoryButton,
                {backgroundColor: selectedCategory === item ? '#e91e63' : '#f0f0f0'}
              ]}
              onPress={() => setSelectedCategory(item)}>
              <Text style={[
                styles.categoryButtonText,
                {color: selectedCategory === item ? '#ffffff' : '#333333'}
              ]}>
                {item}
              </Text>
            </TouchableOpacity>
          )}
          keyExtractor={(item) => item}
        />
      </View>

      {/* 경매 목록 */}
      <FlatList
        data={filteredAuctions}
        renderItem={renderAuctionItem}
        keyExtractor={(item) => item.id.toString()}
        style={styles.auctionList}
        showsVerticalScrollIndicator={false}
      />
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f8f9fa',
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
  filterButton: {
    padding: 8,
  },
  filterIcon: {
    fontSize: 20,
  },
  searchContainer: {
    paddingHorizontal: 20,
    paddingVertical: 16,
    backgroundColor: '#ffffff',
  },
  searchInput: {
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#f8f9fa',
  },
  categoryContainer: {
    paddingVertical: 16,
    backgroundColor: '#ffffff',
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
  },
  categoryButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    marginHorizontal: 4,
    marginLeft: 16,
  },
  categoryButtonText: {
    fontSize: 14,
    fontWeight: '500',
  },
  auctionList: {
    flex: 1,
    paddingHorizontal: 20,
    paddingTop: 16,
  },
  auctionCard: {
    backgroundColor: '#ffffff',
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    flexDirection: 'row',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: {
      width: 0,
      height: 2,
    },
    shadowOpacity: 0.1,
    shadowRadius: 3.84,
    elevation: 5,
  },
  auctionImage: {
    width: 80,
    height: 80,
    backgroundColor: '#e0e0e0',
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 16,
  },
  auctionImagePlaceholder: {
    fontSize: 32,
  },
  auctionContent: {
    flex: 1,
  },
  auctionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  auctionTitle: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#333333',
    flex: 1,
    marginRight: 8,
  },
  statusBadge: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 12,
  },
  statusText: {
    color: '#ffffff',
    fontSize: 10,
    fontWeight: 'bold',
  },
  startPrice: {
    fontSize: 12,
    color: '#666666',
    marginBottom: 4,
  },
  currentPrice: {
    fontSize: 16,
    color: '#e91e63',
    fontWeight: 'bold',
    marginBottom: 8,
  },
  auctionFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  bidCount: {
    fontSize: 12,
    color: '#666666',
  },
  timeLeft: {
    fontSize: 12,
    fontWeight: '500',
  },
  bidButton: {
    backgroundColor: '#e91e63',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    marginLeft: 12,
  },
  bidButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: 'bold',
  },
});

export default AuctionListScreen;