import React, { useState } from 'react';
import {
  View,
  Text,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Image,
  Alert,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Dimensions,
} from 'react-native';
import { launchImageLibrary, MediaType } from 'react-native-image-picker';
import { COLORS, SIZES } from '../../constants';
import { Button } from '../../components/common/Button';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import ApiService from '../../services/api';

const { width } = Dimensions.get('window');

interface AuctionCreateScreenProps {
  onCreateSuccess: () => void;
  onBackPress: () => void;
}

interface AuctionFormData {
  title: string;
  description: string;
  category: string;
  startPrice: string;
  buyNowPrice: string;
  auctionDuration: number; // hours
  region: string;
  images: string[];
}

const CATEGORIES = [
  '전자제품', '의류/패션', '생활용품', '스포츠/레저',
  '도서/음반', '가구/인테리어', '유아용품', '기타'
];

const AUCTION_DURATIONS = [
  { label: '1시간', value: 1 },
  { label: '3시간', value: 3 },
  { label: '6시간', value: 6 },
  { label: '12시간', value: 12 },
  { label: '1일', value: 24 },
  { label: '3일', value: 72 },
  { label: '7일', value: 168 },
];

export const AuctionCreateScreen: React.FC<AuctionCreateScreenProps> = ({
  onCreateSuccess,
  onBackPress,
}) => {
  const [loading, setLoading] = useState(false);
  const [formData, setFormData] = useState<AuctionFormData>({
    title: '',
    description: '',
    category: '',
    startPrice: '',
    buyNowPrice: '',
    auctionDuration: 24,
    region: '',
    images: [],
  });

  const updateFormData = (field: keyof AuctionFormData, value: any) => {
    setFormData(prev => ({
      ...prev,
      [field]: value,
    }));
  };

  const handleImagePicker = () => {
    if (formData.images.length >= 5) {
      Alert.alert('알림', '최대 5장까지 업로드할 수 있습니다.');
      return;
    }

    const options = {
      mediaType: 'photo' as MediaType,
      quality: 0.8,
      maxWidth: 1024,
      maxHeight: 1024,
      includeBase64: false,
    };

    launchImageLibrary(options, (response) => {
      if (response.didCancel || response.errorMessage) {
        return;
      }

      if (response.assets && response.assets[0]) {
        const asset = response.assets[0];
        if (asset.uri) {
          updateFormData('images', [...formData.images, asset.uri]);
        }
      }
    });
  };

  const removeImage = (index: number) => {
    const newImages = formData.images.filter((_, i) => i !== index);
    updateFormData('images', newImages);
  };

  const validateForm = (): boolean => {
    if (!formData.title.trim()) {
      Alert.alert('오류', '제목을 입력해주세요.');
      return false;
    }
    if (!formData.description.trim()) {
      Alert.alert('오류', '상품 설명을 입력해주세요.');
      return false;
    }
    if (!formData.category) {
      Alert.alert('오류', '카테고리를 선택해주세요.');
      return false;
    }
    if (!formData.startPrice || isNaN(Number(formData.startPrice))) {
      Alert.alert('오류', '시작가를 올바르게 입력해주세요.');
      return false;
    }
    if (formData.buyNowPrice && isNaN(Number(formData.buyNowPrice))) {
      Alert.alert('오류', '즉시구매가를 올바르게 입력해주세요.');
      return false;
    }
    if (formData.buyNowPrice && Number(formData.buyNowPrice) <= Number(formData.startPrice)) {
      Alert.alert('오류', '즉시구매가는 시작가보다 높아야 합니다.');
      return false;
    }
    if (!formData.region.trim()) {
      Alert.alert('오류', '거래 지역을 입력해주세요.');
      return false;
    }
    if (formData.images.length === 0) {
      Alert.alert('오류', '최소 1장의 상품 이미지를 업로드해주세요.');
      return false;
    }
    return true;
  };

  const handleSubmit = async () => {
    if (!validateForm()) return;

    try {
      setLoading(true);
      
      // 1. 이미지 업로드
      let imageUrls: string[] = [];
      if (formData.images.length > 0) {
        try {
          const uploadResults = await ApiService.uploadImages(formData.images);
          imageUrls = uploadResults.map(result => result.imageUrl);
        } catch (uploadError) {
          console.error('Image upload failed:', uploadError);
          Alert.alert('오류', '이미지 업로드에 실패했습니다. 다시 시도해주세요.');
          return;
        }
      }

      // 2. 경매 데이터 생성
      const auctionData = {
        title: formData.title.trim(),
        description: formData.description.trim(),
        category: formData.category,
        startPrice: Number(formData.startPrice),
        buyNowPrice: formData.buyNowPrice ? Number(formData.buyNowPrice) : null,
        auctionDuration: formData.auctionDuration,
        region: formData.region.trim(),
        images: imageUrls,
      };

      // 3. 경매 등록 API 호출
      await ApiService.createAuction(auctionData);
      
      Alert.alert(
        '성공',
        '경매가 성공적으로 등록되었습니다!',
        [
          {
            text: '확인',
            onPress: onCreateSuccess,
          },
        ]
      );
    } catch (error) {
      console.error('Failed to create auction:', error);
      Alert.alert('오류', '경매 등록에 실패했습니다. 다시 시도해주세요.');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <LoadingSpinner message="경매를 등록하는 중..." />;
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.header}>
        <TouchableOpacity onPress={onBackPress} style={styles.backButton}>
          <Text style={styles.backButtonText}>← 뒤로</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>경매 등록</Text>
        <View style={styles.placeholder} />
      </View>

      <ScrollView style={styles.scrollContainer} showsVerticalScrollIndicator={false}>
        {/* 이미지 업로드 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>상품 사진 *</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={styles.imageList}>
              {formData.images.map((imageUri, index) => (
                <View key={index} style={styles.imageContainer}>
                  <Image source={{ uri: imageUri }} style={styles.uploadedImage} />
                  <TouchableOpacity
                    style={styles.removeImageButton}
                    onPress={() => removeImage(index)}
                  >
                    <Text style={styles.removeImageText}>×</Text>
                  </TouchableOpacity>
                </View>
              ))}
              {formData.images.length < 5 && (
                <TouchableOpacity
                  style={styles.imagePickerButton}
                  onPress={handleImagePicker}
                >
                  <Text style={styles.imagePickerText}>📷</Text>
                  <Text style={styles.imagePickerLabel}>
                    {formData.images.length}/5
                  </Text>
                </TouchableOpacity>
              )}
            </View>
          </ScrollView>
        </View>

        {/* 제목 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>제목 *</Text>
          <TextInput
            style={styles.input}
            placeholder="상품 제목을 입력하세요"
            value={formData.title}
            onChangeText={(text) => updateFormData('title', text)}
            maxLength={50}
          />
          <Text style={styles.charCount}>{formData.title.length}/50</Text>
        </View>

        {/* 카테고리 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>카테고리 *</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={styles.categoryList}>
              {CATEGORIES.map((category) => (
                <TouchableOpacity
                  key={category}
                  style={[
                    styles.categoryChip,
                    formData.category === category && styles.selectedCategoryChip,
                  ]}
                  onPress={() => updateFormData('category', category)}
                >
                  <Text
                    style={[
                      styles.categoryText,
                      formData.category === category && styles.selectedCategoryText,
                    ]}
                  >
                    {category}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </ScrollView>
        </View>

        {/* 설명 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>상품 설명 *</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            placeholder="상품에 대한 자세한 설명을 입력하세요&#10;• 상품의 상태&#10;• 구매 시기&#10;• 사용 빈도 등"
            value={formData.description}
            onChangeText={(text) => updateFormData('description', text)}
            multiline
            numberOfLines={5}
            textAlignVertical="top"
            maxLength={1000}
          />
          <Text style={styles.charCount}>{formData.description.length}/1000</Text>
        </View>

        {/* 가격 설정 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>가격 설정</Text>
          <View style={styles.priceContainer}>
            <View style={styles.priceInput}>
              <Text style={styles.priceLabel}>시작가 *</Text>
              <TextInput
                style={styles.input}
                placeholder="0"
                value={formData.startPrice}
                onChangeText={(text) => updateFormData('startPrice', text.replace(/[^0-9]/g, ''))}
                keyboardType="numeric"
              />
              <Text style={styles.won}>원</Text>
            </View>
            <View style={styles.priceInput}>
              <Text style={styles.priceLabel}>즉시구매가</Text>
              <TextInput
                style={styles.input}
                placeholder="설정 안함"
                value={formData.buyNowPrice}
                onChangeText={(text) => updateFormData('buyNowPrice', text.replace(/[^0-9]/g, ''))}
                keyboardType="numeric"
              />
              <Text style={styles.won}>원</Text>
            </View>
          </View>
        </View>

        {/* 경매 기간 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>경매 기간</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            <View style={styles.durationList}>
              {AUCTION_DURATIONS.map((duration) => (
                <TouchableOpacity
                  key={duration.value}
                  style={[
                    styles.durationChip,
                    formData.auctionDuration === duration.value && styles.selectedDurationChip,
                  ]}
                  onPress={() => updateFormData('auctionDuration', duration.value)}
                >
                  <Text
                    style={[
                      styles.durationText,
                      formData.auctionDuration === duration.value && styles.selectedDurationText,
                    ]}
                  >
                    {duration.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          </ScrollView>
        </View>

        {/* 거래 지역 */}
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>거래 지역 *</Text>
          <TextInput
            style={styles.input}
            placeholder="예: 서울시 강남구"
            value={formData.region}
            onChangeText={(text) => updateFormData('region', text)}
            maxLength={20}
          />
        </View>

        {/* 등록 버튼 */}
        <View style={styles.submitSection}>
          <Button
            title="경매 등록하기"
            onPress={handleSubmit}
            loading={loading}
            disabled={loading}
            variant="primary"
            size="large"
          />
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: COLORS.BACKGROUND,
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
  scrollContainer: {
    flex: 1,
  },
  section: {
    paddingHorizontal: SIZES.PADDING,
    paddingVertical: SIZES.PADDING / 2,
    borderBottomWidth: 1,
    borderBottomColor: COLORS.BORDER,
  },
  sectionTitle: {
    fontSize: 16,
    fontWeight: '600',
    color: COLORS.TEXT_PRIMARY,
    marginBottom: SIZES.MARGIN / 2,
  },
  input: {
    borderWidth: 1,
    borderColor: COLORS.BORDER,
    borderRadius: SIZES.BORDER_RADIUS,
    paddingHorizontal: SIZES.PADDING,
    paddingVertical: 12,
    fontSize: 16,
    color: COLORS.TEXT_PRIMARY,
    backgroundColor: COLORS.BACKGROUND,
  },
  textArea: {
    height: 120,
  },
  charCount: {
    textAlign: 'right',
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    marginTop: 4,
  },
  imageList: {
    flexDirection: 'row',
    paddingVertical: 8,
  },
  imageContainer: {
    position: 'relative',
    marginRight: 12,
  },
  uploadedImage: {
    width: 80,
    height: 80,
    borderRadius: SIZES.BORDER_RADIUS,
  },
  removeImageButton: {
    position: 'absolute',
    top: -8,
    right: -8,
    backgroundColor: COLORS.ERROR,
    width: 24,
    height: 24,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
  },
  removeImageText: {
    color: COLORS.BACKGROUND,
    fontSize: 16,
    fontWeight: 'bold',
  },
  imagePickerButton: {
    width: 80,
    height: 80,
    borderWidth: 2,
    borderColor: COLORS.BORDER,
    borderStyle: 'dashed',
    borderRadius: SIZES.BORDER_RADIUS,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: COLORS.SURFACE,
  },
  imagePickerText: {
    fontSize: 24,
    marginBottom: 4,
  },
  imagePickerLabel: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
  },
  categoryList: {
    flexDirection: 'row',
    paddingVertical: 8,
  },
  categoryChip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: COLORS.SURFACE,
    borderRadius: 20,
    marginRight: 8,
    borderWidth: 1,
    borderColor: COLORS.BORDER,
  },
  selectedCategoryChip: {
    backgroundColor: COLORS.PRIMARY,
    borderColor: COLORS.PRIMARY,
  },
  categoryText: {
    fontSize: 14,
    color: COLORS.TEXT_PRIMARY,
  },
  selectedCategoryText: {
    color: COLORS.BACKGROUND,
    fontWeight: '600',
  },
  priceContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  priceInput: {
    flex: 1,
    marginRight: SIZES.MARGIN,
  },
  priceLabel: {
    fontSize: 14,
    color: COLORS.TEXT_SECONDARY,
    marginBottom: 8,
  },
  won: {
    position: 'absolute',
    right: 16,
    top: 36,
    fontSize: 16,
    color: COLORS.TEXT_SECONDARY,
  },
  durationList: {
    flexDirection: 'row',
    paddingVertical: 8,
  },
  durationChip: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: COLORS.SURFACE,
    borderRadius: 20,
    marginRight: 8,
    borderWidth: 1,
    borderColor: COLORS.BORDER,
  },
  selectedDurationChip: {
    backgroundColor: COLORS.PRIMARY,
    borderColor: COLORS.PRIMARY,
  },
  durationText: {
    fontSize: 14,
    color: COLORS.TEXT_PRIMARY,
  },
  selectedDurationText: {
    color: COLORS.BACKGROUND,
    fontWeight: '600',
  },
  submitSection: {
    padding: SIZES.PADDING,
    paddingBottom: SIZES.PADDING * 2,
  },
});