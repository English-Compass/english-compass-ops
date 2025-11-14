# API Gateway

English Compass MSA 아키텍처의 API Gateway입니다.

## 개요

Spring Boot 기반 API Gateway로 다음 기능을 제공합니다:
- JWT 토큰 검증 (UserService를 통한 검증)
- 서비스 라우팅
- CORS 처리
- 사용자 정보 헤더 추가 (X-User-Id, X-User-Role)

## 아키텍처

```
프론트엔드
    ↓
API Gateway (Spring Boot:8080)
    ├─→ /api/auth/* → UserService (8081) [인증 불필요]
    ├─→ /api/user/* → UserService (8081) [JWT 검증 필요]
    ├─→ /api/problem/* → ProblemService (8082) [JWT 검증 필요]
    ├─→ /api/analysis/* → LearningAnalysisService (8083) [JWT 검증 필요]
    ├─→ /api/media/* → MediaService (8084) [JWT 검증 필요]
    └─→ /api/speech/* → SpeechService (8085) [JWT 검증 필요]
```

## 인증 플로우

1. **OAuth2 로그인**
   ```
   사용자가 OAuth2 로그인 수행
   ```

2. **UserService에서 로그인 처리 및 JWT 토큰 생성**
   ```
   POST /api/auth/login
   → UserService에서 인증
   → JWT 토큰 발급
   ```

3. **프론트엔드로 JWT 토큰 전달**
   ```
   Response:
   {
     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     "refreshToken": "...",
     "expiresIn": 3600
   }
   ```

4. **클라이언트가 API 요청 시 Authorization 헤더에 토큰 포함**
   ```
   GET /api/learning/analytics
   Authorization: Bearer <JWT_TOKEN>
   ```

5. **API Gateway에서 JWT 토큰 검증 ✅**
   ```
   → API Gateway에서 직접 JWT 토큰 검증 (UserService 호출 불필요)
   → JWT 시크릿 키를 사용하여 토큰 서명 검증
   → 토큰에서 사용자 정보 추출 (userId, role)
   ```

6. **검증 성공 시 백엔드 서비스로 요청 전달 (사용자 정보를 헤더에 포함)**
   ```
   → X-User-Id, X-User-Role 헤더를 추가하여 백엔드 서비스로 전달
   → 백엔드 서비스는 비즈니스 로직만 처리 (검증은 이미 완료됨)
   ```

## 서비스 포트

- **API Gateway**: 8080
- **UserService**: 8081
- **ProblemService**: 8082
- **LearningAnalysisService**: 8083
- **MediaService**: 8084
- **SpeechService**: 8085

## 기술 스택

- Spring Boot 3.2.0
- Spring Cloud Gateway
- WebFlux (Reactive)
- JWT (io.jsonwebtoken)

## 실행 방법

### 1. 네트워크 생성 (이미 있다면 생략)

```bash
docker network create english-compass-network
```

### 2. 로컬에서 실행

```bash
# Maven 빌드
mvn clean package

# 실행
java -jar target/api-gateway-1.0.0.jar
```

또는 Maven으로 직접 실행:

```bash
mvn spring-boot:run
```

### 3. Docker로 실행

```bash
cd /Users/lhj/Workspace/api-gateway
docker-compose up -d
```

### 4. 로그 확인

```bash
docker-compose logs -f api-gateway
```

## 설정 파일

- `src/main/resources/application.properties`: Spring Boot 설정
- `src/main/java/com/englishcompass/apigateway/config/GatewayConfig.java`: 라우팅 규칙
- `pom.xml`: Maven 의존성 관리

## 환경 변수

- `JWT_SECRET`: JWT 시크릿 키 (기본값: your-secret-key-change-this-in-production-minimum-256-bits)
- `SPRING_PROFILES_ACTIVE`: Spring 프로파일 (기본값: default)

## UserService 요구사항

API Gateway가 정상 작동하려면 UserService에서 JWT 토큰을 생성해야 합니다. 
**API Gateway에서 직접 JWT 토큰을 검증하므로, UserService의 검증 엔드포인트는 더 이상 필요하지 않습니다.**

### JWT 토큰 구조 요구사항

UserService에서 생성하는 JWT 토큰은 다음 클레임을 포함해야 합니다:

- `sub` 또는 `userId`: 사용자 ID (필수)
- `role`: 사용자 역할 (선택사항, 기본값: "USER")
- `exp`: 토큰 만료 시간 (필수)
- `iat`: 토큰 발급 시간 (선택사항)

JWT 토큰은 API Gateway와 동일한 시크릿 키(`JWT_SECRET`)로 서명되어야 합니다.

### 로그인 엔드포인트

```
POST /api/auth/login
Body:
  {
    "username": "user@example.com",
    "password": "password"
  }

Response:
  {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "...",
    "expiresIn": 3600
  }
```

## 프로젝트 구조

```
api-gateway/
├── src/
│   ├── main/
│   │   ├── java/com/englishcompass/apigateway/
│   │   │   ├── ApiGatewayApplication.java
│   │   │   ├── config/
│   │   │   │   ├── JwtConfig.java
│   │   │   │   ├── WebClientConfig.java
│   │   │   │   └── CorsConfig.java
│   │   │   ├── filter/
│   │   │   │   └── JwtValidationFilter.java
│   │   │   ├── service/
│   │   │   │   └── JwtValidationService.java
│   │   │   ├── dto/
│   │   │   │   └── UserInfo.java
│   │   │   └── controller/
│   │   │       └── HealthController.java
│   │   └── resources/
│   │       └── application.yml
│   └── test/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
└── README.md
```

## 테스트

### 헬스 체크

```bash
curl http://localhost:8080/health
```

### 로그인 (인증 불필요)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "user@example.com",
    "password": "password"
  }'
```

### 인증된 요청

```bash
curl http://localhost:8080/api/analysis/users/{userId}/analytics \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

## 주요 기능

### 1. JWT 토큰 검증

- `JwtValidationFilter`: Gateway 필터로 JWT 토큰 검증 수행
- `JwtValidationService`: API Gateway에서 직접 JWT 토큰 검증 (UserService 호출 불필요)
  - JWT 시크릿 키를 사용하여 토큰 서명 검증
  - 토큰에서 사용자 정보 추출 (userId, role)
  - 토큰 만료, 서명 오류, 형식 오류 등 예외 처리
- 검증 실패 시 401 Unauthorized 반환

### 2. 사용자 정보 헤더 추가

- 검증 성공 시 `X-User-Id`, `X-User-Role` 헤더를 추가하여 백엔드 서비스로 전달
- 백엔드 서비스는 이 헤더를 통해 사용자 정보를 식별

### 3. 서비스 라우팅

- Spring Cloud Gateway를 사용한 동적 라우팅
- 경로 기반 라우팅 규칙 설정

### 4. CORS 처리

- 전역 CORS 설정으로 모든 Origin 허용
- 필요한 경우 특정 Origin만 허용하도록 설정 가능

## 문제 해결

### 502 Bad Gateway

- 각 마이크로서비스가 실행 중인지 확인
- 네트워크가 올바르게 연결되었는지 확인: `docker network inspect english-compass-network`

### 401 Unauthorized

- JWT 토큰이 유효한지 확인
- Authorization 헤더 형식이 올바른지 확인 (`Bearer <token>`)
- JWT 시크릿 키가 UserService와 동일한지 확인 (`JWT_SECRET` 환경 변수)
- 토큰이 만료되지 않았는지 확인
- 토큰에 `sub` 또는 `userId` 클레임이 포함되어 있는지 확인

### CORS 에러

- `CorsConfig`의 CORS 설정 확인
- 프론트엔드에서 올바른 헤더를 보내는지 확인

### 연결 타임아웃

- UserService가 실행 중인지 확인
- `application.yml`의 서비스 URL이 올바른지 확인
