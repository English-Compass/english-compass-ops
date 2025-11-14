package com.englishcompass.apigateway.filter;

import com.englishcompass.apigateway.service.JwtValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class JwtValidationFilter extends AbstractGatewayFilterFactory<JwtValidationFilter.Config> {

    private final JwtValidationService jwtValidationService;

    public JwtValidationFilter(JwtValidationService jwtValidationService) {
        super(Config.class);
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    public String name() {
        return "JwtValidation";
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            org.springframework.http.HttpMethod method = request.getMethod();

            log.info("=== JWT Validation Filter START === Path: {}, Method: {}", path, method);
            
            // POST 요청인 경우 특별히 로깅
            if (method == org.springframework.http.HttpMethod.POST) {
                log.info("POST request detected. Checking if body will be preserved...");
            }

            // ① 공개 경로 확인: /api/auth/** 경로는 검증 없이 통과
            if (isPublicPath(path)) {
                log.debug("Public path detected, skipping JWT validation: {}", path);
                return chain.filter(exchange);
            }

            // ② 쿠키에서 accessToken 추출
            // 디버깅: 모든 쿠키 로깅
            log.info("All cookies received: {}", request.getCookies().keySet());
            if (!request.getCookies().isEmpty()) {
                request.getCookies().forEach((name, cookies) -> {
                    cookies.forEach(cookie -> {
                        log.info("Cookie - Name: {}, Value length: {}, Value preview: {}", 
                                name, 
                                cookie.getValue() != null ? cookie.getValue().length() : 0,
                                cookie.getValue() != null && cookie.getValue().length() > 50 
                                    ? cookie.getValue().substring(0, 50) + "..." 
                                    : cookie.getValue());
                    });
                });
            }
            
            String token = extractTokenFromCookie(request);
            
            if (token == null || token.isEmpty()) {
                log.error("Missing or empty access_token cookie for path: {}, method: {}. Available cookies: {}", 
                        path, method, request.getCookies().keySet());
                return onError(exchange, "Missing or invalid access token", HttpStatus.UNAUTHORIZED);
            }

            log.debug("Token extracted from cookie (first 20 chars): {}...", 
                    token.length() > 20 ? token.substring(0, 20) : token);

            // ③ 토큰 검증
            return jwtValidationService.validateToken(token)
                    .flatMap(userInfo -> {
                        // userId가 null이면 절대 통과시키지 않음 (NullPointerException 방지)
                        if (userInfo == null || userInfo.getUserId() == null || userInfo.getUserId().isEmpty()) {
                            log.error("CRITICAL: JWT validation returned null userId. Path: {}, Method: {}. Rejecting request.", path, method);
                            return onError(exchange, "Invalid token: missing user identifier", HttpStatus.UNAUTHORIZED);
                        }

                        log.info("JWT validation successful. UserId: {}, Role: {}, Path: {}, Method: {}", 
                                userInfo.getUserId(), userInfo.getRole(), path, method);

                        // ⑤ 성공 처리: userId, role 등을 헤더에 추가
                        // HTTP 메서드는 자동으로 보존되지만, 명시적으로 확인
                        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userInfo.getUserId())
                                .header("X-User-Role", userInfo.getRole() != null ? userInfo.getRole() : "USER")
                                .method(method) // HTTP 메서드 명시적으로 보존
                                .build();

                        // 헤더가 제대로 추가되었는지 최종 확인 (안전장치)
                        String userIdHeader = modifiedRequest.getHeaders().getFirst("X-User-Id");
                        if (userIdHeader == null || userIdHeader.isEmpty()) {
                            log.error("CRITICAL: X-User-Id header is missing after mutation. Path: {}, Method: {}. Rejecting request.", path, method);
                            return onError(exchange, "Internal error: failed to set user header", HttpStatus.INTERNAL_SERVER_ERROR);
                        }

                        log.info("Forwarding request - Path: {}, Method: {}, X-User-Id: {}, X-User-Role: {}", 
                                modifiedRequest.getURI().getPath(), 
                                modifiedRequest.getMethod(),
                                userIdHeader,
                                modifiedRequest.getHeaders().getFirst("X-User-Role"));

                        // ⑥ 요청 전달: 헤더가 추가된 요청을 다음 서비스로 전달
                        // POST 요청의 경우 본문(body)이 자동으로 보존됨
                        ServerWebExchange modifiedExchange = exchange.mutate().request(modifiedRequest).build();
                        
                        log.info("=== JWT Validation Filter SUCCESS === Path: {}, Method: {}, X-User-Id: {}", 
                                path, method, userIdHeader);
                        
                        return chain.filter(modifiedExchange)
                                .doOnError(error -> {
                                    log.error("=== JWT Validation Filter ERROR in chain === Path: {}, Method: {}, Error: {}", 
                                            path, method, error.getMessage(), error);
                                })
                                .doOnSuccess(v -> {
                                    log.info("=== JWT Validation Filter COMPLETE === Path: {}, Method: {}", path, method);
                                });
                    })
                    .onErrorResume(error -> {
                        // ④ 실패 처리: 유효하지 않으면 401 반환 (절대 헤더 없이 통과시키지 않음)
                        log.error("=== JWT Validation Filter FAILED === Path: {}, Method: {}. Error: {}, Message: {}. Returning 401.", 
                                path, method, error.getClass().getSimpleName(), error.getMessage(), error);
                        log.error("Stack trace:", error);
                        return onError(exchange, "Invalid or expired token: " + error.getMessage(), HttpStatus.UNAUTHORIZED);
                    });
        };
    }

    /**
     * 공개 경로인지 확인 (JWT 검증 불필요)
     */
    private boolean isPublicPath(String path) {
        // /api/auth/** 경로는 모두 공개 경로
        return path.startsWith("/api/auth/");
    }

    /**
     * 쿠키에서 access_token 추출
     */
    private String extractTokenFromCookie(ServerHttpRequest request) {
        // 여러 가능한 쿠키 이름 시도
        org.springframework.http.HttpCookie cookie = request.getCookies().getFirst("access_token");
        
        // access_token이 없으면 다른 이름도 시도
        if (cookie == null) {
            cookie = request.getCookies().getFirst("accessToken");
        }
        if (cookie == null) {
            cookie = request.getCookies().getFirst("token");
        }
        
        if (cookie == null) {
            log.warn("No access_token/accessToken/token cookie found. Available cookies: {}", 
                    request.getCookies().keySet());
            return null;
        }
        
        String value = cookie.getValue();
        if (value == null || value.isEmpty()) {
            log.warn("Cookie value is null or empty");
            return null;
        }
        
        log.info("Extracted token from cookie. Value length: {}, First 30 chars: {}", 
                value.length(), value.length() > 30 ? value.substring(0, 30) + "..." : value);
        
        // "Bearer " 접두사가 있으면 제거
        if (value.startsWith("Bearer ")) {
            String token = value.substring(7);
            log.debug("Removed 'Bearer ' prefix. Token length: {}", token.length());
            return token;
        }
        
        return value;
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", 
                status.getReasonPhrase(), message);
        
        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(errorBody.getBytes()))
        );
    }

    public static class Config {
        // 필터 설정이 필요한 경우 여기에 추가
    }
}

