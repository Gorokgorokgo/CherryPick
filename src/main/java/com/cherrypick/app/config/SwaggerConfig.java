package com.cherrypick.app.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
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
                            - 📷 **이미지 업로드**: Supabase Storage 연동
                            
                            ### 인증 방법
                            1. `/api/auth/send-code`로 인증 코드 발송
                            2. `/api/auth/verify-code`로 코드 검증
                            3. `/api/auth/signup` 또는 `/api/auth/login`으로 JWT 토큰 획득
                            4. 이후 모든 API 요청시 `Authorization: Bearer {token}` 헤더 필요
                            
                            ### 비즈니스 규칙
                            - 💰 **보증금 시스템**: 경매 등록시 희망가의 10% 선차감
                            - 🔒 **포인트 예치**: 입찰시 해당 금액 잠금, 새로운 최고가 입찰시 해제
                            - 💸 **수수료**: 거래 완료시 3% 수수료 차감
                            - 📊 **최소 단위**: 모든 금액은 1,000원 단위
                            """)
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("CherryPick Development Team")
                                .email("dev@cherrypick.com")
                                .url("https://github.com/cherrypick-dev"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("https://api.cherrypick.com")
                                .description("Production Server"),
                        new Server()
                                .url("https://dev-api.cherrypick.com")
                                .description("Development Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("CherryPick API 가이드 문서")
                        .url("https://docs.cherrypick.com/api"))
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