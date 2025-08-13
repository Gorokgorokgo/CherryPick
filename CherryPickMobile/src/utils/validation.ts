// Form validation utilities
export interface ValidationResult {
  isValid: boolean;
  error?: string;
}

export const ValidationRules = {
  // Required field validation
  required: (value: string | null | undefined, fieldName: string): ValidationResult => {
    if (!value || value.toString().trim() === '') {
      return { isValid: false, error: `${fieldName}을(를) 입력해주세요.` };
    }
    return { isValid: true };
  },

  // Text length validation
  minLength: (value: string, min: number, fieldName: string): ValidationResult => {
    if (value.length < min) {
      return { isValid: false, error: `${fieldName}은(는) 최소 ${min}자 이상이어야 합니다.` };
    }
    return { isValid: true };
  },

  maxLength: (value: string, max: number, fieldName: string): ValidationResult => {
    if (value.length > max) {
      return { isValid: false, error: `${fieldName}은(는) 최대 ${max}자까지 입력 가능합니다.` };
    }
    return { isValid: true };
  },

  // Number validation
  isNumber: (value: string, fieldName: string): ValidationResult => {
    if (isNaN(Number(value))) {
      return { isValid: false, error: `${fieldName}은(는) 숫자만 입력 가능합니다.` };
    }
    return { isValid: true };
  },

  minValue: (value: number, min: number, fieldName: string): ValidationResult => {
    if (value < min) {
      return { isValid: false, error: `${fieldName}은(는) ${min} 이상이어야 합니다.` };
    }
    return { isValid: true };
  },

  maxValue: (value: number, max: number, fieldName: string): ValidationResult => {
    if (value > max) {
      return { isValid: false, error: `${fieldName}은(는) ${max} 이하여야 합니다.` };
    }
    return { isValid: true };
  },

  // Price validation (Korean won)
  priceFormat: (value: string, fieldName: string): ValidationResult => {
    const numValue = Number(value.replace(/[^0-9]/g, ''));
    if (isNaN(numValue) || numValue < 0) {
      return { isValid: false, error: `${fieldName}을(를) 올바르게 입력해주세요.` };
    }
    if (numValue > 100000000) { // 1억원 제한
      return { isValid: false, error: `${fieldName}은(는) 1억원을 초과할 수 없습니다.` };
    }
    return { isValid: true };
  },

  // Phone number validation (Korean format)
  phoneNumber: (value: string): ValidationResult => {
    const phoneRegex = /^010-?([0-9]{4})-?([0-9]{4})$/;
    if (!phoneRegex.test(value)) {
      return { isValid: false, error: '올바른 전화번호 형식이 아닙니다. (예: 010-1234-5678)' };
    }
    return { isValid: true };
  },

  // Nickname validation
  nickname: (value: string): ValidationResult => {
    if (value.length < 2) {
      return { isValid: false, error: '닉네임은 최소 2자 이상이어야 합니다.' };
    }
    if (value.length > 10) {
      return { isValid: false, error: '닉네임은 최대 10자까지 가능합니다.' };
    }
    const nicknameRegex = /^[가-힣a-zA-Z0-9_]+$/;
    if (!nicknameRegex.test(value)) {
      return { isValid: false, error: '닉네임은 한글, 영문, 숫자, 밑줄(_)만 사용 가능합니다.' };
    }
    return { isValid: true };
  },

  // Image validation
  imageArray: (images: string[], min = 1, max = 5): ValidationResult => {
    if (images.length < min) {
      return { isValid: false, error: `최소 ${min}장의 이미지를 업로드해주세요.` };
    }
    if (images.length > max) {
      return { isValid: false, error: `최대 ${max}장까지 업로드 가능합니다.` };
    }
    return { isValid: true };
  },
};

// Auction form specific validation
export const AuctionValidation = {
  title: (title: string): ValidationResult => {
    const requiredResult = ValidationRules.required(title, '제목');
    if (!requiredResult.isValid) return requiredResult;
    
    return ValidationRules.maxLength(title, 50, '제목');
  },

  description: (description: string): ValidationResult => {
    const requiredResult = ValidationRules.required(description, '상품 설명');
    if (!requiredResult.isValid) return requiredResult;
    
    const minResult = ValidationRules.minLength(description, 10, '상품 설명');
    if (!minResult.isValid) return minResult;
    
    return ValidationRules.maxLength(description, 1000, '상품 설명');
  },

  category: (category: string): ValidationResult => {
    return ValidationRules.required(category, '카테고리');
  },

  startPrice: (startPrice: string): ValidationResult => {
    const requiredResult = ValidationRules.required(startPrice, '시작가');
    if (!requiredResult.isValid) return requiredResult;
    
    const priceResult = ValidationRules.priceFormat(startPrice, '시작가');
    if (!priceResult.isValid) return priceResult;
    
    const numValue = Number(startPrice.replace(/[^0-9]/g, ''));
    return ValidationRules.minValue(numValue, 1000, '시작가');
  },

  buyNowPrice: (buyNowPrice: string, startPrice: string): ValidationResult => {
    if (!buyNowPrice) return { isValid: true }; // Optional field
    
    const priceResult = ValidationRules.priceFormat(buyNowPrice, '즉시구매가');
    if (!priceResult.isValid) return priceResult;
    
    const buyNowValue = Number(buyNowPrice.replace(/[^0-9]/g, ''));
    const startValue = Number(startPrice.replace(/[^0-9]/g, ''));
    
    if (buyNowValue <= startValue) {
      return { isValid: false, error: '즉시구매가는 시작가보다 높아야 합니다.' };
    }
    
    return { isValid: true };
  },

  region: (region: string): ValidationResult => {
    const requiredResult = ValidationRules.required(region, '거래 지역');
    if (!requiredResult.isValid) return requiredResult;
    
    return ValidationRules.maxLength(region, 20, '거래 지역');
  },

  images: (images: string[]): ValidationResult => {
    return ValidationRules.imageArray(images, 1, 5);
  },
};

// Validation helper function
export const validateForm = <T extends Record<string, any>>(
  data: T,
  validators: Record<keyof T, (value: any, data?: T) => ValidationResult>
): { isValid: boolean; errors: Partial<Record<keyof T, string>> } => {
  const errors: Partial<Record<keyof T, string>> = {};
  let isValid = true;

  Object.keys(validators).forEach((key) => {
    const validator = validators[key as keyof T];
    const result = validator(data[key as keyof T], data);
    
    if (!result.isValid) {
      errors[key as keyof T] = result.error;
      isValid = false;
    }
  });

  return { isValid, errors };
};

// Format utilities
export const formatPrice = (price: number | string): string => {
  const numPrice = typeof price === 'string' ? Number(price) : price;
  return new Intl.NumberFormat('ko-KR').format(numPrice);
};

export const formatPhoneNumber = (phoneNumber: string): string => {
  const cleaned = phoneNumber.replace(/\D/g, '');
  const match = cleaned.match(/^(\d{3})(\d{4})(\d{4})$/);
  
  if (match) {
    return `${match[1]}-${match[2]}-${match[3]}`;
  }
  
  return phoneNumber;
};