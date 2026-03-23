# URL Shortener

Spring Boot 기반 URL 단축 서비스입니다. PostgreSQL을 영속 저장소로 사용하고, Redis는 원본 URL 캐시, 클릭 집계 버퍼, 분석 버퍼, rate limiting 용도로 사용합니다.

## 빠른 시작

필수 준비물:

- Docker / Docker Compose
- Java 17 이상

```bash
cp .env.example .env
```

`.env`에서 최소한 `POSTGRES_PASSWORD`, `ADMIN_API_KEY` 값을 채운 뒤 실행합니다.
예시:

```dotenv
POSTGRES_PASSWORD=local-password
ADMIN_API_KEY=local-admin-key
```

```bash
docker-compose up --build
```

서비스가 뜨면 헬스체크:

```bash
curl http://localhost:8080/api/health
```

Swagger UI:

```bash
open http://localhost:8080/swagger-ui/index.html
```

## 환경 변수

- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`: PostgreSQL 연결 정보
- `REDIS_HOST`, `REDIS_PORT`: Redis 연결 정보
- `APP_BASE_URL`: 응답에 포함할 short URL base address
- `ADMIN_API_KEY`: DELETE API 인증 키
- `SHORT_CODE_OBFUSCATION_KEY`: short code 난독화 키
- `RATE_LIMIT_PER_MINUTE`: IP 기반 분당 요청 제한 기본값

## 현재 포함된 구성

- Spring Boot 3 / Java 17 toolchain
- PostgreSQL / Redis / Flyway 연결 설정
- Dockerfile / docker-compose 기반 로컬 실행
- `/api/health` 엔드포인트
- `POST /api/shorten` 엔드포인트
- `GET /{shortCode}` 302 리다이렉트
- `GET /api/urls/{shortCode}/stats` 통계 조회
- `GET /api/urls/{shortCode}/analytics` 일별/시간별 클릭 분석
- `DELETE /api/urls/{shortCode}` 소프트 딜리트
- URL 도메인 기본 엔티티 및 초기 스키마
- `DB sequence ID -> XOR 난독화 -> Base62` 기반 short code 생성
- Redis 기반 클릭 수 카운트 및 Lua buffer 기반 DB 동기화
- Redis 기반 IP rate limiting
- Redis 인덱스 셋 기반 추적과 배치 락

## 실행 예시

단축 URL 생성:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H 'Content-Type: application/json' \
  -d '{"original_url":"https://example.com/docs"}'
```

커스텀 코드와 만료일 지정:

```bash
curl -X POST http://localhost:8080/api/shorten \
  -H 'Content-Type: application/json' \
  -d '{"original_url":"https://example.com/docs","custom_code":"team-docs","expires_at":"2026-12-31T23:59:59Z"}'
```

통계 조회:

```bash
curl http://localhost:8080/api/urls/team-docs/stats
```

시간별 클릭 분석:

```bash
curl "http://localhost:8080/api/urls/team-docs/analytics?granularity=hour&from=2026-03-23T00:00:00Z&to=2026-03-23T23:59:59Z"
```

삭제:

```bash
curl -X DELETE http://localhost:8080/api/urls/team-docs \
  -H 'X-API-KEY: your-admin-key'
```

## API 요약

`POST /api/shorten`

```json
{
  "original_url": "https://example.com/docs",
  "custom_code": "team-docs",
  "expires_at": "2026-12-31T23:59:59Z"
}
```

`GET /{shortCode}`

- 302 Redirect
- 만료 또는 삭제된 URL이면 410
- 존재하지 않으면 404

`GET /api/urls/{shortCode}/stats`

- DB에서 URL 메타데이터를 조회하고, 클릭 수는 DB `click_count`와 Redis pending/processing buffer를 합산해서 응답

`GET /api/urls/{shortCode}/analytics?granularity=hour&from=...&to=...`

- `granularity=hour|day`
- DB에 반영된 버킷 집계와 Redis pending/processing buffer를 병합해서 응답

`DELETE /api/urls/{shortCode}`

- 헤더 `X-API-KEY: {ADMIN_API_KEY}`
- 소프트 딜리트 후 Redis active cache 무효화

## 기술 선택 이유

short code 생성:

- PostgreSQL sequence ID를 먼저 확보한 뒤 `XOR 난독화 -> Base62`로 인코딩했습니다.
- 충돌이 없고 동시성에 강하며, 랜덤 재시도 방식보다 동작 근거를 설명하기 쉽습니다.
- 내부 연속 ID가 그대로 노출되지 않도록 난독화 키를 한 번 섞었습니다.

Redis 사용 위치:

- 원본 URL 조회 캐시
- 총 클릭 수 pending/processing buffer
- 시간별/일별 클릭 분석 pending/processing buffer
- IP rate limiting

DB는 source of truth로 두고, Redis는 짧은 지연 시간과 높은 처리량이 필요한 경로에만 배치했습니다. 이렇게 분리하면 리다이렉트 요청 경로에서 DB write를 피할 수 있고, 짧은 URL 핫스팟 상황에서도 응답 지연과 락 경쟁을 줄일 수 있습니다.

## 클릭 수 동기화 전략

리다이렉트 시 클릭은 먼저 Redis pending key에 적재됩니다. 이때 DB `click_count`는 즉시 증가하지 않습니다.
배치 동기화 시에는 Lua script로 `pending -> processing` 을 원자적으로 이동하면서 sync token을 붙입니다.
DB에는 마지막으로 반영한 sync token을 함께 저장하고, 저장이 성공했을 때만 processing buffer를 삭제합니다.
그래서 DB 저장 실패가 나도 클릭 수가 Redis에서 사라지지 않고 다음 스케줄에서 재시도되며, DB 저장 후 ack 전에 프로세스가 죽어도 같은 batch가 다시 더해지지 않습니다.

## 분석 API 전략

리다이렉트가 발생하면 총 클릭 수 외에 시간 버킷 집계도 함께 Redis에 적재합니다.

- 시간별 집계: `hour` 버킷
- 일별 집계: `day` 버킷

분석 집계도 별도 스케줄러가 Redis buffer를 DB `url_click_metrics` 테이블로 반영합니다.
analytics API는 조회 시점에 DB 버킷 집계와 Redis pending/processing buffer를 함께 읽어서 응답합니다. 즉, 분석 응답은 DB와 Redis를 모두 조회하며, DB 테이블 반영은 배치 주기로 이뤄집니다.

## DB 반영 방식

이 프로젝트는 모든 쓰기를 배치로 처리하지 않습니다.

- URL 생성, 삭제, 메타데이터 조회는 즉시 DB를 사용합니다.
- 클릭 수 집계만 Redis 선집계 후 배치성 스케줄러로 DB에 반영합니다.

기본 동기화 주기는 `app.click-sync-interval-ms` 이며 현재 기본값은 `60000ms` 입니다.
분석 집계 주기도 별도 스케줄러로 동작하며 기본값은 `app.analytics-sync-interval-ms=60000ms` 입니다.

## 트레이드오프

- stats API는 호출 시점마다 DB 메타데이터와 Redis buffer를 함께 조회하므로, 응답 기준 클릭 수는 배치 반영 전 값까지 포함할 수 있습니다.
- 하지만 DB 테이블의 `click_count` 컬럼 자체는 스케줄 주기만큼 늦게 반영될 수 있습니다.
- analytics API도 같은 방식으로 DB 집계와 Redis buffer를 함께 조회하며, `url_click_metrics` 테이블 자체는 스케줄 주기만큼 늦게 반영될 수 있습니다.
- rate limiting은 IP 기반 단순 제한이라 NAT 환경에서는 여러 사용자가 한 IP를 공유할 수 있습니다.
- 현재는 Redis 인덱스 셋으로 추적 대상을 관리하고, 조회 시 stale member를 lazy cleanup 합니다.
- 배치 락은 TTL 기반이므로 장시간 작업에서는 lock renewal이 추가로 필요할 수 있습니다.

## 테스트

- 단위 테스트
- WebMvc 테스트
- Testcontainers 기반 통합 테스트

실행:

```bash
./gradlew test
```

## 시간이 더 있었다면

- Testcontainers 기반 케이스를 더 늘려서 만료/충돌/배치 재시도 시나리오까지 검증
- Redis 인덱스의 stale member 정리 전략을 더 공격적으로 정교화
- 배치 락에 renewal과 강제 회수 정책 추가
- 관리자 인증을 단순 API 키보다 더 안전한 방식으로 확장

## 남은 개선 포인트
- 운영 환경용 Redis 인덱스 정리와 배치 락 전략 보강
