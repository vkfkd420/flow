# flow - 파일 확장자 차단 & 업로드

파일 업로드 시 확장자 차단 정책을 관리하고, 그 정책을 **실제 업로드에서 서버가 강제**하는 과제 프로젝트입니다.

| | |
|--|--|
| 배포 사이트 | http://43.202.189.27 |
| GitHub | https://github.com/vkfkd420/flow |
| 고려사항 문서 | [CONSIDERATIONS.md](CONSIDERATIONS.md) |
| AI 활용 기록 | [PROMPT_LOG.md](PROMPT_LOG.md) |

> 인증이 없어 배포 사이트에서는 누구나 정책을 바꿀 수 있습니다. (과제 요건: 누구나 접속 가능한 URL)

## 주요 기능

- **확장자 차단 정책**: 고정 확장자 7개 체크(체크 = 차단), 커스텀 확장자 추가·삭제 (최대 20자, 200개, 중복 방지)
- **파일 업로드**: 정책에 걸리면 사유와 함께 거부, 통과하면 S3에 저장 (요청당 1개, 10MB)
  - 파일명의 모든 점 구간 검사 (`invoice.exe.txt`의 `exe`도 검사)
  - 실행 파일을 다른 확장자로 위장하면 차단 (`report.jpg`인데 내용이 exe)

## 기술 스택

| 구분 | 스택 |
|------|------|
| Frontend | Vue 2.7 (Vue CLI 5), axios |
| Backend | Spring Boot 4.0.8, Java 17, Maven, MyBatis |
| DB | MySQL 8.4 |
| 저장소 | AWS S3 |
| 배포 | AWS EC2(프론트를 포함한 jar 1개) + RDS MySQL |

## Table schema

테이블 하나에 고정·커스텀 확장자를 함께 저장합니다. 정의: [db/schema.sql](db/schema.sql), 초기 데이터: [db/seed.sql](db/seed.sql)

**`FILE_EXTENSION_POLICY`**

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| `ID` | `BIGINT` | PK, AUTO_INCREMENT | |
| `EXTENSION` | `VARCHAR(20)` | NOT NULL, UNIQUE, 형식 CHECK | 정규화된 확장자 (점 없이 소문자, 예: `exe`) |
| `TYPE` | `VARCHAR(10)` | NOT NULL, CHECK | `FIXED` 고정 / `CUSTOM` 커스텀 |
| `IS_BLOCKED` | `TINYINT(1)` | NOT NULL, DEFAULT 1, CHECK | `1` 차단 / `0` 허용 (고정 확장자의 체크 상태) |
| `CREATED_AT` | `DATETIME` | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 생성 시각 |
| `UPDATED_AT` | `DATETIME` | NOT NULL, DEFAULT CURRENT_TIMESTAMP ON UPDATE | 수정 시각 |

| 제약 | 내용 | 막는 것 |
|------|------|---------|
| `UQ_FILE_EXTENSION` | `UNIQUE (EXTENSION)` | 커스텀 중복, **커스텀과 고정의 겹침** (한 테이블이라 함께 막힘) |
| `CK_FILE_EXTENSION_TYPE` | `TYPE IN ('FIXED', 'CUSTOM')` | 잘못된 종류 |
| `CK_FILE_EXTENSION_BLOCKED` | `IS_BLOCKED IN (0, 1)` | 0/1 외의 값 |
| `CK_FILE_EXTENSION_FORMAT` | `REGEXP_LIKE(EXTENSION, '^[a-z0-9]{1,20}$', 'c')` | 앱을 거치지 않은 값(대문자, 점, 공백, 한글 등) |
| `CK_FILE_EXTENSION_CUSTOM_BLOCKED` | `TYPE = 'FIXED' OR IS_BLOCKED = 1` | "허용" 상태의 커스텀 (커스텀은 추가 = 차단) |

- **인덱스**: PK(`ID`), UNIQUE(`EXTENSION`). 행이 최대 207개(고정 7 + 커스텀 200)라 그 외 인덱스는 두지 않았습니다.
- 엔진·문자셋: InnoDB, `utf8mb4` / `utf8mb4_unicode_ci`
- 초기 데이터: 고정 확장자 `bat, cmd, com, cpl, exe, scr, js` (모두 `IS_BLOCKED = 0`, 체크 해제)

## API

에러는 모두 `{ "code": "...", "message": "화면에 보여줄 문장" }` 형식입니다.

| 메서드 | 경로 | 설명 |
|--------|------|------|
| GET | `/api/extensions` | 정책 조회 (고정 목록, 커스텀 목록, 개수/상한) |
| PATCH | `/api/extensions/fixed/{extension}` | 고정 확장자 체크 변경 `{ "blocked": true }` |
| POST | `/api/extensions/custom` | 커스텀 추가 `{ "extension": "sh" }` |
| DELETE | `/api/extensions/custom/{id}` | 커스텀 삭제 |
| POST | `/api/files` | 파일 업로드 (multipart, 필드 `file`) |

## 실행 방법 (로컬)

**필요한 것**: JDK 17, Node.js 22, MySQL 8.4. Windows라면 아래 스크립트가 설치부터 DB 구성, 테스트까지 한 번에 합니다.

```bash
git clone https://github.com/vkfkd420/flow.git
cd flow
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

직접 구성하는 경우:

1. MySQL에 DB `flow`와 계정을 만들고 `db/schema.sql` → `db/seed.sql` 순서로 실행
2. `backend/.env.example`을 `backend/.env`로 복사해 DB 비밀번호 입력
3. 실행 (각각 새 터미널)

```bash
cd backend && ./mvnw spring-boot:run
```

```bash
cd frontend && npm ci && npm run serve
```

- 위 명령은 Git Bash 기준입니다. Windows PowerShell에서는 `cd backend; .\mvnw.cmd spring-boot:run`
- 화면: http://localhost:3000 (API는 `localhost:8080`으로 프록시)
- 파일 업로드를 실제로 저장하려면 S3 자격 증명(`aws configure`)과 `.env`의 `AWS_S3_BUCKET`이 필요합니다. 없으면 정상 파일만 `503`이고 정책·차단은 동작합니다.
- 테스트: `cd backend && ./mvnw clean test` (56개, 로컬 MySQL에 스키마가 있어야 함)
- 자세한 절차와 문제 해결: [SETUP.md](SETUP.md)

## 폴더 구조

```
flow/
├─ frontend/          Vue 2 (src/components: 정책 화면, 업로드 화면)
├─ backend/           Spring Boot (controller / service / dao / dto / common)
├─ db/
│  ├─ schema.sql      테이블 정의
│  └─ seed.sql        초기 데이터
├─ deploy/            AWS 배포 스크립트 (빌드, 배포, systemd 서비스, EC2 초기화)
├─ setup.ps1          로컬 환경 자동 구성 (Windows)
├─ README.md
├─ CONSIDERATIONS.md  고려사항과 판단 근거
├─ PROMPT_LOG.md      AI 활용 기록
├─ SETUP.md           다른 PC에서 실행하기
├─ CLEANUP.md         AWS 과금 정리
└─ HANDOFF.md         작업 현황 (작업 인수인계용)
```

## 배포

- 구성: EC2 `t4g.micro`(Spring Boot jar에 프론트 포함, systemd) + RDS MySQL(비공개) + S3(비공개), 서울 리전
- 다시 배포: `bash deploy/build.sh` → `bash deploy/deploy.sh <EC2 IP>` (SSH 키 필요)
- 리소스와 서버 구성: [HANDOFF.md 5장](HANDOFF.md), 과제 종료 후 정리: [CLEANUP.md](CLEANUP.md)
