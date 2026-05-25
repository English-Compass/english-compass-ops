# API Gateway

English Compass MSA의 단일 진입점 서비스입니다. JWT 인증, CORS, 라우팅을 처리하고 다운스트림 서비스에 사용자 정보 헤더를 전달합니다.

## 기술 스택

| 항목 | 내용 |
|------|------|
| 언어 | Java 17 |
| 빌드 도구 | Maven |
| 프레임워크 | Spring Cloud Gateway, Spring WebFlux |
| 포트 | 8080 |
| JWT | JJWT 0.12.3 |

## 주요 기능

- **JWT 인증 필터**: 보호된 경로에서 토큰 서명 및 만료 검증
- **사용자 정보 헤더 주입**: JWT에서 `userId`, `role`을 추출해 `X-User-Id`, `X-User-Role` 헤더로 다운스트림에 전달
- **경로 기반 라우팅**: 경로 패턴에 따라 각 마이크로서비스로 요청 전달
- **CORS 처리**: 프론트엔드 오리진 허용

## 인증 흐름

```
클라이언트
  │
  ├── /api/auth/**  → JWT 검증 없이 UserService로 전달
  │
  └── 보호된 경로
        │
        JwtValidationFilter
          │ 토큰 검증 실패 → 401 Unauthorized
          │ 토큰 검증 성공
          │   └── X-User-Id, X-User-Role 헤더 추가
          │
        다운스트림 서비스 (ProblemService, LearningService, ...)
```

## 라우팅 규칙

| 경로 패턴 | 대상 서비스 | JWT 필요 |
|-----------|-------------|---------|
| `/api/auth/**` | UserService | 불필요 |
| `/api/user/**` | UserService | 필요 |
| `/api/problem/**` | ProblemService | 필요 |
| `/api/analysis/**` | LearningService | 불필요 |
| `/api/media/**` | MediaService | 필요 |
| `/api/recommendations/**` | MediaService | 필요 |
| `/api/speech/**` | SpeechService | 필요 |
| `/problem/internal/**` | ProblemService | 불필요 (서비스 간 내부 호출) |

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `JWT_SECRET` | JWT 서명 키 — UserService와 동일해야 함 | `default-key` |
| `SERVICE_USER_SERVICE_URI` | UserService 주소 | `http://user-service-app:8080` |
| `SERVICE_PROBLEM_SERVICE_URI` | ProblemService 주소 | `http://problem-service-app:8080` |
| `SERVICE_LEARNING_SERVICE_URI` | LearningService 주소 | `http://learning-service-app:8080` |
| `SERVICE_MEDIA_SERVICE_URI` | MediaService 주소 | `http://media-service:8080` |
| `SERVICE_SPEECH_SERVICE_URI` | SpeechService 주소 | `http://speech-service:8080` |

## 실행 방법

### 로컬 실행

```bash
cd api-gateway
mvn clean package
mvn spring-boot:run
```

### Docker (공유 인프라 포함)

```bash
# 공유 네트워크 생성 (최초 1회)
docker network create english-compass-network

# MySQL, Redis, Kafka, Zookeeper, Kafka UI 포함 전체 인프라 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f api-gateway
```

## 공유 인프라 (docker-compose)

api-gateway의 `docker-compose.yml`이 모든 공유 인프라를 정의합니다. 다른 서비스는 이 인프라를 공유합니다.

| 서비스 | 호스트 포트 | 비고 |
|--------|------------|------|
| MySQL | 3306 | 공유 DB (user_service_db, learning_service_db 등) |
| Redis | 6379 | ProblemService, LearningService 공유 캐시 |
| Zookeeper | 2181 | Kafka 코디네이터 |
| Kafka (호스트 접근) | 9094 | 로컬 개발 시 접근 포트 |
| Kafka (컨테이너 간) | 9092 | 서비스 간 통신 포트 |
| Kafka UI | 8090 | 브로커 모니터링 (`http://localhost:8090`) |

## 프로젝트 구조

```
src/main/java/com/englishcompass/apigateway/
├── ApiGatewayApplication.java
├── config/
│   ├── GatewayConfig.java        # 라우팅 규칙 정의
│   ├── JwtConfig.java
│   ├── CorsConfig.java
│   └── WebClientConfig.java
├── filter/
│   ├── JwtValidationFilter.java  # JWT 검증 및 헤더 주입
│   └── LocationRewriteFilter.java
└── service/
    └── JwtValidationService.java
```

## JWT 토큰 요구사항

UserService가 발급하는 JWT는 다음 클레임을 포함해야 합니다.

- `sub` 또는 `userId`: 사용자 ID (필수)
- `role`: 사용자 역할 (선택, 기본값 `USER`)
- `exp`: 만료 시간 (필수)

토큰은 `JWT_SECRET` 환경 변수와 동일한 키로 서명되어야 합니다.

## 트러블슈팅

| 증상 | 원인 | 해결 방법 |
|------|------|-----------|
| 502 Bad Gateway | 다운스트림 서비스 미실행 | 각 서비스 컨테이너 상태 확인 |
| 401 Unauthorized | 잘못된 토큰 또는 JWT_SECRET 불일치 | UserService와 동일한 JWT_SECRET 설정 확인 |
| CORS 에러 | 허용되지 않은 오리진 | `CorsConfig` 허용 목록 확인 |
