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
- **커밋**: 입력 `하나로 합쳐서 커밋하고 push해줘` → 커밋 전에 `.env` 제외와 `.env.example` 비밀번호 공란을 확인한 뒤 push

### #9 작업 인수인계 문서
- **입력**: `그전에 지금까지 했던것이랑 다음에 할것을 md로 정리해서 다른 컴퓨터에서도 이어서 작업할수있게 해줘`
- **AI 작업**: `HANDOFF.md` 작성 (결정사항, 완료 항목, 다음 할 일, 미정 사항, 새 컴퓨터 환경 구성 절차)
  - 전역 지침(`~/.claude/CLAUDE.md`)은 저장소 밖에 있어서 새 컴퓨터로 따로 옮겨야 한다는 점을 명시
  - 정리하면서 새로 확인한 문제: Spring Boot와 Vue CLI 개발 서버의 기본 포트가 둘 다 8080 → 미정 사항에 추가

### #10 다른 컴퓨터에서 작업 재개 - 현황 분석
- **입력**: `파일을 분석부터해봐`
- **AI 작업**: 과제 문서, HANDOFF, PROMPT_LOG, 소스 파일을 읽고 현재 상태 요약 (뼈대만 있고 기능 코드는 없음)
- **확인 결과**: 이 컴퓨터의 환경이 HANDOFF에 적힌 구성과 다름
  - JDK 8만 설치됨 (backend는 17 필요), Node.js 20 (기존 22), MySQL 미설치, `backend/.env`와 전역 지침 `~/.claude/CLAUDE.md` 없음
  - HANDOFF의 PROMPT_LOG 범위 표기(#1 ~ #8)가 실제(#9까지)와 다름

### #11 배포처 변경: AWS
- **입력**: `그전에 내용을 수정할게 aws로 배포할거야`
- **AI 작업**: HANDOFF 결정사항에 배포 항목(AWS) 추가, 다음 할 일과 미정 사항에서 기존 후보(Aiven, TiDB Cloud, Cloudtype)를 AWS로 교체
  - 앱 실행과 운영 DB에 쓸 AWS 서비스는 아직 정하지 않았으므로 미정으로 남김

### #12 JDK 17 설치
- **입력**: `응 JDK 17 설치부터 해줘`
- **AI 작업**: winget으로 Temurin JDK 17(17.0.20.1) 설치
- **확인 결과**:
  - 설치 후 시스템 `JAVA_HOME`은 17로 잡혔지만, **사용자 `JAVA_HOME`이 JDK 8(`C:\Program Files\Java\jdk1.8.0_311`)로 설정되어 있어 시스템 값을 덮어씀** → `mvnw`는 `JAVA_HOME`을 쓰므로 그대로 두면 Java 8로 빌드되어 실패
  - 사용자 `JAVA_HOME`은 다른 프로젝트에서 쓰고 있을 수 있어 AI가 임의로 바꾸지 않음
  - 설치 전에도 Microsoft Build of OpenJDK 17(17.0.19)이 이미 있었음 (분석 단계에서 `Program Files\Microsoft` 경로를 확인하지 않아 놓침)
- **검증**: 현재 세션에서만 `JAVA_HOME`을 Temurin 17로 지정해 `mvnw compile` 통과 (테스트는 DB가 없어 아직 실행 안 함)
- **AI 제시 선택지**: 사용자 `JAVA_HOME`을 17로 변경 / 그대로 두고 이 프로젝트 실행 시에만 17 지정
- **입력**: `1번으로 JAVA_HOME 17로 바꿔줘` → 사용자 `JAVA_HOME`을 Temurin 17 경로로 변경, 변경 값으로 `mvnw compile` 다시 통과
  - 사용자 PATH의 JDK 8 `bin`은 시스템 PATH(17)보다 뒤에 오므로 그대로 둠

### #13 MySQL 8.4.9 설치 (새 컴퓨터)
- **AI 제시 선택지**: HANDOFF대로 직접 설치 / 이미 있는 Docker로 컨테이너 실행
- **입력**: `MySQL 8.4.9 직접 설치해줘`
- **AI 작업**: winget으로 `Oracle.MySQL` 8.4.9 설치 (의존성 VC++ 재배포 패키지 포함), `mysqld --version`으로 8.4.9 확인
- **확인 결과**: #7과 같이 MSI 무인 설치는 프로그램 파일만 설치함 (서비스 없음, 데이터 디렉터리 없음, 3306 미사용)
  - root 비밀번호 설정이 필요한 초기 구성은 사용자가 MySQL Configurator로 직접 진행하도록 안내 (HANDOFF 4-3 절차)
  - DBeaver는 이 컴퓨터에 이미 설치되어 있음 확인

### #14 DB 연결 확인 (새 컴퓨터)
- **입력**: `설정 완료했어, 연결 확인해줘`
- **AI 작업**: 확인해 보니 `MySQL84` 서비스는 실행 중(자동 시작)이지만 `backend/.env`가 없음 → `.env.example`을 복사해 `.env`를 만들고 `git check-ignore`로 Git 제외 확인, 비밀번호는 사용자가 직접 입력하도록 안내
- **입력**: `비밀번호 입력했어, 연결 확인해줘`
- **검증** (#8과 같은 2단계):
  1. `mysql` CLI로 `flow` 계정 로그인 → `flow@localhost`, MySQL 8.4.9, DB 문자셋 `utf8mb4` / `utf8mb4_unicode_ci`, 권한 `flow.*`만 확인 (비밀번호는 `.env`에서 읽고 출력하지 않음)
  2. 임시 테스트로 `DataSource`와 MyBatis `SqlSession`에서 쿼리 실행 → `flow` DB, Connector/J 9.7.0 확인. `contextLoads` 포함 테스트 3개 통과 후 임시 테스트 파일 삭제

### #15 테이블 스키마 설계
- **입력**: `이제 작업 이어서 시작하자`
- **AI 질문** (선택지 제시, 추천안 표시):
  - 테이블 구조: 하나(`type` 컬럼으로 구분) / 둘(고정·커스텀 분리)
  - 커스텀에 고정 확장자(`exe`) 입력 시: 거부하고 안내 / 고정 체크로 자동 전환
  - 확장자 규칙: 영문 소문자·숫자만 허용 + 업로드 시 파일명의 모든 점 구간 검사 / 확장자 안의 점 허용(`tar.gz`)
  - 변경 이력: 생성·수정 시각만 / 이력 테이블 추가
- **선택**: 테이블 구조는 **사용자가 직접 DDL을 작성해서 답함** (`FILE_EXTENSION_POLICY`, 한 테이블, `TYPE` + `IS_BLOCKED`, `EXTENSION` UNIQUE, CHECK 제약). 나머지 3개는 추천안 선택
- **AI 작업**: 사용자 DDL을 그대로 쓰고, 선택한 규칙을 DB에서도 강제하도록 제약 2개만 추가
  - `CK_FILE_EXTENSION_FORMAT`: `REGEXP_LIKE(EXTENSION, '^[a-z0-9]{1,20}$', 'c')` — 앱 검증을 거치지 않은 값(대문자, 점, 공백, 유니코드)도 저장 불가. 기본 collation(`utf8mb4_unicode_ci`)이 대소문자를 구분하지 않아서 `'c'` 옵션으로 대소문자 구분 매칭
  - `CK_FILE_EXTENSION_CUSTOM_BLOCKED`: 커스텀은 추가 = 차단이므로 `IS_BLOCKED = 1` 강제
  - 엔진과 문자셋(`InnoDB`, `utf8mb4`) 명시
  - `seed.sql`: 고정 확장자 7개를 `IS_BLOCKED = 0`(기본 체크 해제)으로 명시해서 입력. 컬럼 기본값(1)은 커스텀 추가용
- **검증**: 로컬 DB에 `schema.sql` → `seed.sql` 적용 후 제약 테스트
  - 실패해야 하는 10건 모두 거부 확인: 고정과 겹치는 `exe`(UNIQUE), `EXE2`·`.sh`·`tar.gz`·`한글`·빈 문자열(FORMAT), 잘못된 TYPE, 커스텀 `IS_BLOCKED = 0`, 21자(길이), `IS_BLOCKED = 2`
  - 성공해야 하는 입력(커스텀 `sh`, 20자, 고정 `exe` 체크)은 트랜잭션 안에서 확인 후 롤백
- **AI가 새로 발견한 점**: 로컬(Windows)의 `lower_case_table_names`가 1이라 테이블이 소문자(`file_extension_policy`)로 저장됨. AWS RDS 같은 Linux MySQL은 기본값이 0이라 테이블명이 대소문자를 구분함 → 로컬에서는 대소문자가 섞인 쿼리도 동작하지만 배포 환경에서는 실패할 수 있으므로, SQL에서 테이블명을 항상 `FILE_EXTENSION_POLICY`로 일관되게 쓰기로 함

### #16 확장자 정책 API (첫 번째 컴퓨터로 복귀)
- **입력**: `안녕 다른컴퓨터에서 조금하다가 돌아왔어 파일이 수정되었을테니 확인해봐`
- **AI 작업**: `git fetch`와 커밋 diff로 다른 컴퓨터 작업(#10~#15) 확인
  - 이 컴퓨터 DB에는 스키마가 적용되지 않은 상태, HANDOFF의 기록 범위 표기(#1 ~ #11)가 실제(#15)와 다름을 발견해 보고
- **입력**: `자 이어서 해보자`
- **AI 작업 1**: 이 컴퓨터 DB에 `schema.sql` → `seed.sql` 적용, 핵심 제약 3건(형식, 고정과 중복, 커스텀 허용 금지) 거부 확인. HANDOFF 기록 범위 수정
- **AI 질문** (선택지 + 미리보기): API 설계안 / 에러 응답 형식 `{ code, message }` vs ProblemDetail / 입력 앞의 점을 하나만 제거 vs 모두 제거 (스키마 주석이 "앞의 '.' 제거"라고만 되어 있어 해석이 두 가지)
- **선택**: 모두 추천안 (설계안 그대로, `{ code, message }`, 점 하나만 제거)
- **AI 작업 2**: Controller → Service(@Transactional) → MyBatis Mapper(XML) 구조로 구현
  - `ExtensionRule`: 정규화 규칙을 한 곳에 둠 (strip → 앞의 점 하나 제거 → `toLowerCase(Locale.ROOT)` → `^[a-z0-9]{1,20}$`). DB CHECK와 같은 규칙. 업로드 검사에서 재사용 예정
    - `Locale.ROOT`: 터키어 로케일 등에서 `I`의 소문자 변환 결과가 달라지는 문제 방지
    - `strip()`: `trim()`과 달리 유니코드 공백도 제거
  - 200개 제한: 트랜잭션 안에서 `SELECT COUNT(*) ... WHERE TYPE = 'CUSTOM' FOR UPDATE`로 먼저 잠근 뒤 중복·개수 확인 → INSERT. InnoDB 기본 격리 수준(REPEATABLE READ)에서 조회 범위의 간격(gap)까지 잠겨 동시 INSERT가 대기함
  - 중복 추가: 먼저 조회해서 고정 확장자와 겹침(`FIXED_EXTENSION_CONFLICT`)과 커스텀 중복(`DUPLICATE_EXTENSION`)을 구분해 안내. UNIQUE 위반(`DuplicateKeyException`)도 같은 메시지로 변환 (최종 방어선)
  - 고정 변경/커스텀 삭제는 `WHERE ... AND TYPE = ?`로 종류를 제한 → 커스텀 삭제 API로 고정 확장자를 지울 수 없음
  - 예외 처리: 업무 예외는 `{ code, message }`, 없는 경로(404)·잘못된 메서드(405)·잘못된 ID 형식(400)이 500으로 떨어지지 않도록 처리 (구현 중 AI가 스스로 발견해 보완)
- **검증**: 테스트 34개 통과
  - `ExtensionRuleTest` (22건): 대소문자, 공백, 앞의 점, 20/21자, `..sh`, `tar.gz`, `../sh`, 한글, 전각 문자(`ｅｘｅ`) 등
  - `ExtensionPolicyApiTest` (10건, 테스트마다 롤백): 조회, 고정 체크 저장, 404/400, 정규화 저장 후 삭제, 중복 409, 고정과 겹침 409, 커스텀 삭제 API로 고정 삭제 불가, 잘못된 경로/ID
  - `ExtensionPolicyConcurrencyTest`: 남은 자리가 1개일 때 10건 동시 추가 → 1건만 성공, 9건 `CUSTOM_LIMIT_EXCEEDED`
  - **테스트 자체 검증**: `FOR UPDATE`를 임시로 빼고 3회 실행 → 3회 모두 10건이 다 성공(총 209개)하며 테스트 실패. 잠금을 되돌리면 다시 통과 → 테스트가 경쟁 상태를 실제로 잡아낸다는 것을 확인

### #17 백엔드 패키지 구조 변경
- **입력**: `구조가 controller, service, dto, dao, mapper 패키지로 나눠져 있어야지`
- **AI 판단**: AI는 기능 단위 패키지(`com.flow.extension`)로 만들었지만 사용자는 레이어 단위를 원함. `dao`와 `mapper`의 역할 구분이 여러 가지로 해석되어 3가지 안을 미리보기로 제시
  - dao = 클래스, mapper = 인터페이스 / dao = VO, mapper = 인터페이스 / dao = 인터페이스, mapper = XML
- **선택**: **dao = `@Mapper` 인터페이스, mapper = XML** (`resources/mapper/ExtensionPolicyDao.xml`)
- **AI 작업**: 로직은 그대로 두고 파일 이동과 package/import만 수정
  - `controller`, `service`, `dao`(`ExtensionPolicyMapper` → `ExtensionPolicyDao`), `dto`(요청/응답 DTO + DB 행 `ExtensionPolicy`), `common`(예외 처리 + `ExtensionRule`)
  - 테스트도 같은 패키지로 이동, Service의 필드명을 타입에 맞춰 `mapper` → `dao`
- **문제와 해결**: 이동 후 테스트 실행 시 컨텍스트 로딩 실패. 원인은 코드가 아니라 `target/classes`에 이름을 바꾸기 전의 `ExtensionPolicyMapper.xml`이 남아 MyBatis가 옛 XML까지 읽은 것 → `mvnw clean test`로 해결
- **검증**: `clean test` 34개 통과, 빌드 결과물에 새 XML만 있는 것 확인

### #18 파일 업로드 API
- **입력**: `파일 업로드 API 시작하자`
- **AI 질문** (선택지 + 미리보기): 저장 방식(로컬 디스크 + UUID / 로컬 + DB 기록 / S3), 제한(1건 1개 10MB / 여러 개), 내용 검사(실행 파일 위장만 차단 / 안 함 / Apache Tika)
- **선택**: **S3** (AI 추천은 로컬 디스크), 1건 1개 10MB, 실행 파일 위장만 차단
- **AI 확인**: 이 컴퓨터에 AWS CLI와 자격 증명 없음 → 사용자는 AWS 계정만 있고 버킷은 없음. **코드를 먼저 짜고 테스트에서는 S3를 Mock으로 대체**하기로 함
- **AI 작업**:
  - 처리 순서: 파일명 검사 → 확장자 정책(모든 점 구간) → 내용(매직 넘버) → S3 저장
  - `UploadFileRule`: 경로 제거(basename), 끝의 점·공백 제거(Windows가 무시하므로 `a.exe.`는 exe), 첫 구간을 뺀 모든 점 구간을 확장자 후보로 사용, 제어 문자와 `:*?"<>|` 거부(NTFS 스트림 `a.txt::$DATA`, 널 바이트 `a\0.exe.jpg` 등), 파일명 255자 제한
  - `ExecutableSignature`: Windows PE, ELF, Mach-O, 스크립트(`#!/`) 판별. 형식에 맞는 확장자(exe, so, sh 등)가 아니면 위장으로 차단 — 확장자가 맞으면 판단은 확장자 정책에 맡김
    - PE는 `MZ`만 보면 MZ로 시작하는 텍스트도 걸리므로 `e_lfanew`가 가리키는 `PE\0\0`까지 확인 (AI가 오탐 가능성을 따져 보완)
    - `CAFEBABE`(Mach-O 유니버설)는 Java class 파일과 매직 넘버가 같아 제외
  - S3 저장: 키는 `uploads/{UUID}` (원본 파일명·확장자 미사용 → 경로 조작, 덮어쓰기, 확장자 기반 실행 방지), `Content-Type`은 클라이언트 값을 믿지 않고 `application/octet-stream`, 원본 파일명은 URL 인코딩해서 메타데이터에 보관
  - 자격 증명은 AWS SDK 기본 체인 사용 (로컬 `~/.aws/credentials`, 배포는 IAM 역할) → 앱 설정과 `.env`에 액세스 키를 두지 않음
  - 오류: `EMPTY_FILE`, `INVALID_FILE_NAME`, `FILE_NAME_TOO_LONG`, `BLOCKED_EXTENSION`, `DISGUISED_EXECUTABLE`(400), `FILE_TOO_LARGE`(413), `STORAGE_UNAVAILABLE`(503, 버킷 미설정·S3 오류)
  - 로그: 성공과 차단 모두 남김. 제어 문자가 있는 파일명은 로그 주입 방지를 위해 기록하지 않음
- **검증**: 테스트 56개 통과 (새로 22개)
  - `UploadFileRuleTest`, `ExecutableSignatureTest`: 이중 확장자, 끝의 점·공백, 경로, 금지 문자, MZ로 시작하는 텍스트(오탐 없음), 범위를 벗어난 PE 오프셋
  - `FileUploadApiTest` (S3 Mock): UUID 키와 `octet-stream` 저장, 고정 확장자 체크 전후, `invoice.exe.txt`·`SETUP.EXE`·`setup.exe.` 차단, 커스텀 `sh` 차단, `report.jpg`(PE)·`run`(ELF)·`notes.txt`(`#!/bin/sh`) 위장 차단, 차단 시 S3 호출 없음, S3 오류 시 503
  - **실제 서버로 확인**: MockMvc는 multipart 크기 제한을 적용하지 않아 서버를 띄워 curl로 확인 → 5MB는 검사 통과 후 503(버킷 미설정), 12MB·60MB는 연결이 끊기지 않고 413과 안내 메시지
- **남은 것**: 실제 S3 버킷 연동 확인 (버킷, IAM 사용자, `aws configure` 준비 후)

### #19 프론트 화면
- **입력**: `프론트 화면부터 하자` (S3 준비보다 먼저)
- **AI 질문** (선택지 + 미리보기): 화면 구성(한 페이지 두 영역 / 탭 / vue-router), HTTP(fetch / axios), UI(직접 CSS / Element UI)
- **선택**: **탭 분리** (AI 추천은 한 페이지), **axios** (AI 추천은 fetch), 직접 작성한 CSS
- **AI 작업**:
  - 포트 충돌(미정 사항) 정리: Vue 개발 서버 3000, `/api`는 8080으로 프록시 → 코드는 상대 경로만 사용해 배포 방식과 무관
  - `api/index.js`: axios 인스턴스와 API 함수, 서버 `{ code, message }`를 화면 문장으로 바꾸는 `errorMessage`
  - `ExtensionPolicy.vue`: 과제 원본 화면 배치(고정 체크박스, 입력 + 추가, `n/200`, 칩 + X)
    - 클라이언트 검증은 서버 `ExtensionRule`과 같은 규칙(`utils/extension.js`)으로 즉시 안내만 하고 최종 판단은 서버
    - 입력 `maxlength=20`, 200개가 차면 입력·추가 비활성, 요청 중 버튼 비활성(중복 제출 방지)
    - 저장 실패 시 체크 상태를 되돌림, 삭제가 404면(다른 곳에서 이미 삭제) 목록에서도 제거해 DB와 맞춤, 로딩 실패 시 "다시 시도"
  - `FileUpload.vue`: 파일 선택 → 업로드, 진행률, 10MB 초과는 보내기 전에 안내, 이번 접속의 결과 목록
  - 탭 전환 시 입력·결과가 유지되도록 `v-show`, 스캐폴드 예제(HelloWorld, 로고) 삭제, 페이지 제목·`lang="ko"` 설정
- **AI가 스스로 잡은 버그**: 체크박스 저장 실패 시 되돌리기
  - 처음 코드는 실패하면 `item.blocked`를 원래 값으로 되돌렸는데, 데이터가 바뀐 적이 없어서(체크박스 DOM만 바뀜) Vue가 다시 그리지 않음 → 화면은 체크된 채로 남아 DB와 불일치
  - 데이터를 먼저 체크박스 값으로 맞춘 뒤 실패 시 되돌리도록 수정 → 실제 화면에서 백엔드를 끄고 확인
- **검증** (백엔드 + 개발 서버를 띄우고 내장 브라우저로 조작):
  - 정책: `exe` 체크 → 새로고침 후 유지, `" .SH "` → `sh`로 추가(1/200), 중복·`EXE`(고정)·`tar.gz`(형식) 안내, 삭제 후 0/200
  - 업로드(스크립트로 테스트 파일 생성): `deploy.sh` 커스텀 차단, `report.jpg`(PE) 위장 차단, `setup.exe` 차단, `ok.txt`는 503(버킷 미설정), 11MB는 서버로 보내지 않고 안내
  - 장애: 백엔드 중지 후 체크 → 체크 상태 되돌아감, 새로고침 시 안내 + 다시 시도 → 백엔드 재시작 후 복구
    - 이때 안내 문구가 `요청을 처리하지 못했습니다. (HTTP 500)`로 나옴 → 개발 서버 프록시가 JSON이 아닌 500을 돌려주기 때문. 백엔드 에러는 항상 `{ code, message }`이므로 그 형식이 아니면 "서버와 통신하지 못했습니다"로 안내하도록 수정
  - 모바일(375px): 가로 스크롤 없음, 행이 세로로 쌓임
  - lint 통과, build 성공. 테스트로 바꾼 DB 값(`exe` 체크)은 seed 상태로 되돌림
- **확인했지만 손대지 않은 것**: 빌드 경고 `export 'default' (imported as 'style0') was not found`
  - 커밋된 스캐폴드 원본을 임시 폴더에서 빌드해 비교 → 원본에도 `<style>` 블록마다 같은 경고 → 이번 변경 때문이 아니고 CSS 결과물도 정상이라 범위 밖으로 두고 HANDOFF 미정 사항에 기록

---

## 2. 사용한 스킬 / 플러그인 / 도구

| 도구 | 사용처 | 이유 |
|------|--------|------|
| Claude Code | 과제 분석, 환경 확인, 문서 작성 | 코딩 에이전트로 파일 읽기·명령 실행·작성을 한 곳에서 처리 |
| Spring Initializr (start.spring.io) | backend 생성 | 버전/의존성 호환 범위를 메타데이터로 확인하고 생성 |
| Vue CLI 5 (`@vue/cli`) | frontend 생성 | 선택한 Vue 2 구성 |
| AWS SDK for Java v2 (`s3` 2.55.8) | 업로드 파일 S3 저장 | 배포처 AWS에 맞춰 선택, 자격 증명 기본 체인 사용 |
| Mockito `@MockitoBean` | 업로드 API 테스트 | 버킷 없이 S3 호출 여부·요청 내용(키, Content-Type) 검증 |
| axios 1.20 | 프론트 API 호출 | 사용자 선택. 업로드 진행률(`onUploadProgress`) 표시 |
| Claude 내장 브라우저 (Browser pane) | 프론트 화면 동작 확인 | 실제 화면 조작, 장애 상황(백엔드 중지) 재현, 모바일 폭 확인 |
| winget | JDK 17, Node.js 22 설치 | Windows 기본 패키지 관리자로 버전 고정 설치 |
| `~/.claude/CLAUDE.md` (전역 지침) | 모든 작업 | 요청 범위 밖 수정 방지, 불확실성 명시, 커밋 메시지 형식 통일 |

---

## 3. 판단 근거 회고

(작업 진행하며 작성)
