# UserService JWT 검증 엔드포인트 요구사항

API Gateway가 정상 작동하려면 UserService에 다음 엔드포인트를 구현해야 합니다.

## 1. JWT 검증 엔드포인트

### 엔드포인트
```
GET /api/auth/validate
```

### 요청 헤더
```
Authorization: Bearer <JWT_TOKEN>
```

### 응답 (성공 시 - 200 OK)
```
Headers:
  X-User-Id: <user_id>
  X-User-Role: <user_role> (선택사항)
  
Body: 없음 (빈 응답)
```

### 응답 (실패 시 - 401 Unauthorized)
```json
{
  "error": "Unauthorized",
  "message": "Invalid or expired token"
}
```

## 2. 구현 예시 (Spring Boot)

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    /**
     * JWT 토큰 검증 엔드포인트
     * API Gateway의 auth_request 모듈이 호출
     */
    @GetMapping("/validate")
    public ResponseEntity<Void> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        
        try {
            // Bearer 토큰 추출
            String token = authHeader.replace("Bearer ", "");
            
            // 토큰 검증
            if (!jwtTokenProvider.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // 사용자 정보 추출
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            String userRole = jwtTokenProvider.getRoleFromToken(token);
            
            // 응답 헤더에 사용자 정보 추가
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-User-Id", userId);
            headers.set("X-User-Role", userRole);
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .build();
                    
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
```

## 3. JWT 토큰 구조 예시

```json
{
  "sub": "user123",
  "userId": "user123",
  "role": "USER",
  "iat": 1234567890,
  "exp": 1234571490
}
```

## 4. nginx auth_request 동작 방식

1. 클라이언트가 `/api/learning/analytics`로 요청
2. nginx가 `auth_request /internal/auth/validate` 실행
3. 내부적으로 `GET /api/auth/validate` 요청 (Authorization 헤더 포함)
4. UserService가 토큰 검증 후 `X-User-Id` 헤더 반환
5. nginx가 검증 성공 시 원래 요청을 LearningService로 전달
6. `X-User-Id` 헤더를 함께 전달하여 백엔드 서비스에서 사용자 식별 가능

## 5. 테스트

### 직접 테스트

```bash
# 유효한 토큰으로 테스트
curl -X GET http://user-service:8081/api/auth/validate \
  -H "Authorization: Bearer <VALID_JWT_TOKEN>"

# 응답 헤더 확인
# X-User-Id: user123
```

### API Gateway를 통한 테스트

```bash
# API Gateway를 통해 인증된 요청
curl http://localhost/api/learning/analytics \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## 6. 주의사항

1. **토큰 만료 처리**: 만료된 토큰은 401 반환
2. **토큰 형식 검증**: Bearer 토큰 형식 확인
3. **성능**: 이 엔드포인트는 모든 인증된 요청마다 호출되므로 캐싱 고려
4. **에러 처리**: 모든 예외는 401로 반환하여 nginx가 적절히 처리하도록 함

