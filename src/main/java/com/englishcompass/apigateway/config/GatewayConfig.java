package com.englishcompass.apigateway.config;

import com.englishcompass.apigateway.filter.JwtValidationFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class GatewayConfig {

    private final JwtValidationFilter jwtValidationFilter;

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // OAuth2 콜백 엔드포인트 (JWT 검증 불필요)
                // /api/auth/oauth2/callback/kakao → /login/oauth2/code/kakao로 변환
                .route("oauth2-callback", r -> r
                        .path("/api/auth/oauth2/callback/**")
                        .filters(f -> f
                                .rewritePath("/api/auth/oauth2/callback/(?<segment>.*)", "/login/oauth2/code/${segment}"))
                        .uri("http://user-service-app:8080"))
                
                // OAuth2 인증 시작 엔드포인트 (JWT 검증 불필요)
                // /api/auth/oauth2/** → /oauth2/**로 변환 (user-service의 permitAll() 패턴과 매칭)
                .route("oauth2-authorization", r -> r
                        .path("/api/auth/oauth2/**")
                        .filters(f -> f
                                .rewritePath("/api/auth/oauth2/(?<segment>.*)", "/oauth2/${segment}"))
                        .uri("http://user-service-app:8080"))
                
                // 기타 인증 엔드포인트 (JWT 검증 불필요)
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))  // /api 제거 → /auth/**로 전달
                        .uri("http://user-service-app:8080"))
                
                // User Service (JWT 검증 필요) - @RequestMapping("user")
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /user/**로 전달
                        .uri("http://user-service-app:8080"))
                
                // Problem Service 내부 API (서비스 간 통신용 - JWT 검증 불필요)
                // 더 구체적인 경로를 먼저 정의해야 함 (라우트 매칭 우선순위)
                // order를 명시적으로 낮게 설정하여 우선 매칭되도록 함
                .route("problem-service-internal", r -> r
                        .order(-1)  // 낮은 order 값 = 높은 우선순위
                        .path("/api/problem/internal/**")
                        .filters(f -> f
                                .filter((exchange, chain) -> {
                                    String originalPath = exchange.getRequest().getURI().getPath();
                                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                    log.info("✅ Internal API Route Matched: problem-service-internal");
                                    log.info("   Original path: {}", originalPath);
                                    log.info("   After stripPrefix(1): /problem/internal{}", originalPath.substring("/api".length()));
                                    log.info("   Target URI: http://problem-service-app:8080");
                                    log.info("   JWT Filter: NOT APPLIED (internal API)");
                                    log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
                                    return chain.filter(exchange);
                                })
                                .stripPrefix(1))  // JWT 필터 없음, /api 제거 → /problem/internal/**로 전달
                        .uri("http://problem-service-app:8080"))
                
                // Problem Service 일반 API (JWT 검증 필요) - @RequestMapping("problem")
                .route("problem-service", r -> r
                        .order(0)  // 기본 order 값
                        .path("/api/problem/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /problem/**로 전달
                        .uri("http://problem-service-app:8080"))
                
                // Learning Analysis Service (대시보드용 - JWT 검증 불필요)
                .route("learning-analysis-service-public", r -> r
                        .order(-1)
                        .path("/api/analysis/**")
                        .filters(f -> f.stripPrefix(1))
                        .uri("http://learning-service-app:8080"))
                
                // Media Service (JWT 검증 필요) - @RequestMapping("media")
                .route("media-service", r -> r
                        .path("/api/media/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /media/**로 전달
                        .uri("http://media-service:8080"))
                
                // Speech Service (JWT 검증 필요) - @RequestMapping("speech")
                .route("speech-service", r -> r
                        .path("/api/speech/**")
                        .filters(f -> f 
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /speech/**로 전달
                        .uri("http://speech-service:8080"))
                
                .build();
    }
}

