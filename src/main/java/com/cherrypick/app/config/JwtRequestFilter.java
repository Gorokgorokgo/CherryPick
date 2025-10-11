package com.cherrypick.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private final JwtConfig jwtConfig;

    public JwtRequestFilter(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain chain) throws ServletException, IOException {
        
        // 인증이 필요없는 경로는 필터를 건너뜀
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/api/auth/") ||
            requestPath.startsWith("/api/hello/") ||
            requestPath.startsWith("/swagger-ui/") ||
            requestPath.startsWith("/v3/api-docs/") ||
            requestPath.startsWith("/actuator/") ||
            requestPath.equals("/test") ||
            requestPath.equals("/swagger-ui.html")) {
            chain.doFilter(request, response);
            return;
        }

        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;

        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                email = jwtConfig.extractEmail(jwtToken);
            } catch (Exception e) {
                logger.warn("JWT Token 추출 실패: " + e.getMessage());
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtConfig.validateToken(jwtToken, email)) {
                Long userId = jwtConfig.extractUserId(jwtToken);
                
                // UserDetails 객체 생성
                UserDetails userDetails = User.builder()
                    .username(email)
                    .password("") // JWT 기반이므로 패스워드 불필요
                    .authorities(new ArrayList<>())
                    .build();
                
                UsernamePasswordAuthenticationToken authToken = 
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        chain.doFilter(request, response);
    }
}