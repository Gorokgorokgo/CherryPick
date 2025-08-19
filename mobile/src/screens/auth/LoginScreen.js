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
} from 'react-native';

const LoginScreen = ({navigation}) => {
  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');

  const handleLogin = () => {
    if (!phoneNumber || !password) {
      Alert.alert('알림', '전화번호와 비밀번호를 입력해주세요.');
      return;
    }

    // TODO: API 연동
    console.log('로그인 시도:', {phoneNumber, password});
    
    // 임시로 메인 화면으로 이동
    navigation.replace('Main');
  };

  const goToSignup = () => {
    navigation.navigate('Signup');
  };

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#ffffff" />
      
      <View style={styles.header}>
        <Text style={styles.logo}>🍒</Text>
        <Text style={styles.title}>CherryPick</Text>
        <Text style={styles.subtitle}>로그인하여 경매에 참여하세요</Text>
      </View>

      <View style={styles.formContainer}>
        <View style={styles.inputContainer}>
          <Text style={styles.inputLabel}>전화번호</Text>
          <TextInput
            style={styles.input}
            placeholder="전화번호를 입력하세요"
            value={phoneNumber}
            onChangeText={setPhoneNumber}
            keyboardType="phone-pad"
            maxLength={11}
          />
        </View>

        <View style={styles.inputContainer}>
          <Text style={styles.inputLabel}>비밀번호</Text>
          <TextInput
            style={styles.input}
            placeholder="비밀번호를 입력하세요"
            value={password}
            onChangeText={setPassword}
            secureTextEntry
          />
        </View>

        <TouchableOpacity style={styles.loginButton} onPress={handleLogin}>
          <Text style={styles.loginButtonText}>로그인</Text>
        </TouchableOpacity>

        <View style={styles.signupContainer}>
          <Text style={styles.signupText}>계정이 없으신가요? </Text>
          <TouchableOpacity onPress={goToSignup}>
            <Text style={styles.signupLink}>회원가입</Text>
          </TouchableOpacity>
        </View>
      </View>
    </SafeAreaView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#ffffff',
  },
  header: {
    alignItems: 'center',
    paddingTop: 50,
    paddingBottom: 40,
  },
  logo: {
    fontSize: 60,
    marginBottom: 16,
  },
  title: {
    fontSize: 28,
    fontWeight: 'bold',
    color: '#e91e63',
    marginBottom: 8,
  },
  subtitle: {
    fontSize: 16,
    color: '#666666',
  },
  formContainer: {
    flex: 1,
    paddingHorizontal: 24,
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
  loginButton: {
    backgroundColor: '#e91e63',
    borderRadius: 8,
    paddingVertical: 16,
    alignItems: 'center',
    marginTop: 20,
    marginBottom: 20,
  },
  loginButtonText: {
    color: '#ffffff',
    fontSize: 16,
    fontWeight: 'bold',
  },
  signupContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  signupText: {
    fontSize: 14,
    color: '#666666',
  },
  signupLink: {
    fontSize: 14,
    color: '#e91e63',
    fontWeight: 'bold',
  },
});

export default LoginScreen;