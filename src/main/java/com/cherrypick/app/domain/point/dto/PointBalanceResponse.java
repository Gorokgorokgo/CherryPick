package com.cherrypick.app.domain.point.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PointBalanceResponse {
    
    private Long userId;
    private Long balance;
    private Long lockedAmount; // 예치된(잠긴) 금액
    private Long availableAmount; // 사용 가능한 금액
    
    public static PointBalanceResponse of(Long userId, Long balance, Long lockedAmount) {
        Long availableAmount = balance - lockedAmount;
        return new PointBalanceResponse(userId, balance, lockedAmount, availableAmount);
    }
}