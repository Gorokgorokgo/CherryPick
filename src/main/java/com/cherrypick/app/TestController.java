package com.cherrypick.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
public class TestController {
    
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
}