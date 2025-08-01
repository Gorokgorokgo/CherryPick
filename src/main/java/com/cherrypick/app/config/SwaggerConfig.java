package com.cherrypick.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CherryPick API")
                        .description("""
                            ## 중고물품 경매 서비스 API 문서
                            
                            CherryPick은 중고물품을 경매 방식으로 거래할 수 있는 플랫폼입니다.
                            
                            ### 주요 기능
                            - 🔐 **인증/인가**: 전화번호 기반 회원가입/로그인
                            - 🏷️ **경매 관리**: 경매 등록, 조회, 검색
                            - 💰 **입찰 시스템**: 실시간 입찰, 자동 입찰
                            - 💳 **포인트 시스템**: 충전, 출금, 거래 내역
                            - 🏦 **계좌 관리**: 다중 계좌 등록 및 관리
                            - 📷 **이미지 업로드**: AWS S3 Storage 연동
                            
                            ### 인증 방법
                            1. `/api/auth/send-code`로 인증 코드 발송
                            2. `/api/auth/verify-code`로 코드 검증
                            3. `/api/auth/signup` 또는 `/api/auth/login`으로 JWT 토큰 획득
                            4. 이후 모든 API 요청시 `Authorization: Bearer {token}` 헤더 필요
                            
                            ### 비즈니스 규칙
                            - 💰 **보증금 시스템**: 경매 등록시 희망가의 10% 선차감
                            - 🔒 **포인트 예치**: 입찰시 해당 금액 잠금, 새로운 최고가 입찰시 해제
                            - 📊 **최소 단위**: 모든 금액은 1,000원 단위
                            
                            ### 💸 수수료 정책 (환경변수 기반 동적 조절)
                            
                            #### 기본 수수료 체계
                            - **기본 수수료율**: 환경변수 `COMMISSION_RATE`로 설정 (기본 3%)
                            - **신규 사용자 혜택**: 가입 후 30일간 수수료 무료 (`NEW_USER_FREE_DAYS`)
                            - **프로모션 기간**: 특별 이벤트 기간 중 수수료 할인/무료 적용
                            
                            #### 판매자 레벨별 수수료 할인
                            - **Lv 10+**: 0.2% 할인 (수수료 2.8%)
                            - **Lv 30+**: 0.3% 할인 (수수료 2.7%)
                            - **Lv 50+**: 0.4% 할인 (수수료 2.6%)
                            - **Lv 70+**: 0.6% 할인 (수수료 2.4%)
                            - **Lv 90+**: 1.2% 할인 (수수료 1.8%)
                            - **Lv 100**: 1.8% 할인 (수수료 1.2%)
                            
                            #### 🛡️ 마이너스 수수료 방지
                            - 할인율이 기본 수수료율을 초과하는 경우 자동으로 0%로 조정
                            - 프로모션 기간 중에는 레벨 할인 적용하지 않음
                            
                            #### 2025년 특별 정책
                            - **전체 무료**: 2025년 12월 31일까지 모든 거래 수수료 0%
                            - **신규 혜택 유지**: 2026년 이후에도 신규 가입자 첫 30일 무료
                            - **점진적 인상**: 2026년부터 1% → 단계적 인상 예정
                            """)
                        .version("v1.0.0")
)
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.cherrypick.com")
                                .description("Production Server"),
                        new Server()
                                .url("https://dev-api.cherrypick.com")
                                .description("Development Server")
                ))
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 인증 토큰을 입력하세요. " +
                                                   "로그인/회원가입 후 받은 토큰을 'Bearer ' 없이 입력하면 됩니다.")));
    }
}