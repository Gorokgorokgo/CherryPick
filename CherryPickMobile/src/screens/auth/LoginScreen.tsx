import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  Alert,
  KeyboardAvoidingView,
  Platform,
  ScrollView,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { COLORS, SIZES } from '../../constants';
import { Button } from '../../components/common/Button';
import ApiService from '../../services/api';
import SocketService from '../../services/socket';

interface LoginScreenProps {
  onLoginSuccess: () => void;
  onSignupPress: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  onLoginSuccess,
  onSignupPress,
}) => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleLogin = async () => {
    if (!phoneNumber.trim() || !password.trim()) {
      Alert.alert('오류', '전화번호와 비밀번호를 입력해주세요.');
      return;
    }

    setLoading(true);
    try {
      const response = await ApiService.login(phoneNumber, password);
      
      if (response.success && response.data.token) {
        // Save token
        await AsyncStorage.setItem('jwt_token', response.data.token);
        await AsyncStorage.setItem('user_info', JSON.stringify(response.data.user));
        
        // Connect socket
        await SocketService.connect();
        
        onLoginSuccess();
      } else {
        Alert.alert('로그인 실패', response.message || '로그인에 실패했습니다.');
      }
    } catch (error: any) {
      console.error('Login error:', error);
      Alert.alert('로그인 실패', error.response?.data?.message || '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  const formatPhoneNumber = (text: string) => {
    // Remove all non-numeric characters
    const numbers = text.replace(/[^0-9]/g, '');
    
    // Format as 010-0000-0000
    if (numbers.length <= 3) {
      return numbers;
    } else if (numbers.length <= 7) {
      return `${numbers.slice(0, 3)}-${numbers.slice(3)}`;
    } else if (numbers.length <= 11) {
      return `${numbers.slice(0, 3)}-${numbers.slice(3, 7)}-${numbers.slice(7, 11)}`;
    }
    return numbers;
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <ScrollView contentContainerStyle={styles.scrollContainer}>
        <View style={styles.header}>
          <Text style={styles.logo}>🍒</Text>
          <Text style={styles.title}>CherryPick</Text>
          <Text style={styles.subtitle}>로그인하여 경매에 참여하세요</Text>
        </View>

        <View style={styles.form}>
          <View style={styles.inputContainer}>
            <Text style={styles.label}>전화번호</Text>
            <TextInput
              style={styles.input}
              value={phoneNumber}
              onChangeText={(text) => setPhoneNumber(formatPhoneNumber(text))}
              placeholder="010-0000-0000"
              keyboardType="phone-pad"
              maxLength={13}
              autoCapitalize="none"
            />
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.label}>비밀번호</Text>
            <TextInput
              style={styles.input}
              value={password}
              onChangeText={setPassword}
              placeholder="비밀번호를 입력하세요"
              secureTextEntry
              autoCapitalize="none"
            />
          </View>

          <Button
            title="로그인"
            onPress={handleLogin}
            loading={loading}
            disabled={!phoneNumber.trim() || !password.trim()}
            style={styles.loginButton}
          />

          <View style={styles.divider}>
            <View style={styles.dividerLine} />
            <Text style={styles.dividerText}>또는</Text>
            <View style={styles.dividerLine} />
          </View>

          <Button
            title="회원가입"
            onPress={onSignupPress}
            variant="outline"
            style={styles.signupButton}
          />
        </View>

        <View style={styles.footer}>
          <Text style={styles.footerText}>
            MVP 5주차 버전 - 실시간 채팅 기능 추가
          </Text>
          <Text style={styles.footerSubtext}>
            현재 무료 프로모션: 연결 서비스 수수료 0%
          </Text>
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
  scrollContainer: {
    flexGrow: 1,
    padding: SIZES.PADDING * 2,
  },
  header: {
    alignItems: 'center',
    marginTop: SIZES.MARGIN * 2,
    marginBottom: SIZES.MARGIN * 3,
  },
  logo: {
    fontSize: 60,
    marginBottom: SIZES.MARGIN,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: COLORS.PRIMARY,
    marginBottom: SIZES.MARGIN / 2,
  },
  subtitle: {
    fontSize: 16,
    color: COLORS.TEXT_SECONDARY,
    textAlign: 'center',
  },
  form: {
    flex: 1,
  },
  inputContainer: {
    marginBottom: SIZES.MARGIN * 1.5,
  },
  label: {
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
    paddingVertical: SIZES.PADDING,
    fontSize: 16,
    backgroundColor: COLORS.BACKGROUND,
  },
  loginButton: {
    marginTop: SIZES.MARGIN,
  },
  divider: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: SIZES.MARGIN * 2,
  },
  dividerLine: {
    flex: 1,
    height: 1,
    backgroundColor: COLORS.BORDER,
  },
  dividerText: {
    marginHorizontal: SIZES.MARGIN,
    color: COLORS.TEXT_SECONDARY,
    fontSize: 14,
  },
  signupButton: {
    marginBottom: SIZES.MARGIN * 2,
  },
  footer: {
    alignItems: 'center',
    marginTop: 'auto',
    paddingTop: SIZES.MARGIN * 2,
  },
  footerText: {
    fontSize: 12,
    color: COLORS.TEXT_SECONDARY,
    textAlign: 'center',
    marginBottom: 4,
  },
  footerSubtext: {
    fontSize: 11,
    color: COLORS.SUCCESS,
    textAlign: 'center',
  },
});