# UserService docker-compose.yml 수정 가이드

## 문제
UserService가 `mysql` 호스트명으로 DB에 연결하려 하지만, API Gateway의 서비스 이름은 `db`입니다.

## 해결 방법

UserService의 docker-compose.yml에서 다음을 수정하세요:

### 변경 전
```yaml
environment:
  - SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/user_service_db?...
```

### 변경 후
```yaml
environment:
  - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/user_service_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
```

## API Gateway의 서비스 이름 확인

API Gateway의 docker-compose.yml에서:
- 서비스 이름: `db` (이 이름으로 연결해야 함)
- 컨테이너 이름: `user_service_db` (이건 참고용)

## 전체 예시

```yaml
version: '3.8'

services:
  user-service-app:
    build: .
    container_name: user-service-app
    ports:
      - "8081:8081"
    environment:
      - SPRING_PROFILES_ACTIVE=docker
      # MySQL 연결 - API Gateway의 'db' 서비스에 연결
      - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/user_service_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul
      - SPRING_DATASOURCE_USERNAME=user_service_user
      - SPRING_DATASOURCE_PASSWORD=user_service_password
      # Redis 연결
      - SPRING_REDIS_HOST=redis
      - SPRING_REDIS_PORT=6379
      # Kafka 연결
      - KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      # JWT 시크릿 키 (API Gateway와 동일해야 함)
      - JWT_SECRET=${JWT_SECRET:-your-secret-key-change-this-in-production-minimum-256-bits}
    networks:
      - english-compass-network
    restart: unless-stopped

networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

## 중요 사항

1. **서비스 이름 사용**: `db` (docker-compose의 서비스 이름)
2. **컨테이너 이름 아님**: `user_service_db` (컨테이너 이름은 사용하지 않음)
3. **네트워크 확인**: `english-compass-network`에 연결되어 있어야 함

