# HANDOFF - 작업 현황 및 이어서 하기

다른 컴퓨터(또는 새 Claude Code 세션)에서 작업을 이어가기 위한 문서입니다.

- 마지막 갱신: 2026-10-01
- 저장소: https://github.com/vkfkd420/flow
- 과제 원문: [과제_파일업로드_AI개발.md](과제_파일업로드_AI개발.md)
- 상세 진행 기록: [PROMPT_LOG.md](PROMPT_LOG.md) (#1 ~ #24)

---

## 1. 확정된 결정사항

| 항목 | 결정 | 비고 |
|------|------|------|
| Frontend | Vue 2.7.16 + Vue CLI 5 (babel, eslint) | Vite 대신 Vue CLI 선택 |
| Backend | Spring Boot 4.0.8, Java 17, Maven | MyBatis 스타터가 Boot 4.0.x까지만 지원해서 4.1.x가 아닌 4.0.8 |
| DB 접근 | MyBatis 4.0.1 | JPA 대신 선택 (스키마와 SQL이 1:1로 드러나도록) |
| DB | MySQL 8.4 (로컬 8.4.9) | 처음엔 Oracle(OCI)로 정했다가 MySQL로 변경 |
| DB 관리 | DBeaver | |
| 배포 | AWS: EC2 1대(Spring Boot + 프론트 포함) + RDS MySQL, HTTP | 아래 5번 참고. HTTPS는 필요 시 CloudFront 추가 |
| 패키지명 | `com.flow` | |
| 백엔드 패키지 구조 | `controller`, `service`, `dao`(@Mapper 인터페이스), `dto`(요청/응답 + DB 행), `common` | MyBatis XML은 `resources/mapper/{Dao 이름}.xml` |
| 고정 확장자 | **체크 = 차단** | 기본값은 모두 해제 |
| 인증 | 없음 (추후 고려) | 공개 배포 시 누구나 정책 변경 가능 → CONSIDERATIONS에 기록 예정 |
| 접속 정보 | `backend/.env` (Git 제외) 또는 OS 환경 변수 | `backend/.env.example` 참고 |
| 커밋 메시지 | `[강현민] 변경사항 내용` | `Co-Authored-By` 줄 붙이지 않음 |
| PROMPT_LOG | Git 설정 이후 작업부터 기록 | 작업할 때마다 항목 추가 |

## 2. 지금까지 한 것

- [x] Git 저장소 연결 (`origin` → GitHub, 브랜치 `main`)
- [x] 과제 문서 분석 및 요구사항 정리
- [x] 개발 도구 설치: JDK 17 (Temurin), Node.js 22, MySQL 8.4.9
- [x] `backend/` 생성 (Spring Initializr: web, validation, mybatis, mysql)
- [x] `frontend/` 생성 (Vue CLI, Vue 2 프리셋)
- [x] `db/schema.sql`, `db/seed.sql`, `README.md`, `CONSIDERATIONS.md` 자리 생성 (내용 없음)
- [x] 로컬 MySQL 구성: DB `flow` (utf8mb4), 전용 계정 `flow@localhost` (`flow.*` 권한만)
- [x] Spring Boot ↔ MySQL 연결 확인 (DataSource, MyBatis SqlSession 양쪽에서 쿼리 실행)
- [x] 검증: frontend `lint` / `build` 통과, backend `mvnw test` 통과
- [x] 두 번째 컴퓨터 환경 구성: JDK 17 (사용자 `JAVA_HOME` 17로 변경), MySQL 8.4.9, `backend/.env`, 연결 확인 (PROMPT_LOG #10~#14)
- [x] 테이블 스키마: `FILE_EXTENSION_POLICY` 한 테이블 (`TYPE` FIXED/CUSTOM, `IS_BLOCKED`), `EXTENSION` UNIQUE, 형식 CHECK(`^[a-z0-9]{1,20}$`), 로컬 DB 적용 및 제약 테스트 (PROMPT_LOG #15)
  - 커스텀에 고정 확장자 입력 시 거부하고 안내 / 업로드 시 파일명의 모든 점 구간 검사 / 이력은 생성·수정 시각만
- [x] 확장자 정책 API (PROMPT_LOG #16, 패키지 구조 #17) — 테스트 34개 통과
  - `GET /api/extensions`, `PATCH /api/extensions/fixed/{extension}`, `POST /api/extensions/custom`, `DELETE /api/extensions/custom/{id}`
  - 에러 응답은 `{ code, message }` (message는 화면에 그대로 표시), 입력 앞의 점은 하나만 제거
  - 200개 제한은 `SELECT COUNT(*) ... FOR UPDATE` 잠금으로 직렬화 (동시성 테스트로 확인)
  - 정규화 규칙은 `ExtensionRule` 한 곳에 있음 → 업로드 검사에서도 재사용
- [x] 파일 업로드 API `POST /api/files` (PROMPT_LOG #18) — 전체 테스트 56개 통과, S3는 Mock으로 테스트
  - 저장: S3 `uploads/{UUID}` (원본 파일명 미사용, `application/octet-stream`), 1건 1개, 10MB (초과 시 413)
  - 검사 순서: 파일명(금지 문자, 255자) → 확장자 정책(모든 점 구간, 끝의 점·공백 무시) → 실행 파일 위장(PE/ELF/Mach-O/`#!/`)
  - 자격 증명은 AWS SDK 기본 체인 (로컬 `~/.aws/credentials`, 배포 IAM 역할). 버킷은 `AWS_S3_BUCKET`
- [x] 프론트 화면 (PROMPT_LOG #19) — 탭 2개(확장자 차단 정책 / 파일 업로드), axios, 직접 작성한 CSS
  - 개발 서버 `localhost:3000`, `/api`는 `localhost:8080`으로 프록시 (`vue.config.js`). 코드는 상대 경로 `/api`만 사용
  - 클라이언트 검증(정규화·중복·고정 겹침·200개·10MB)은 즉시 안내용, 최종 판단은 서버
  - 저장 실패 시 체크 상태 되돌림, 삭제 404 시 목록에서 제거(DB와 맞춤), 로딩 실패 시 다시 시도
- [x] S3 실제 연동 (PROMPT_LOG #20) — 버킷 `min420-flow-uploads` (서울), IAM 사용자 `flow-app`
  - 권한은 `uploads/*` PutObject만 (밖에 쓰기·목록·읽기·삭제 거부를 CLI로 확인)
  - 한글 파일명 정상 저장 확인 (Windows Git Bash curl은 파일명을 CP949로 보내 깨지므로 테스트는 Node `fetch`나 브라우저로)

- [x] AWS 배포 (PROMPT_LOG #21) — **http://43.202.189.27** (면접 당일까지 유지)
  - 공개 URL에서 정책 변경·업로드·차단·S3 저장(IAM 역할) 확인, 재부팅 후 자동 복구 확인
  - 테이블명은 항상 대문자 `FILE_EXTENSION_POLICY` (RDS는 `lower_case_table_names = 0`)

## 3. 다음에 할 것

1. **문서**: `CONSIDERATIONS.md` (과제 3번 항목 전체), `README.md` 실행 방법·스키마·배포, `PROMPT_LOG.md` 회고

### 미정 / 확인 필요
- 프론트 빌드 경고 `export 'default' (imported as 'style0') was not found`: `<style>` 블록마다 발생, 스캐폴드 원본에도 있음(webpack 5.111 + vue-loader 15 조합으로 추정). CSS 결과물은 정상

---

## 4. 새 컴퓨터에서 이어서 하기

[SETUP.md](SETUP.md)를 따라 하세요. 루트의 `setup.ps1`로 한 번에 구성할 수 있습니다. (도구 설치, 저장소 받기, 로컬 MySQL, S3 자격 증명, `.env`, 실행, 동작 확인, 다른 컴퓨터에서 배포, 문제 해결, Claude 지침 옮기기)

---

## 5. 배포 환경 (AWS, 서울 리전)

- URL: http://43.202.189.27 (Elastic IP, 고정)
- 예상 비용: 월 약 $32.8 (크레딧 차감) — EC2 $7.6, RDS $18.3, RDS 스토리지 $2.6, EBS $0.7, 퍼블릭 IPv4 $3.7

### 리소스 (모두 태그 `Project=flow`)

| 리소스 | 이름/ID | 설정 |
|--------|---------|------|
| EC2 | `flow-app` (`i-020284f62d2094fbf`) | `t4g.micro`, Amazon Linux 2023 ARM, IMDSv2 필수, EBS 8GB 암호화 |
| RDS | `flow-db` | MySQL 8.4.11, `db.t4g.micro`, gp3 20GB, 퍼블릭 접근 없음, 암호화, 백업 1일 |
| 보안 그룹 | `flow-ec2-sg` / `flow-rds-sg` | 80 전체 + 22 관리자 IP만 / 3306은 `flow-ec2-sg`에서만 |
| IAM 역할 | `flow-ec2-role` | S3 `min420-flow-uploads/uploads/*` PutObject만 |
| 키 페어 | `flow-key` | `~/.ssh/flow-key.pem` (저장소에 없음) |
| S3 | `min420-flow-uploads` | 퍼블릭 액세스 차단 |

### 서버 구성
- 앱: `/opt/flow/app.jar`, systemd `flow` 서비스 (`deploy/flow.service`), 실행 계정 `flow`(로그인 불가), 80 포트
- 환경 변수: `/etc/flow/flow.env` (root만 읽기) — `SERVER_PORT`, `DB_URL`(`sslMode=REQUIRED`), `DB_USERNAME`, `DB_PASSWORD`, `AWS_REGION`, `AWS_S3_BUCKET`
- DB 앱 계정: `flow@172.31.%` (`SELECT/INSERT/UPDATE/DELETE`만). 관리자 `admin`은 스키마 변경용
- 로그: `sudo journalctl -u flow`

### 다시 배포하기 (코드 변경 후)

```bash
bash deploy/build.sh
bash deploy/deploy.sh 43.202.189.27
```

- SSH(22)는 관리자 IP만 허용. 다른 곳에서 배포하려면 `flow-ec2-sg`에 현재 IP 추가 필요
- `deploy.sh`는 SSH만 사용하므로 `flow-deploy` 액세스 키가 없어도 됨 (인프라 변경 시에만 필요)

### 정리 (과제 종료 후)
[CLEANUP.md](CLEANUP.md)를 순서대로 따라 하세요. (Elastic IP 해제와 RDS 자동 백업 삭제를 놓치기 쉬움)
