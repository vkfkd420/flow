# PROMPT_LOG

AI 활용 기록 (과제 4번 항목)

- 사용 AI: Claude Code (Claude 데스크톱 앱 Code 탭, 모델 Claude Opus 5.5)
- 기록 범위: 개발 환경의 Git 설정 이후부터
- 전역 지침: `~/.claude/CLAUDE.md`에 코딩 원칙(최소 변경, 가정 금지, 불확실성 명시, 커밋 메시지 형식 등)을 등록해 두고 작업

---

## 1. 프롬프트 기록 (시간 순)

### #1 프로젝트 시작
- **입력**: `프로젝트 시작하자`
- **AI 응답**: 프로젝트 내용을 알 수 없으니 임의로 가정하지 않고 확인 질문을 함 (무엇을 만드는지, 형태, 기술 스택, 참고 자료, 첫 목표)
- **결과**: 과제 문서(`과제_파일업로드_AI개발.md`)를 프로젝트 폴더에 넣음

### #2 과제 이해 확인
- **입력**: `과제_파일업로드_AI개발 이거 넣어놨어 전체적으로 일단 읽고 너가 이해한것을 설명해봐`
- **AI 응답**: 과제를 필수 구현(A 정책 관리, B 실제 업로드 강제) / 고려사항 문서 / AI 활용 기록 / 제출물로 나누어 요약하고, 정해야 할 사항 5가지를 질문
  - 기술 스택과 배포처 (DB 무료 유지 가능 여부 포함)
  - 고정 확장자 "체크 = 차단" 해석이 맞는지 (문서에 명시되지 않음)
  - 인증 부재: 공개 배포 시 누구나 정책 변경/업로드 가능
  - PROMPT_LOG 기록 범위
  - 과제 원문을 저장소에 올려도 되는지
- **결과**: 아래 #3 결정으로 이어짐

### #3 기술 스택 및 방향 결정
- **입력**:
  1. 프론트 Vue 2, 백엔드 Spring Boot, DB Oracle, DB 관리는 DBeaver. `frontend`, `backend`, `db`로 폴더 구분
  2. 고정 확장자는 체크하면 차단
  3. 인증은 없이 진행 (추후 고려)
  4. PROMPT_LOG.md는 Git 설정 이후부터 기록
  5. 과제 파일도 저장소에 올림
  6. 폴더 구조 지정 (`frontend/`, `backend/`, `db/schema.sql`, `db/seed.sql`, `README.md`, `CONSIDERATIONS.md`, `PROMPT_LOG.md`)
- **AI 응답**: 로컬 개발 도구 설치 여부 확인 → Java, Node, Maven/Gradle, Oracle, Docker 모두 미설치 상태 확인. PROMPT_LOG.md 작성 후 과제 파일과 함께 첫 커밋
- **결과**: 과제 문서와 PROMPT_LOG.md 첫 커밋 및 push

### #4 개발 환경 결정
- **AI 질문** (선택지 제시, 추천안 표시):
  - Oracle 운영 방식: OCI Always Free Autonomous DB 하나로 / 로컬 XE + 배포 OCI / 로컬 XE만
  - 빌드 도구와 Java 버전: Gradle + 17 / Maven + 17 / Maven + 11
  - Vue 2 구성: Vue 2.7 + Vite / Vue 2.7 + Vue CLI
  - 도구 설치를 AI가 할지
- **선택**: OCI Autonomous DB 하나로 개발/배포, **Maven + Java 17** (AI 추천은 Gradle), **Vue 2.7 + Vue CLI** (AI 추천은 Vite), 설치는 AI가 진행
- **AI 작업**: winget으로 Temurin JDK 17(17.0.20.1), Node.js 22 LTS(22.23.2) 설치 후 버전 확인
  - Node는 Vue CLI 5 호환을 고려해 22 LTS로 선택

### #5 프로젝트 뼈대 생성
- **AI 질문**: DB 접근 방식 JPA / MyBatis (AI 의견: 스키마와 SQL이 1:1로 드러나는 MyBatis가 과제 성격에 맞음)
- **입력**: `MyBatis로 하고 뼈대 만들어줘`
- **AI 작업**:
  - `backend/`: Spring Initializr로 생성 (Maven, Java 17, 의존성 web / validation / mybatis / oracle)
    - Initializr 기본값은 Boot 4.1.1이었지만, MyBatis 스타터 지원 범위가 `[4.0.0, 4.1.0-M1)`이라 **Boot 4.0.8**로 생성 (앞서 AI가 "Spring Boot 3.x"라고 안내했던 부분이 실제 확인 후 바뀜)
    - `application.properties`에 DB 접속 정보를 환경 변수(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)로 받도록 설정, MyBatis mapper 경로 설정
  - `frontend/`: Vue CLI 5로 Vue 2 프리셋 생성 (babel, eslint)
    - 생성된 `package.json`은 `vue ^2.6.14`였지만 실제 설치 버전은 2.7.16 → 선택한 Vue 2.7과 일치하도록 `^2.7.16`으로 명시
  - `db/schema.sql`, `db/seed.sql`, `README.md`, `CONSIDERATIONS.md`: 자리만 생성 (내용은 설계 후 작성)
  - 루트 `.gitignore`: OCI Wallet 파일과 `.env`를 커밋하지 않도록 추가 (DB 접속 인증서 유출 방지)
- **검증**: frontend `npm run lint`, `npm run build` 통과 / backend `mvnw compile` 통과
  - backend 기본 테스트(`contextLoads`)는 DB 접속 정보가 없으면 실패하므로 아직 실행하지 않음

### #6 DB 변경: Oracle → MySQL
- **입력**: `그전에 계획이 바뀌었어 mysql로 하자`
- **AI 작업**: 프로젝트 전체에서 Oracle 관련 부분을 검색한 뒤 해당 부분만 교체
  - `backend/pom.xml`: `ojdbc17` → `mysql-connector-j` (버전은 Spring Boot가 관리, 9.7.0으로 확인)
  - `application.properties`: 접속 예시를 `jdbc:mysql://localhost:3306/flow`로 변경 (환경 변수 방식은 유지)
  - `README.md`, `db/schema.sql` 주석의 DB 표기 변경
  - `.gitignore`: 더 이상 필요 없는 OCI Wallet 항목 제거
  - 위 #3~#5의 Oracle 관련 기록은 당시 결정 과정이므로 수정하지 않음
- **검증**: `mvnw compile` 통과, 의존성 목록에서 Oracle 드라이버가 빠지고 MySQL 드라이버만 남은 것 확인

### #7 로컬 MySQL 설치
- **입력**: `MySQL 8.4 설치해줘`
- **AI 작업**: winget에서 제공 버전 확인 후 `Oracle.MySQL` 8.4.9(8.4 LTS 최신) 설치
- **확인 결과**: MSI 무인 설치는 프로그램 파일만 설치하고, 데이터 디렉터리 초기화·Windows 서비스 등록·root 비밀번호 설정은 하지 않음
  - root 비밀번호는 사용자가 직접 정해야 하는 값이라 AI가 임의로 만들지 않고, MySQL Configurator를 사용자가 직접 실행하도록 안내
  - 앱 전용 DB(`flow`, utf8mb4)와 전용 계정(`flow@localhost`, `flow.*` 권한만) 생성 SQL을 안내 → 사용자가 DBeaver에서 실행
    - root를 앱에서 쓰지 않는 이유: 문제 발생 시 영향 범위를 앱 DB로 한정

### #8 DB 연결
- **입력**: `설정 완료했어, DB 연결해줘` → `비밀번호 입력했어, 연결 확인해줘`
- **AI 작업**:
  - 접속 정보는 `backend/.env`(Git 제외)에 두고 `spring.config.import=optional:file:.env[.properties]`로 읽도록 설정. 파일이 없으면 OS 환경 변수 사용 (배포 시)
  - 저장소에는 키 이름만 있는 `backend/.env.example`을 올리도록 `.gitignore`에 `!.env.example` 추가, `git check-ignore`로 확인
  - 비밀번호는 채팅으로 받지 않고 사용자가 `.env`에 직접 입력
- **검증** (2단계):
  1. `mysql` CLI로 `flow` 계정 로그인, DB 문자셋(utf8mb4), 권한(`flow.*`만) 확인 — 비밀번호는 `.env`에서 읽고 출력하지 않음
  2. Spring Boot에서 실제 커넥션 확인
     - Hikari/MyBatis는 첫 사용 시점에 연결하므로 "앱이 뜬다"만으로는 연결 검증이 안 된다고 판단
     - 임시 테스트로 `DataSource`와 MyBatis `SqlSession`에서 각각 쿼리 실행 → `flow@localhost`, `flow` DB, MySQL 8.4.9, Connector/J 9.7.0 확인
     - 기존 `contextLoads` 포함 테스트 2개 통과 후 임시 테스트 파일 삭제

---

## 2. 사용한 스킬 / 플러그인 / 도구

| 도구 | 사용처 | 이유 |
|------|--------|------|
| Claude Code | 과제 분석, 환경 확인, 문서 작성 | 코딩 에이전트로 파일 읽기·명령 실행·작성을 한 곳에서 처리 |
| Spring Initializr (start.spring.io) | backend 생성 | 버전/의존성 호환 범위를 메타데이터로 확인하고 생성 |
| Vue CLI 5 (`@vue/cli`) | frontend 생성 | 선택한 Vue 2 구성 |
| winget | JDK 17, Node.js 22 설치 | Windows 기본 패키지 관리자로 버전 고정 설치 |
| `~/.claude/CLAUDE.md` (전역 지침) | 모든 작업 | 요청 범위 밖 수정 방지, 불확실성 명시, 커밋 메시지 형식 통일 |

---

## 3. 판단 근거 회고

(작업 진행하며 작성)
