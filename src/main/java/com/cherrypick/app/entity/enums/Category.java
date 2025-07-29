package com.cherrypick.app.entity.enums;

/**
 * 상품 카테고리 열거형
 */
public enum Category {
    ELECTRONICS("전자제품"),
    FASHION("패션/의류"),
    HOME_APPLIANCES("가전제품"),
    BOOKS("도서"),
    SPORTS("스포츠/레저"),
    BEAUTY("뷰티/화장품"),
    AUTOMOTIVE("자동차용품"),
    FURNITURE("가구/인테리어"),
    BABY("유아용품"),
    PET("반려동물용품"),
    ETC("기타");

    private final String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}