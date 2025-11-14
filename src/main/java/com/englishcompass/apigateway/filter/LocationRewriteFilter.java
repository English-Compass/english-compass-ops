package com.englishcompass.apigateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class LocationRewriteFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpResponse originalResponse = exchange.getResponse();
        
        ServerHttpResponseDecorator decoratedResponse = new ServerHttpResponseDecorator(originalResponse) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders(super.getHeaders());
                
                // Location 헤더가 있는지 확인
                List<String> locationHeaders = headers.get(HttpHeaders.LOCATION);
                if (locationHeaders != null && !locationHeaders.isEmpty()) {
                    String location = locationHeaders.get(0);
                    
                    if (location == null) {
                        return headers;
                    }
                    
                    // 외부 URL (다른 도메인)은 변환하지 않고 그대로 전달
                    // Location 헤더의 시작 부분만 확인 (URL 파라미터 안의 내부 호스트는 무시)
                    if (location.startsWith("http://") || location.startsWith("https://")) {
                        // localhost:8080이나 내부 서비스로 시작하는 경우만 변환
                        boolean isInternalUrl = location.startsWith("http://localhost:8080") ||
                                               location.startsWith("http://user-service-app:8081") ||
                                               location.startsWith("http://problem-service:8082") ||
                                               location.startsWith("http://learning-analysis-service:8083") ||
                                               location.startsWith("http://media-service:8084") ||
                                               location.startsWith("http://speech-service:8085");
                        
                        // 외부 URL (카카오, 구글 등)은 그대로 전달
                        // user-service의 OAuth2 설정에서 이미 올바른 redirect_uri를 생성하므로 변환 불필요
                        if (!isInternalUrl) {
                            log.debug("External URL detected, passing through: {}", location.length() > 100 ? location.substring(0, 100) + "..." : location);
                            return headers;
                        }
                    }
                    
                    // 이미 localhost:8080으로 시작하는 Location은 변환하지 않음 (무한 루프 방지)
                    if (location.startsWith("http://localhost:8080")) {
                        // 동일 경로로 리다이렉트하는 경우 Location 헤더 제거 (무한 루프 방지)
                        String requestPath = exchange.getRequest().getURI().getPath();
                        String locationPath = location.replace("http://localhost:8080", "");
                        
                        if (requestPath.equals(locationPath)) {
                            log.warn("Preventing redirect loop: same path redirect detected. Request: {}, Location: {}", requestPath, location);
                            headers.remove(HttpHeaders.LOCATION);
                            return headers;
                        }
                        return headers;
                    }
                    
                    if (location != null) {
                        String newLocation = null;
                        
                        // user-service-app:8081을 localhost:8080/api로 변환
                        if (location.contains("user-service-app:8081")) {
                            // URI 파싱하여 경로와 쿼리 파라미터 분리
                            java.net.URI uri = java.net.URI.create(location);
                            String path = uri.getPath();
                            String query = uri.getQuery();
                            String fragment = uri.getFragment();
                            
                            // 쿼리 파라미터와 fragment를 유지하기 위한 문자열 생성
                            String queryString = query != null ? "?" + query : "";
                            String fragmentString = fragment != null ? "#" + fragment : "";
                            
                            // /oauth2/로 시작하는 경로는 /api/auth/oauth2/로 변환
                            if (path.startsWith("/oauth2/")) {
                                newLocation = "http://localhost:8080/api/auth" + path + queryString + fragmentString;
                                
                                // 동일 경로로 리다이렉트하는 경우 방지
                                String requestPath = exchange.getRequest().getURI().getPath();
                                if (requestPath.equals("/api/auth" + path)) {
                                    log.warn("Preventing redirect loop: user-service redirects to same path. Request: {}, Location: {}", requestPath, location);
                                    headers.remove(HttpHeaders.LOCATION);
                                    return headers;
                                }
                            } else if (path.startsWith("/auth/")) {
                                // 이미 /auth/가 있으면 /api만 추가
                                newLocation = "http://localhost:8080/api" + path + queryString + fragmentString;
                            } else if (path.startsWith("/login/oauth2/")) {
                                // /login/oauth2/는 /api/auth/oauth2/callback/로 변환
                                String callbackPath = path.replace("/login/oauth2/", "/oauth2/callback/");
                                newLocation = "http://localhost:8080/api/auth" + callbackPath + queryString + fragmentString;
                            } else {
                                // 기본적으로 /api/auth 추가
                                newLocation = "http://localhost:8080/api/auth" + path + queryString + fragmentString;
                            }
                            log.debug("Rewrote Location header from {} to {}", location, newLocation);
                        }
                        // 다른 내부 서비스들도 처리
                        else if (location.contains(":8081")) {
                            java.net.URI uri = java.net.URI.create(location);
                            String path = uri.getPath();
                            String query = uri.getQuery();
                            String fragment = uri.getFragment();
                            String queryString = query != null ? "?" + query : "";
                            String fragmentString = fragment != null ? "#" + fragment : "";
                            
                            if (path.startsWith("/oauth2/")) {
                                newLocation = "http://localhost:8080/api/auth" + path + queryString + fragmentString;
                            } else if (path.startsWith("/login/oauth2/")) {
                                String callbackPath = path.replace("/login/oauth2/", "/oauth2/callback/");
                                newLocation = "http://localhost:8080/api/auth" + callbackPath + queryString + fragmentString;
                            } else {
                                newLocation = "http://localhost:8080/api" + path + queryString + fragmentString;
                            }
                            log.debug("Rewrote Location header from {} to {}", location, newLocation);
                        }
                        else if (location.contains(":8082")) {
                            java.net.URI uri = java.net.URI.create(location);
                            String path = uri.getPath();
                            String query = uri.getQuery();
                            String fragment = uri.getFragment();
                            String queryString = query != null ? "?" + query : "";
                            String fragmentString = fragment != null ? "#" + fragment : "";
                            newLocation = "http://localhost:8080/api" + path + queryString + fragmentString;
                            log.debug("Rewrote Location header from {} to {}", location, newLocation);
                        }
                        else if (location.contains(":8083")) {
                            java.net.URI uri = java.net.URI.create(location);
                            String path = uri.getPath();
                            String query = uri.getQuery();
                            String fragment = uri.getFragment();
                            String queryString = query != null ? "?" + query : "";
                            String fragmentString = fragment != null ? "#" + fragment : "";
                            newLocation = "http://localhost:8080/api" + path + queryString + fragmentString;
                            log.debug("Rewrote Location header from {} to {}", location, newLocation);
                        }
                        else if (location.contains(":8084")) {
                            java.net.URI uri = java.net.URI.create(location);
                            String path = uri.getPath();
                            String query = uri.getQuery();
                            String fragment = uri.getFragment();
                            String queryString = query != null ? "?" + query : "";
                            String fragmentString = fragment != null ? "#" + fragment : "";
                            newLocation = "http://localhost:8080/api" + path + queryString + fragmentString;
                            log.debug("Rewrote Location header from {} to {}", location, newLocation);
                        }
                        else if (location.contains(":8085")) {
                            java.net.URI uri = java.net.URI.create(location);
                            String path = uri.getPath();
                            String query = uri.getQuery();
                            String fragment = uri.getFragment();
                            String queryString = query != null ? "?" + query : "";
                            String fragmentString = fragment != null ? "#" + fragment : "";
                            newLocation = "http://localhost:8080/api" + path + queryString + fragmentString;
                            log.debug("Rewrote Location header from {} to {}", location, newLocation);
                        }
                        
                        if (newLocation != null) {
                            headers.set(HttpHeaders.LOCATION, newLocation);
                        }
                    }
                }
                
                return headers;
            }
        };
        
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    @Override
    public int getOrder() {
        // NettyWriteResponseFilter 이전에 실행되도록 설정
        return -1;
    }
}

