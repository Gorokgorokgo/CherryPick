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
                            이미지 업로드       → 경매 등록          → Q&A 문의        → 입찰 참여
                            POST /images/upload POST /auctions     POST /qna/question POST /bids
                            GET /images        GET /auctions      GET /qna/questions GET /bids/my
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
                            - **입찰**: 포인트 예치 없이 자유로운 입찰 참여
                            
                            ### 🔗 연결 서비스 시스템 (NEW!)
                            - **연결 수수료**: 낙찰 후 판매자만 결제 (구매자 무료)
                            - **기본 수수료**: 낙찰가의 3% (레벨별 할인 적용)  
                            - **레벨 할인**: 초보(0%) → 성장(10%) → 숙련(20%) → 고수(30%) → 마스터(40%)
                            - **채팅 활성화**: 수수료 결제 완료 후 판매자-구매자 직접 소통
                            
                            ### 🎯 레벨 시스템 혜택
                            
                            | 레벨 구간 | 범위 | 연결 수수료 할인 | 주요 혜택 |
                            |----------|------|------------------|-----------|
                            | 🟢 초보 | 0-20 | 할인 없음 (3.0%) | 무료 입찰, 기본 경매 등록 |
                            | 🟡 성장 | 21-40 | 10% 할인 (2.7%) | 경매 등록, 채팅 기능 |
                            | 🟠 숙련 | 41-60 | 20% 할인 (2.4%) | 프리미엄 경매, 우선 채팅 |
                            | 🔴 고수 | 61-80 | 30% 할인 (2.1%) | VIP 경매, 우선 고객지원 |
                            | 🟣 마스터 | 81-100 | 40% 할인 (1.8%) | 최우선 지원, 베타 기능 |
                            
                            #### 🛡️ 마이너스 수수료 방지
                            - 최소 수수료율 1.2% 보장 (레벨 100 기준)
                            - 할인 적용 후에도 최소 수수료 유지
                            
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
                                .description("경매 등록, 조회, 검색 | 무료 경매 등록"),
                        new Tag()
                                .name("7단계 - Q&A 관리")
                                .description("경매 상품 문의 및 답변 관리 | 판매자-구매자 소통"),
                        new Tag()
                                .name("8단계 - 입찰 관리")
                                .description("경매 입찰 및 내역 조회 | 무료 입찰 시스템"),
                        new Tag()
                                .name("9단계 - 연결 서비스")
                                .description("판매자-구매자 연결 및 수수료 관리 | 수수료 결제 후 채팅 활성화"),
                        new Tag()
                                .name("10단계 - 레벨 권한 시스템")
                                .description("레벨 기반 권한 관리 | 입찰 제한 없음, 수수료 할인 혜택")
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