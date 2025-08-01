package com.cherrypick.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.tags.Tag;
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
                            ## CherryPick API 가이드 - 중고물품 경매 서비스
                            
                            안전하고 투명한 중고물품 경매 플랫폼 API 문서입니다.
                            
                            ---
                            
                            ## 사용자 여정별 API 가이드
                            
                            ### STEP 1. 회원 가입 및 계정 설정
                            ```
                            전화번호 인증      → 회원가입/로그인      → JWT 토큰 획득
                            POST /auth/send-code  POST /auth/signup    인증 완료!
                            POST /auth/verify     POST /auth/login
                            ```
                            
                            ### STEP 2. 프로필 및 계좌 설정  
                            ```
                            프로필 설정        → 계좌 등록           → 포인트 충전
                            PUT /users/profile  POST /accounts      POST /points/charge
                            GET /users/profile  GET /accounts       GET /points/balance
                            ```
                            
                            ### STEP 3. 경매 활동
                            ```
                            이미지 업로드       → 경매 등록          → 입찰 참여
                            POST /images/upload POST /auctions     POST /bids
                            GET /images        GET /auctions      GET /bids/my
                            ```
                            
                            ---
                            
                            ## 인증 방법
                            
                            1. **인증코드 발송**: `POST /api/auth/send-code`
                            2. **코드 검증**: `POST /api/auth/verify-code` 
                            3. **회원가입/로그인**: `POST /api/auth/signup` 또는 `POST /api/auth/login`
                            4. **토큰 사용**: 모든 API 요청시 `Authorization: Bearer {token}` 헤더 필요
                            
                            ---
                            
                            ## 핵심 비즈니스 규칙
                            
                            ### 💰 포인트 시스템
                            - **충전**: 본인 명의 계좌만, 수수료 무료, 최대 보유 제한 없음
                            - **출금**: 충전한 본인 계좌만, 수수료 무료, 최소 출금 제한 없음
                            - **예치**: 입찰시 즉시 잠금, 새 최고가시 기존 예치 100% 즉시 해제
                            
                            ### 🏪 보증금 시스템  
                            - **보증금**: 희망가격 × 10% (경매 등록시 즉시 차감)
                            - **반환 조건**: 정상 거래 완료, 구매자 노쇼, 유찰시 100% 반환
                            - **위로금**: 노쇼시 경매기간별 차등 지급 (3h:10% ~ 72h:100%)
                            - **몰수**: 허위 매물, 반복적 노쇼(3회 이상), 악의적 조작시
                            
                            ### 🎯 Reserve Price (최저 내정가)
                            - **설정**: 판매자가 선택적으로 설정 (시작가 ≤ Reserve Price ≤ 희망가)
                            - **비공개**: 입찰자들에게 금액 미공개
                            - **유찰 조건**: 최고 입찰가 < Reserve Price시 유찰 처리
                            - **유찰시**: 판매자 보증금 100% 반환, 모든 입찰자 포인트 해제
                            
                            ### 💳 수수료 정책 (환경변수 기반 동적 조절)
                            
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
                            
                            ### 🤖 자동 입찰 시스템
                            - **입찰 단위**: 현재가의 1~10% 범위 (사용자 설정, 100원 단위 반올림)
                            - **입찰 딜레이**: 1초 후 자동 입찰 실행
                            - **최대가 도달시**: 자동 입찰 중단
                            - **예시**: 현재가 50,000원, 2% 설정 → 다음 입찰시 51,000원 자동 입찰
                            
                            ### ⚠️ 노쇼 패널티
                            - **제재 단계**: 1회(3일) → 2회(1주) → 3회(1개월) → 4회(1년) → 5회(영구정지)
                            - **누적 차감**: 정상 거래 10회 완료시 누적 1회 차감
                            
                            ---
                            
                            ## 보안 정책
                            
                            - **전화번호 인증**: SMS 인증으로 본인 확인
                            - **JWT 토큰**: 24시간 유효, 자동 갱신
                            - **계좌 보안**: 본인 명의 계좌만 등록 가능
                            - **거래 보안**: 모든 거래는 포인트 시스템을 통해 안전하게 처리
                            
                            ---
                            
                            ## 문의 및 지원
                            
                            - **개발팀**: dev@cherrypick.com
                            - **고객지원**: support@cherrypick.com  
                            - **GitHub**: https://github.com/cherrypick-auction
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
                .tags(List.of(
                        new Tag()
                                .name("1단계 - 인증")
                                .description("전화번호 기반 회원가입/로그인 | 모든 서비스 이용의 첫 번째 단계"),
                        new Tag()
                                .name("2단계 - 사용자 프로필")
                                .description("사용자 정보 관리 | 닉네임, 개인정보 설정"),
                        new Tag()
                                .name("3단계 - 계좌 관리")
                                .description("포인트 충전/출금을 위한 계좌 등록 및 관리 | 본인 명의 계좌만 등록 가능"),
                        new Tag()
                                .name("4단계 - 포인트 시스템")
                                .description("포인트 충전/출금 및 거래 내역 관리 | 모든 경매 거래의 기반"),
                        new Tag()
                                .name("5단계 - 이미지 업로드")
                                .description("경매 상품 이미지 업로드 | AWS S3 연동, 최대 5MB"),
                        new Tag()
                                .name("6단계 - 경매 관리")
                                .description("경매 등록, 조회, 검색 | 보증금 10% 자동 차감"),
                        new Tag()
                                .name("7단계 - 입찰 관리")
                                .description("경매 입찰 및 내역 조회 | 포인트 자동 예치/해제 시스템")
                ))
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