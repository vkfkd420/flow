# HANDOFF - 작업 현황 및 이어서 하기

다른 컴퓨터(또는 새 Claude Code 세션)에서 작업을 이어가기 위한 문서입니다.

- 마지막 갱신: 2026-09-30
- 저장소: https://github.com/vkfkd420/flow
- 과제 원문: [과제_파일업로드_AI개발.md](과제_파일업로드_AI개발.md)
- 상세 진행 기록: [PROMPT_LOG.md](PROMPT_LOG.md) (#1 ~ #8)

---

## 1. 확정된 결정사항

| 항목 | 결정 | 비고 |
|------|------|------|
| Frontend | Vue 2.7.16 + Vue CLI 5 (babel, eslint) | Vite 대신 Vue CLI 선택 |
| Backend | Spring Boot 4.0.8, Java 17, Maven | MyBatis 스타터가 Boot 4.0.x까지만 지원해서 4.1.x가 아닌 4.0.8 |
| DB 접근 | MyBatis 4.0.1 | JPA 대신 선택 (스키마와 SQL이 1:1로 드러나도록) |
| DB | MySQL 8.4 (로컬 8.4.9) | 처음엔 Oracle(OCI)로 정했다가 MySQL로 변경 |
| DB 관리 | DBeaver | |
| 패키지명 | `com.flow` | |
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

## 3. 다음에 할 것

1. **테이블 스키마 설계** → `db/schema.sql`, `db/seed.sql`
   - 고정/커스텀 확장자를 테이블 하나로 둘지 둘로 나눌지
   - 확장자 정규화 규칙 (소문자, 앞의 `.` 제거, 허용 문자)
   - 중복 방지 (UNIQUE 제약), 고정 확장자와 커스텀이 겹치는 경우 처리
   - 컬럼/타입/제약/인덱스 명시 (제출 필수)
2. **백엔드 API**: 정책 조회, 고정 확장자 체크 변경, 커스텀 추가/삭제 (20자, 200개 제한, 중복 방지)
3. **파일 업로드 API**: 서버 측 정책 강제, 차단 사유 반환, 안전한 저장 (원본 파일명 사용 금지 등)
4. **프론트 화면**: 정책 관리 화면 + 업로드 화면, 로딩/에러 처리
5. **테스트**: 확장자 검증 로직 중심
6. **문서**: `CONSIDERATIONS.md` (과제 3번 항목 전체), `README.md` 실행 방법, `PROMPT_LOG.md` 회고
7. **배포**: 배포처와 운영 MySQL 호스팅 미정 (후보: Aiven, TiDB Cloud Serverless, Cloudtype — 무료 조건은 확인 필요). 면접 당일까지 접속 가능해야 함

### 미정 / 확인 필요
- 배포처와 운영 DB 호스팅
- 로컬 개발 포트: Spring Boot와 Vue CLI 개발 서버의 기본 포트가 둘 다 8080 (Vue CLI는 사용 중이면 다른 포트로 자동 변경). 프록시 설정과 함께 정해야 함

---

## 4. 새 컴퓨터에서 이어서 하기

### 4-1. 도구 설치 (Windows, winget)

```bash
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.17.JDK -e
winget install --id OpenJS.NodeJS.22 -e
winget install --id Oracle.MySQL -e --version 8.4.9
```

설치 후 터미널(또는 Claude 앱)을 다시 시작해야 PATH가 적용됩니다.

### 4-2. Git 설정 및 저장소 받기

```bash
git config --global user.name "강현민"
git config --global user.email "vkfkd420@naver.com"
git clone https://github.com/vkfkd420/flow.git
```

### 4-3. MySQL 구성

1. `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql_configurator.exe` 실행
   - Development Computer, 포트 3306, root 비밀번호 설정, Windows 서비스 등록(자동 시작)
2. DBeaver에서 root로 접속 후 실행

```sql
CREATE DATABASE flow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'flow'@'localhost' IDENTIFIED BY '원하는_비밀번호';
GRANT ALL PRIVILEGES ON flow.* TO 'flow'@'localhost';
```

3. 스키마가 작성된 뒤라면 `db/schema.sql`, `db/seed.sql` 순서로 실행

### 4-4. 접속 정보 설정

`backend/.env.example`을 `backend/.env`로 복사한 뒤 `DB_PASSWORD`를 입력합니다. `.env`는 Git에 올라가지 않습니다.

### 4-5. 실행 확인

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

```bash
cd frontend
npm install
npm run serve
```

- backend는 `backend` 폴더에서 실행해야 `.env`를 읽습니다.

### 4-6. Claude Code 지침 옮기기

코딩 원칙 지침은 저장소가 아니라 **이전 컴퓨터의 `C:\Users\<사용자>\.claude\CLAUDE.md`**에 있습니다. 새 컴퓨터의 같은 위치에 복사해야 같은 규칙(한국어 응답, 최소 변경, 커밋 메시지 형식, `Co-Authored-By` 미사용 등)이 적용됩니다.

새 세션을 시작할 때는 이렇게 요청하면 됩니다.

> HANDOFF.md와 PROMPT_LOG.md 읽고 이어서 작업하자
