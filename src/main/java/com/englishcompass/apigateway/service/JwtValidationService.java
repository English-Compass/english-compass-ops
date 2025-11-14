package com.englishcompass.apigateway.service;

import com.englishcompass.apigateway.config.JwtConfig;
import com.englishcompass.apigateway.dto.UserInfo;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtValidationService {

    private final JwtConfig jwtConfig;

    /**
     * API Gateway에서 직접 JWT 토큰을 검증하고 사용자 정보를 추출합니다.
     * 
     * @param token JWT 토큰
     * @return 사용자 정보 (userId)
     */
    public Mono<UserInfo> validateToken(String token) {
        return Mono.fromCallable(() -> {
            try {
                log.debug("Validating JWT token directly in API Gateway");

                // JWT 시크릿 키 생성
                SecretKey secretKey = Keys.hmacShaKeyFor(
                        jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8)
                );

                // JWT 토큰 파싱 및 검증
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                // 토큰에서 사용자 정보 추출
                String userId = claims.getSubject();
                if (userId == null) {
                    // subject가 없으면 userId 클레임 확인
                    userId = claims.get("userId", String.class);
                }

                if (userId == null || userId.isEmpty()) {
                    log.warn("JWT token missing userId/subject claim");
                    throw new RuntimeException("Invalid token: missing user identifier");
                }

                // role 클레임 추출
                String role = claims.get("role", String.class);
                if (role == null || role.isEmpty()) {
                    // role이 없으면 기본값으로 "USER" 설정
                    role = "USER";
                    log.debug("JWT token missing role claim, defaulting to USER");
                }

                log.debug("Token validated successfully. UserId: {}, Role: {}", userId, role);

                return UserInfo.builder()
                        .userId(userId)
                        .role(role)
                        .build();

            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                log.warn("JWT token expired: {}", e.getMessage());
                throw new RuntimeException("Token expired", e);
            } catch (io.jsonwebtoken.security.SignatureException e) {
                log.warn("JWT token signature validation failed: {}", e.getMessage());
                throw new RuntimeException("Invalid token signature", e);
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                log.warn("JWT token malformed: {}", e.getMessage());
                throw new RuntimeException("Malformed token", e);
            } catch (Exception e) {
                log.error("JWT token validation failed: {}", e.getMessage());
                throw new RuntimeException("Token validation failed", e);
            }
        });
    }
}

