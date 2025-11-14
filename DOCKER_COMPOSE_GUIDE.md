# 다른 서비스들의 Docker Compose 설정 가이드

API Gateway와 통신하기 위해 각 서비스의 `docker-compose.yml`에서 다음 사항을 확인해야 합니다.

## 필수 설정 사항

### 1. Docker 네트워크 설정

모든 서비스는 **같은 네트워크**를 사용해야 합니다:

```yaml
networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

### 2. 서비스 이름 설정

각 서비스의 `services` 섹션에서 **서비스 이름**이 API Gateway의 설정과 일치해야 합니다:

| 서비스 | 서비스 이름 | 포트 | API Gateway 설정 |
|--------|------------|------|-----------------|
| UserService | `user-service` | 8081 | `http://user-service:8081` |
| ProblemService | `problem-service` | 8082 | `http://problem-service:8082` |
| LearningAnalysisService | `learning-analysis-service` | 8083 | `http://learning-analysis-service:8083` |
| MediaService | `media-service` | 8084 | `http://media-service:8084` |
| SpeechService | `speech-service` | 8085 | `http://speech-service:8085` |

## 각 서비스별 docker-compose.yml 예시

### UserService (8081)

```yaml
version: '3.8'

services:
  user-service:  # ⚠️ 이 이름이 중요! GatewayConfig와 일치해야 함
    build: .
    container_name: user-service-app
    ports:
      - "8081:8081"  # ⚠️ 포트 번호 확인
    networks:
      - english-compass-network
    # ... 기타 설정

networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

### ProblemService (8082)

```yaml
version: '3.8'

services:
  problem-service:  # ⚠️ 이 이름이 중요!
    build: .
    container_name: problem-service-app
    ports:
      - "8082:8082"  # ⚠️ 포트 번호 확인
    networks:
      - english-compass-network
    # ... 기타 설정

networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

### LearningAnalysisService (8083)

```yaml
version: '3.8'

services:
  learning-analysis-service:  # ⚠️ 이 이름이 중요!
    build: .
    container_name: learning-analysis-service-app
    ports:
      - "8083:8083"  # ⚠️ 포트 번호 확인
    networks:
      - english-compass-network
    # ... 기타 설정

networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

### MediaService (8084)

```yaml
version: '3.8'

services:
  media-service:  # ⚠️ 이 이름이 중요!
    build: .
    container_name: media-service-app
    ports:
      - "8084:8084"  # ⚠️ 포트 번호 확인
    networks:
      - english-compass-network
    # ... 기타 설정

networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

### SpeechService (8085)

```yaml
version: '3.8'

services:
  speech-service:  # ⚠️ 이 이름이 중요!
    build: .
    container_name: speech-service-app
    ports:
      - "8085:8085"  # ⚠️ 포트 번호 확인
    networks:
      - english-compass-network
    # ... 기타 설정

networks:
  english-compass-network:
    external: true
    name: english-compass-network
```

## 체크리스트

각 서비스의 docker-compose.yml에서 다음을 확인하세요:

- [ ] `networks` 섹션에 `english-compass-network` (external: true) 추가
- [ ] `services` 섹션의 서비스 이름이 API Gateway 설정과 일치
- [ ] 포트 번호가 올바르게 설정됨
- [ ] 서비스가 `english-compass-network`에 연결됨

## 네트워크 생성

네트워크가 없으면 먼저 생성해야 합니다:

```bash
docker network create english-compass-network
```

## 문제 해결

### 서비스를 찾을 수 없음 (Service not found)

- 서비스 이름이 GatewayConfig와 일치하는지 확인
- 같은 네트워크에 있는지 확인: `docker network inspect english-compass-network`

### 연결 거부 (Connection refused)

- 포트 번호가 올바른지 확인
- 서비스가 실행 중인지 확인: `docker ps`

