package com.englishcompass.apigateway.config;

import com.englishcompass.apigateway.filter.JwtValidationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                        .uri("http://user-service-app:8081"))
                
                // OAuth2 인증 시작 엔드포인트 (JWT 검증 불필요)
                // /api/auth/oauth2/** → /oauth2/**로 변환 (user-service의 permitAll() 패턴과 매칭)
                .route("oauth2-authorization", r -> r
                        .path("/api/auth/oauth2/**")
                        .filters(f -> f
                                .rewritePath("/api/auth/oauth2/(?<segment>.*)", "/oauth2/${segment}"))
                        .uri("http://user-service-app:8081"))
                
                // 기타 인증 엔드포인트 (JWT 검증 불필요)
                .route("auth-service", r -> r
                        .path("/api/auth/**")
                        .filters(f -> f.stripPrefix(1))  // /api 제거 → /auth/**로 전달
                        .uri("http://user-service-app:8081"))
                
                // User Service (JWT 검증 필요) - @RequestMapping("user")
                .route("user-service", r -> r
                        .path("/api/user/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /user/**로 전달
                        .uri("http://user-service-app:8081"))
                
                // Problem Service (JWT 검증 필요) - @RequestMapping("problem")
                .route("problem-service", r -> r
                        .path("/api/problem/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /problem/**로 전달
                        .uri("http://problem-service:8082"))
                
                // Learning Analysis Service (JWT 검증 필요) - @RequestMapping("analysis")
                .route("learning-analysis-service", r -> r
                        .path("/api/analysis/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /analysis/**로 전달
                        .uri("http://learning-analysis-service:8083"))
                
                // Media Service (JWT 검증 필요) - @RequestMapping("media")
                .route("media-service", r -> r
                        .path("/api/media/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /media/**로 전달
                        .uri("http://media-service:8084"))
                
                // Speech Service (JWT 검증 필요) - @RequestMapping("speech")
                .route("speech-service", r -> r
                        .path("/api/speech/**")
                        .filters(f -> f
                                .filter(jwtValidationFilter.apply(new JwtValidationFilter.Config()))
                                .stripPrefix(1))  // /api 제거 → /speech/**로 전달
                        .uri("http://speech-service:8085"))
                
                .build();
    }
}

