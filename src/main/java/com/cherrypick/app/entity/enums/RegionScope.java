package com.cherrypick.app.entity.enums;

/**
 * 지역 범위 열거형
 */
public enum RegionScope {
    DISTRICT("구/군"),
    CITY("시/도"),
    NATIONAL("전국");

    private final String description;

    RegionScope(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}