import React, {useState} from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  SafeAreaView,
  Alert,
  StatusBar,
  ScrollView,
} from 'react-native';

const SignupScreen = ({navigation}) => {
  const [formData, setFormData] = useState({
    phoneNumber: '',
    password: '',
    confirmPassword: '',
    nickname: '',
    verificationCode: '',
  });
  const [isCodeSent, setIsCodeSent] = useState(false);

  const updateFormData = (field, value) => {
    setFormData(prev => ({...prev, [field]: value}));
  };

  const sendVerificationCode = () => {
    if (!formData.phoneNumber) {
      Alert.alert('알림', '전화번호를 입력해주세요.');
      return;
    }

    // TODO: API 연동
    console.log('인증번호 전송:', formData.phoneNumber);
    setIsCodeSent(true);
    Alert.alert('알림', '인증번호가 전송되었습니다.');
  };

  const handleSignup = () => {
    if (!formData.phoneNumber || !formData.password || !formData.nickname) {
      Alert.alert('알림', '모든 필드를 입력해주세요.');
      return;
    }

    if (formData.password !== formData.confirmPassword) {
      Alert.alert('알림', '비밀번호가 일치하지 않습니다.');
      return;
    }

    if (!isCodeSent || !formData.verificationCode) {
      Alert.alert('알림', '전화번호 인증을 완료해주세요.');
      return;
    }

    // TODO: API 연동
    console.log('회원가입 시도:', formData);
    Alert.alert('성공', '회원가입이 완료되었습니다.', [
      {text: '확인', onPress: () => navigation.replace('Login')},
    ]);
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      
      <ScrollView style={styles.scrollView}>
        <View style={styles.header}>
          <TouchableOpacity 
            style={styles.backButton}
            onPress={() => navigation.goBack()}>
            <Text style={styles.backButtonText}>← 뒤로</Text>
          </TouchableOpacity>
          <Text style={styles.title}>회원가입</Text>
          <Text style={styles.subtitle}>CherryPick에 오신 것을 환영합니다</Text>
        </View>

        <View style={styles.formContainer}>
          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>전화번호</Text>
            <View style={styles.phoneInputContainer}>
              <TextInput
                style={[styles.input, styles.phoneInput]}
                placeholder="전화번호를 입력하세요"
                value={formData.phoneNumber}
                onChangeText={(value) => updateFormData('phoneNumber', value)}
                keyboardType="phone-pad"
                maxLength={11}
              />
              <TouchableOpacity 
                style={styles.verifyButton}
                onPress={sendVerificationCode}>
                <Text style={styles.verifyButtonText}>
                  {isCodeSent ? '재전송' : '인증'}
                </Text>
              </TouchableOpacity>
            </View>
          </View>

          {isCodeSent && (
            <View style={styles.inputContainer}>
              <Text style={styles.inputLabel}>인증번호</Text>
              <TextInput
                style={styles.input}
                placeholder="인증번호 6자리를 입력하세요"
                value={formData.verificationCode}
                onChangeText={(value) => updateFormData('verificationCode', value)}
                keyboardType="number-pad"
                maxLength={6}
              />
            </View>
          )}

          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>닉네임</Text>
            <TextInput
              style={styles.input}
              placeholder="닉네임을 입력하세요"
              value={formData.nickname}
              onChangeText={(value) => updateFormData('nickname', value)}
              maxLength={20}
            />
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>비밀번호</Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호를 입력하세요"
              value={formData.password}
              onChangeText={(value) => updateFormData('password', value)}
              secureTextEntry
            />
          </View>

          <View style={styles.inputContainer}>
            <Text style={styles.inputLabel}>비밀번호 확인</Text>
            <TextInput
              style={styles.input}
              placeholder="비밀번호를 다시 입력하세요"
              value={formData.confirmPassword}
              onChangeText={(value) => updateFormData('confirmPassword', value)}
              secureTextEntry
            />
          </View>

          <TouchableOpacity style={styles.signupButton} onPress={handleSignup}>
            <Text style={styles.signupButtonText}>회원가입</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  scrollView: {
    flex: 1,
  },
  header: {
    paddingHorizontal: 24,
    paddingTop: 20,
    paddingBottom: 30,
  },
  backButton: {
    marginBottom: 20,
  },
  backButtonText: {
    fontSize: 16,
    color: '#e91e63',
    fontWeight: '500',
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#333333',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666666',
  },
  formContainer: {
    paddingHorizontal: 24,
    paddingBottom: 30,
  },
  inputContainer: {
    marginBottom: 20,
  },
  inputLabel: {
    fontSize: 14,
    fontWeight: '500',
    color: '#333333',
    marginBottom: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 8,
    paddingHorizontal: 16,
    paddingVertical: 12,
    fontSize: 16,
    backgroundColor: '#ffffff',
  },
  phoneInputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  phoneInput: {
    flex: 1,
    marginRight: 12,
  },
  verifyButton: {
    backgroundColor: '#e91e63',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
  },
  verifyButtonText: {
    color: '#ffffff',
    fontSize: 14,
    fontWeight: 'bold',
  },
  signupButton: {
    backgroundColor: '#e91e63',
    borderRadius: 8,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 20,
  },
  signupButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});

export default SignupScreen;