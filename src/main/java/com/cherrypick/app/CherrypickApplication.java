package com.cherrypick.app;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.util.TimeZone;

@EnableAsync
@EnableScheduling
@SpringBootApplication
public class CherrypickApplication {

    static {
        // .env 파일 로드
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory(".")
                    .ignoreIfMalformed()
                    .ignoreIfMissing()
                    .load();
            
            dotenv.entries().forEach(entry -> {
                System.setProperty(entry.getKey(), entry.getValue());
            });
        } catch (Exception e) {
            // .env 파일이 없어도 애플리케이션 실행 계속 (시스템 환경 변수 사용)
        }
    }

    public static void main(String[] args) {
        // 시스템 기본 시간대를 Asia/Seoul로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
        
        SpringApplication.run(CherrypickApplication.class, args);
    }
}