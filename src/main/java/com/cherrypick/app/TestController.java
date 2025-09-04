package com.cherrypick.app;

import com.cherrypick.app.domain.websocket.dto.AuctionUpdateMessage;
import com.cherrypick.app.domain.websocket.service.WebSocketMessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.math.BigDecimal;

@RestController
public class TestController {
    
    @Autowired
    private WebSocketMessagingService webSocketMessagingService;
    
    @GetMapping("/test")
    public String test() {
        return "✅ Auto Deploy Test Success!!!! " + LocalDateTime.now();
    }
    
    @GetMapping("/test/time")
    public String testTime() {
        LocalDateTime serverTime = LocalDateTime.now();
        LocalDateTime seoulTime = LocalDateTime.now(java.time.ZoneId.of("Asia/Seoul"));
        return String.format("Server Time: %s | Seoul Time: %s", serverTime, seoulTime);
    }
    
    /**
     * 다른 사용자 입찰 시뮬레이션
     * GET /test/bid/19/17000 -> 경매 19번에 17000원 입찰 시뮬레이션
     */
    @GetMapping("/test/bid/{auctionId}/{bidAmount}")
    public String simulateOtherUserBid(
            @PathVariable Long auctionId,
            @PathVariable Integer bidAmount) {
        
        try {
            // 다른 사용자 입찰 시뮬레이션
            webSocketMessagingService.notifyNewBid(
                auctionId,
                BigDecimal.valueOf(bidAmount),
                10, // 가상 입찰 횟수
                "테스터" + (int)(Math.random() * 100) // 랜덤 닉네임
            );
            
            return String.format("✅ 경매 %d번에 %d원 입찰 시뮬레이션 완료! 앱에서 실시간 업데이트를 확인하세요.", auctionId, bidAmount);
            
        } catch (Exception e) {
            return "❌ 오류: " + e.getMessage();
        }
    }
    
    /**
     * 연속 입찰 시뮬레이션 (경매 열기 효과)
     */
    @GetMapping("/test/bid-war/{auctionId}")
    public String simulateBidWar(@PathVariable Long auctionId) {
        
        try {
            // 3명의 다른 사용자가 연속 입찰하는 시뮬레이션
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    webSocketMessagingService.notifyNewBid(auctionId, BigDecimal.valueOf(17000), 11, "입찰왕A");
                    
                    Thread.sleep(3000);
                    webSocketMessagingService.notifyNewBid(auctionId, BigDecimal.valueOf(17500), 12, "입찰고수B");
                    
                    Thread.sleep(2000);
                    webSocketMessagingService.notifyNewBid(auctionId, BigDecimal.valueOf(18000), 13, "최고가C");
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
            
            return String.format("🔥 경매 %d번 입찰 경쟁 시뮬레이션 시작! 7초간 3번의 입찰이 발생합니다.", auctionId);
            
        } catch (Exception e) {
            return "❌ 오류: " + e.getMessage();
        }
    }
}