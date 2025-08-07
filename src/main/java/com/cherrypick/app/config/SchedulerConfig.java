package com.cherrypick.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄러 설정 클래스
 * 
 * 주요 기능:
 * - 경매 종료 자동 처리
 * - 낙찰자 결정 및 연결 서비스 생성
 * - 정기적인 시스템 정리 작업
 */
@Configuration
@EnableScheduling
public class SchedulerConfig {
    // 기본 스케줄러 설정
    // ThreadPoolTaskScheduler 커스텀이 필요한 경우 추가 구현
}