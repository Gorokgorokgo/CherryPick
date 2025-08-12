import 'react-native-gesture-handler';
import React from 'react';
import { StatusBar } from 'react-native';
import RootNavigator from './src/navigation';
import { COLORS } from './src/constants';

/**
 * CherryPick Mobile App
 * MVP 5주차 - 실시간 채팅 시스템 및 기본 화면 구성
 */
function App(): React.JSX.Element {
  return (
    <>
      <StatusBar
        barStyle="light-content"
        backgroundColor={COLORS.PRIMARY}
        translucent={false}
      />
      <RootNavigator />
    </>
  );
}

export default App;
