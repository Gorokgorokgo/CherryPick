package com.cherrypick.app.domain.common.enums;

/**
 * 이미지 상태 Enum
 *
 * TEMP: 임시 업로드 상태 (경매 등록 전)
 * PERMANENT: 영구 저장 상태 (경매 등록 완료)
 */
public enum ImageStatus {
    TEMP,       // 임시 업로드
    PERMANENT   // 영구 저장
}
