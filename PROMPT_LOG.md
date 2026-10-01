# PROMPT_LOG

AI 활용 기록 (과제 4번 항목)

- 사용 AI: Claude Code (Claude 데스크톱 앱 Code 탭, 모델 Claude Opus 5.5)
- 기록 범위: 프로젝트 작업만 (개발 PC 환경 구성·도구 설치·PC 이동 기록은 제외해서 번호가 비어 있음)
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
- **AI 응답**: PROMPT_LOG.md 작성 후 과제 파일과 함께 첫 커밋
- **결과**: 과제 문서와 PROMPT_LOG.md 첫 커밋 및 push

### #4 빌드 도구·프론트 구성 결정
- **AI 질문** (선택지 제시, 추천안 표시):
  - Oracle 운영 방식: OCI Always Free Autonomous DB 하나로 / 로컬 XE + 배포 OCI / 로컬 XE만
  - 빌드 도구와 Java 버전: Gradle + 17 / Maven + 17 / Maven + 11
  - Vue 2 구성: Vue 2.7 + Vite / Vue 2.7 + Vue CLI
- **선택**: OCI Autonomous DB 하나로 개발/배포, **Maven + Java 17** (AI 추천은 Gradle), **Vue 2.7 + Vue CLI** (AI 추천은 Vite)

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

### #11 배포처 변경: AWS
- **입력**: `그전에 내용을 수정할게 aws로 배포할거야`
- **AI 작업**: HANDOFF 결정사항에 배포 항목(AWS) 추가, 다음 할 일과 미정 사항에서 기존 후보(Aiven, TiDB Cloud, Cloudtype)를 AWS로 교체
  - 앱 실행과 운영 DB에 쓸 AWS 서비스는 아직 정하지 않았으므로 미정으로 남김

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

### #16 확장자 정책 API
- **입력**: `자 이어서 해보자`
- **AI 질문** (선택지 + 미리보기): API 설계안 / 에러 응답 형식 `{ code, message }` vs ProblemDetail / 입력 앞의 점을 하나만 제거 vs 모두 제거 (스키마 주석이 "앞의 '.' 제거"라고만 되어 있어 해석이 두 가지)
- **선택**: 모두 추천안 (설계안 그대로, `{ code, message }`, 점 하나만 제거)
- **AI 작업**: Controller → Service(@Transactional) → MyBatis Mapper(XML) 구조로 구현
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
- **AI 확인**: AWS 계정만 있고 버킷·자격 증명은 아직 없음. **코드를 먼저 짜고 테스트에서는 S3를 Mock으로 대체**하기로 함
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

### #20 S3 실제 연동
- **입력**: `CONSIDERATIONS.md 작성하자` → (중단) → `S3 연동해보자`
- **AI 작업**: S3 버킷·IAM 사용자 준비 절차 안내
  - 콘솔 작업(버킷, IAM 사용자, 액세스 키)과 `aws configure` 키 입력은 사용자가 직접 하도록 안내 — 액세스 키는 채팅으로 받지 않음
  - 앱 IAM 사용자 권한은 `arn:aws:s3:::{버킷}/uploads/*`에 대한 `s3:PutObject`만 (키가 유출돼도 읽기·삭제·목록 조회 불가)
- **입력**: `min420-flow-uploads` (버킷 이름)
- **검증**:
  - `aws sts get-caller-identity` → `user/flow-app`, 리전 `ap-northeast-2`
  - **권한이 좁은지 확인** (거부되어야 하는 동작만 시험해서 버킷에 흔적을 남기지 않음): `uploads/` 밖 쓰기, 목록 조회, 읽기, 삭제, 퍼블릭 액세스 설정 조회 → 모두 거부
  - `.env`에 `AWS_S3_BUCKET` 추가 후 앱으로 실제 업로드 → 201, `uploads/{UUID}`로 저장
  - 한글 파일명이 깨짐(`S3 ���� Ȯ��.txt`) → 원인 확인: 같은 터미널에서 서버의 한글 응답 메시지는 정상 → Windows Git Bash의 curl이 파일명을 CP949 바이트로 보낸 것으로 판단. 브라우저처럼 UTF-8로 보내는 Node `fetch`로 다시 올려 `보고서 최종(1).txt`가 정상 저장되는 것 확인 → 앱 문제가 아니라 테스트 도구 문제
  - `MZ` + 0으로 채운 `fake.jpg`는 통과 (PE 헤더가 없으므로 실행 파일 아님 → 오탐 없음을 실제로 확인), 진짜 PE를 `사진.jpg`로 위장한 파일은 S3에 가기 전에 `DISGUISED_EXECUTABLE`로 차단
- **남은 확인** (앱 계정은 읽기 권한이 없어 사용자가 콘솔에서 확인): 퍼블릭 액세스 차단 설정, 업로드된 객체의 Content-Type·메타데이터, 테스트 객체 3개 정리

### #21 AWS 실제 배포
- **입력**: `CONSIDERATIONS.md 작성하기전에 전체적으로 실제배포하고 확인하고싶어`
- **AI 질문** (선택지 + 구성도 미리보기): 구성(EC2 + RDS / EC2에 MySQL까지 / Elastic Beanstalk), 무료 혜택 종류, 리소스 생성 주체(AI가 CLI로 / 직접 콘솔), HTTPS 여부
- **선택**: **EC2 1대 + RDS MySQL**, 2025년 7월 이후 가입(크레딧 방식), **AI가 CLI로**, HTTP로 시작 (모두 추천안)
  - 프론트는 Spring Boot jar에 포함 → 도메인 하나, CORS 불필요, 프론트 코드는 상대 경로 `/api` 그대로
- **배포용 IAM 사용자 `flow-deploy`** (사용자가 콘솔에서 생성): EC2·RDS 관리형 정책 + 인라인 정책(`flow-*` 역할만 생성/전달, 가격 조회)
  - 사용자가 붙여 넣은 인라인 정책에 콘솔 경고 발생 → AI가 원인을 추측(PassRole 와일드카드, 역할 전용 동작에 instance-profile ARN 혼재)하고 수정안 제시: 문장 분리, 계정 ID 명시, `iam:PassedToService = ec2.amazonaws.com` 조건 → 경고 해소
- **비밀번호**: 사용자가 `~/.flow-deploy.env`(저장소 밖)에 직접 작성, AI는 길이·금지 문자만 확인하고 값은 출력하지 않음
  - 처음 4자 → RDS 관리자 비밀번호는 AWS 규칙상 최소 8자라 생성 불가함을 안내 → 사용자가 사용자 결정으로 앱 계정은 유지하려 했으나 최종적으로 둘 다 11자로 변경
- **비용 확인 후 승인**: Pricing API로 서울 리전 실제 단가 조회 → EC2 `t4g.micro` $0.0104/h, RDS `db.t4g.micro` $0.025/h, RDS gp3 $0.131/GB-월, EBS gp3 $0.0912/GB-월, 퍼블릭 IPv4 $0.005/h → **월 약 $32.8** 제시 후 사용자 승인
  - `t4g`(ARM)를 고른 이유: `t3.micro`($0.013/h)보다 약 20% 저렴, Java 17은 ARM에서 문제없음
- **생성한 리소스** (모두 태그 `Project=flow`, 기본 VPC):
  - 보안 그룹 `flow-ec2-sg`(80 전체, 22는 관리자 IP /32만), `flow-rds-sg`(3306은 `flow-ec2-sg`에서만)
  - 키 페어 `flow-key`(ed25519, `~/.ssh`에만 보관), IAM 역할 `flow-ec2-role`(S3 `uploads/*` PutObject만)
  - RDS `flow-db` (MySQL 8.4.11, 퍼블릭 접근 없음, 스토리지 암호화, 백업 1일), EC2 `flow-app` (AL2023 ARM, IMDSv2 필수, EBS 암호화), Elastic IP
- **배포 구성** (`deploy/`):
  - `ec2-userdata.sh`: 최초 부팅 시 Java 17·MySQL 클라이언트 설치, 스왑 1GB(메모리 1GB 보완), 로그인 불가 실행 계정 `flow`
  - `build.sh`: 프론트 빌드 → `pom.xml`의 리소스 설정으로 `frontend/dist`를 jar의 `static/`에 포함(없으면 건너뜀) → 테스트 포함 패키징
  - `flow.service`(systemd): `flow` 계정으로 실행, `CAP_NET_BIND_SERVICE`로 root 없이 80 포트, 자동 재시작
  - `deploy.sh`: jar와 서비스 파일 업로드 후 재시작
  - `/etc/flow/flow.env`(root만 읽기): DB 접속 정보(`sslMode=REQUIRED`), 포트, 버킷 — 저장소에 없음
- **DB 초기화** (EC2를 거쳐 RDS에 적용, 비밀번호는 SSH 표준 입력으로만 전달):
  - 앱 계정은 **`SELECT/INSERT/UPDATE/DELETE`만**, 접속 호스트는 VPC 내부(`172.31.%`)만 → 테이블 구조 변경은 관리자 계정으로만
  - RDS는 `lower_case_table_names = 0`이라 테이블이 대문자 `FILE_EXTENSION_POLICY`로 저장됨을 확인 (로컬 Windows는 소문자) → #15에서 정한 "테이블명 항상 대문자" 규칙이 실제로 필요했음
- **진행 중 부딪힌 문제** (모두 Windows 환경에서 `aws.exe`를 Git Bash로 쓰면서 생김):
  1. `--user-data file:///tmp/...` → `aws.exe`가 Git Bash 경로를 못 찾음 → 이때 뒤 명령이 빈 인스턴스 ID로 대기 → AI가 중단하고 남은 리소스가 없는지 태그로 확인
  2. Windows 경로로 바꾸자 한글 주석 때문에 CP949로 디코딩 실패 → `fileb://`(바이트 그대로)로 해결
  3. `DeviceName=/dev/xvda`를 Git Bash가 `D:/Git/dev/xvda`로 자동 변환 → `MSYS_NO_PATHCONV=1`로 해결
  4. SSH 키 `invalid format` → CRLF는 없었고, `sed`로 파일을 다시 쓰자 해결됨 → 파일 끝 줄바꿈 누락으로 추정 (확실하지 않음)
- **검증** (공개 URL `http://43.202.189.27`, 내장 브라우저):
  - 정책: `exe` 체크 → 재조회 시 유지, `.SH` → `sh` 추가(1/200)
  - 업로드: `배포 확인.txt` **EC2 IAM 역할로 S3 저장 성공**(액세스 키 없음), `setup.exe`·`run.sh` 차단, PE를 `photo.png`로 위장 → 차단. 서버 로그에 성공·차단 사유 기록 확인
  - 네트워크: 외부에서 RDS 3306, EC2 8080 접속 불가
  - **재부팅 후 26초 만에 자동 복구** (systemd enabled, 스왑 유지)
  - 확인 중 발견한 문구 오류 `'exe' 차단를 저장했습니다`(조사) → `차단을` / `차단 해제를`로 수정, 재배포 후 확인
  - 테스트로 바꾼 정책(`exe` 체크, `sh`)은 seed 상태로 되돌림

### #22 배포 파일 줄바꿈 LF 고정
- **AI가 발견**: #21 커밋 시 `deploy/*.sh`, `flow.service`에 "LF가 CRLF로 바뀐다" 경고 → 이 PC는 `core.autocrlf=true`라 다른 Windows PC에서 clone하면 CRLF로 받아지고, 그대로 EC2에 올리면 `$'\r': command not found` 등으로 실패할 수 있음 → 사용자에게 제안
- **입력**: `응 수정하고 커밋 push해줘`
- **AI 작업**: 루트 `.gitattributes`에 `*.sh`, `*.service`를 `text eol=lf`로 지정 (`backend/.gitattributes`의 `mvnw` 처리와 같은 방식)
- **검증 중 AI가 스스로 틀린 부분**: 새로 clone한 뒤 `grep -c $'\r'`로 확인 → 모든 파일(설정이 원래 있던 `mvnw`까지)이 CRLF로 나와 수정이 안 먹은 것처럼 보임
  - `mvnw`까지 CRLF라는 점이 이상해 다른 방법으로 재확인 → `git ls-files --eol`에서 `w/lf`, `od -c`로도 줄 끝이 `\n`뿐 → **설정은 정상이었고 확인 방법(Git Bash의 grep CR 패턴 처리)이 잘못된 것**
  - 이후 줄바꿈 확인은 `git ls-files --eol`로 함. #21의 SSH 키 CRLF 확인도 같은 방법이라 그때 결론("CRLF 없음")도 신뢰하기 어려움 → 원인은 여전히 미확정
- **최종 확인**: 새 clone에서 `deploy/` 4개와 `mvnw`는 `w/lf`, 설정 대상이 아닌 `README.md`는 `w/crlf`

### #26 CONSIDERATIONS.md 작성
- **입력**: `CONSIDERATIONS.md 작성하자`
- **결과**: 과제 3번 항목별로 판단·근거 작성. 쓰는 중에 긴 한글 파일명은 1바이트 파일이어도 `413`으로 거부되는 문제를 발견 (Tomcat multipart 헤더 제한, 미조치)

### #27 에러 응답 구조 정리 (ErrorCode enum)
- **입력**: `백엔드쪽 코드를 좀더 간결화할수있을까?` → AI는 전체 724줄로 크게 줄일 곳이 없고, 리팩토링하면 재배포가 필요하다는 점(이 PC에는 SSH 키 없음)을 들어 손대지 않는 쪽을 추천 → `GlobalExceptionHandler, ApiException 이거 구조가 마음에안들어 좀 간결화하고싶은데`
- **AI 질문** (미리보기): ErrorCode enum으로 모음 / ApiException 정적 메서드 / 핸들러만 정리 — 세 안 모두 응답 형식 `{ code, message }`는 유지
- **선택**: **ErrorCode enum**
- **AI 작업**:
  - `common/ErrorCode.java` 추가: 상태 코드·코드·메시지 17개를 한 곳에. 메시지 속 숫자(20자, 200개, 255자, 10MB)는 enum에 다시 적지 않고 `%d`로 두고 기존 상수에서 넘김 → 숫자가 두 곳에 생겨 한쪽만 바뀌는 일 방지
  - `ApiException(ErrorCode, Object... args)`: 서비스 호출이 `new ApiException(ErrorCode.DUPLICATE_EXTENSION, extension)` 한 줄로
  - `GlobalExceptionHandler`: 문자열 코드·메시지를 모두 enum으로, 응답 생성은 `respond()` 하나로
  - `FileUploadService`: `storageUnavailable()` 제거, `reject(code, name, args...)`
- **동작 변경 (사용자에게 먼저 알림)**: enum은 코드 하나에 메시지 하나라, 코드 하나에 메시지가 두 개였던 2곳을 통일. 둘 다 화면에서는 나오지 않는 경우
  - `INVALID_FILE_NAME`: 파일명이 null일 때의 "파일명이 없습니다." → "파일명이 없거나 사용할 수 없는 문자가 포함되어 있습니다."
  - `INVALID_REQUEST`: 형식 오류(400)의 "요청 형식이 올바르지 않습니다." → 404/405와 같은 "요청을 처리할 수 없습니다."
- **검증**:
  - `mvnw clean test` 56개 통과 (테스트 변경은 동시성 테스트의 `getCode()` → `getCode().name()` 한 줄)
  - **테스트는 code만 확인하고 메시지는 확인하지 않아서**, 바꾸기 전 문자열 연결로 만들던 문구와 enum이 만드는 문구를 jshell로 17건 대조 → 모두 일치. 사용자 입력에 `%`가 들어가도 형식 문자열로 해석되지 않음 확인
    - 처음 실행에서 17건 모두 다르다고 나옴 → 결과가 아니라 jshell이 UTF-8 스크립트를 CP949로 읽은 문제였음, 인코딩 지정 후 재실행
  - 로컬 서버에 실제 요청 9건(404/409/400 계열, 위장 실행 파일 2종)으로 상태 코드·code·문구 확인

### #28 CONSIDERATIONS.md 다듬기 (사용자 검토)
- **입력**: `CONSIDERATIONS 이거 작성한거 너무 보기 힘들어 너무 길어서 읽는데 시간도 오래걸리고 가독성도 안좋아` → 길이를 절반으로 줄임
- **사용자가 잡은 AI의 과장**: `우리는 동시편집기능없어`, `우리는 로그도없어`, `이중확장자 등록안되고 대소문자도 다 소문자로 저장하고` → 기능이 있는 것처럼 쓰거나 등록과 업로드 규칙을 섞어 쓴 부분을 실제 구현대로 수정

### #29 변경 사항 검토 후 재배포
- **입력**: `한번 파일을 읽어볼래?` (#26~#28 변경 검토)
- **AI 작업**: 커밋 3개(#26~#28)의 기록·코드·문서를 읽고 대조
  - 코드(#27 ErrorCode): 기록대로이고 동작 변경은 기록된 메시지 2개뿐
  - **배포 서버가 #27 이전 코드**임을 발견: #27 이후 다시 배포되지 않았음 → 배포 서버에 형식 오류 요청을 보내 이전 문구(`요청 형식이 올바르지 않습니다.`)가 나오는 것과 jar 시각으로 확인
  - 문서 불일치 2건 보고 (HANDOFF와 CONSIDERATIONS가 서로 다른 버전을 가리킴) → 이후 `응 같이 고치고 커밋 push해줘`로 수정
- **입력**: `일단 1번 배포해줘`
- **AI 작업**: HEAD(`b449b72`)를 변경 없는 상태에서 `deploy/build.sh`(테스트 56개 통과) → `deploy/deploy.sh`
- **검증** (저장되지 않는 요청만 사용): 형식 오류·잘못된 ID가 새 문구 `요청을 처리할 수 없습니다.`, 고정 확장자 겹침 409, 위장 실행 파일(PE→png, 확장자 없는 ELF)과 빈 파일 400 문구 확인, 정책 상태는 seed 그대로(커스텀 0)
  - 확인 중 본 문구: `확장자가 없어 이를 숨기고 있어` (없어/있어 반복) — #18부터 같았던 문구, 이번 변경과 무관해 그대로 둠

### #30 README.md 작성
- **입력**: `README.md 작성하자`
- **AI 작업**: 과제 제출 요건(GitHub 주소, 배포 URL, table schema, 실행 방법) 중심으로 짧게 작성하고, 자세한 내용은 기존 문서(SETUP, CONSIDERATIONS, HANDOFF, CLEANUP)로 연결
  - #28에서 사용자가 CONSIDERATIONS가 길어 읽기 힘들다고 한 점을 반영해 표 위주로 구성
  - table schema: 컬럼(타입·제약·설명), 제약 5개와 각각이 막는 것, 인덱스(PK·UNIQUE만 둔 이유), 엔진·문자셋, 초기 데이터 — `db/schema.sql`을 읽고 그대로 옮김
  - 맨 위에 "인증이 없어 누구나 정책을 바꿀 수 있음"을 명시 (배포 URL을 받는 사람이 먼저 알아야 할 점)
- **AI가 스스로 보완**: 실행 명령의 `&&`는 Windows PowerShell 5.1에서 동작하지 않음 → PowerShell 명령 병기, S3 저장 조건에 자격 증명뿐 아니라 `.env`의 `AWS_S3_BUCKET`도 필요함을 추가
- **검증**: README의 상대 링크 7개가 실제 파일을 가리키는지 확인

### #31 CONSIDERATIONS.md 범위 축소 (사용자 결정)
- **입력**: 핵심 항목 목록과 함께 `이정도만 고려하는거야`, 이어서 `운영쪽은빼자 내가 대답할자신이없어` → 검증/보안 4개 + 정책/데이터 2개, 6개 항목만 남김
- S3 퍼블릭 접근 차단은 AI가 쓰기 전에 익명 요청 403으로 확인했지만, 설정 자체는 AI 권한으로 볼 수 없었음 → 사용자가 운영 항목을 빼기로 결정

### #32 CONSIDERATIONS 주장 검증
- **입력**: `지금 considerations에있는것들은 다 검증된거야?`
- **결과**: AI가 코드만 보고 쓴 주장 4개를 실제 요청으로 확인. 3개는 맞았고, "요청당 파일 1개만 처리"는 "여러 개를 보내면 첫 번째만 처리"로 문장을 고침

### #34 이중 확장자 처리 재확인
- **입력**: `다 저거대로 처리중인거맞아? 이중 확장자는 처리안하고있는것같은데`
- **결과**: 실제 요청으로 확인 → 처리되고 있음. 고정 확장자 기본값이 모두 해제라 `exe`를 체크하기 전에는 `invoice.exe.txt`가 통과해서 처리 안 되는 것처럼 보였음

---

## 2. 사용한 스킬 / 플러그인 / 도구

| 도구 | 사용처 | 이유 |
|------|--------|------|
| Claude Code | 과제 분석, 환경 확인, 문서 작성 | 코딩 에이전트로 파일 읽기·명령 실행·작성을 한 곳에서 처리 |
| Spring Initializr (start.spring.io) | backend 생성 | 버전/의존성 호환 범위를 메타데이터로 확인하고 생성 |
| Vue CLI 5 (`@vue/cli`) | frontend 생성 | 선택한 Vue 2 구성 |
| AWS SDK for Java v2 (`s3` 2.55.8) | 업로드 파일 S3 저장 | 배포처 AWS에 맞춰 선택, 자격 증명 기본 체인 사용 |
| Mockito `@MockitoBean` | 업로드 API 테스트 | 버킷 없이 S3 호출 여부·요청 내용(키, Content-Type) 검증 |
| AWS CLI 2.37.6 | S3 권한 확인 | 앱 IAM 사용자가 `uploads/*` 쓰기 외에는 거부되는지 직접 확인 |
| Node.js `fetch` | 한글 파일명 업로드 확인 | Windows curl이 파일명을 CP949로 보내는 문제를 피해 브라우저와 같은 UTF-8 전송으로 검증 |
| AWS Pricing API | 배포 전 비용 추정 | 추측이 아닌 서울 리전 실제 단가로 월 비용 제시 |
| AWS CLI (EC2, RDS, IAM) | 배포 인프라 생성 | 명령이 그대로 기록되어 재현 가능, 태그로 리소스 추적 |
| systemd, OpenSSH | 앱 상시 실행, 배포 | 재부팅 자동 복구, root 없이 80 포트 |
| axios 1.20 | 프론트 API 호출 | 사용자 선택. 업로드 진행률(`onUploadProgress`) 표시 |
| Claude 내장 브라우저 (Browser pane) | 프론트 화면 동작 확인 | 실제 화면 조작, 장애 상황(백엔드 중지) 재현, 모바일 폭 확인 |
| `~/.claude/CLAUDE.md` (전역 지침) | 모든 작업 | 요청 범위 밖 수정 방지, 불확실성 명시, 커밋 메시지 형식 통일 |

---

## 3. 판단 근거 회고

### 그대로 쓴 것
| AI 결과 | 그대로 쓴 이유 | 기록 |
|---------|----------------|------|
| 정책 API 설계, 에러 형식 `{ code, message }` | 화면이 `message`를 그대로 보여주면 돼서 단순함 | #16 |
| 실행 파일 위장만 매직 넘버로 차단 (Tika 제외) | 과제의 위험(실행 파일)에 맞는 범위, 오탐이 적음 | #18 |
| 200개 제한 잠금(`FOR UPDATE`)과 동시성 테스트 | 잠금을 빼면 테스트가 실패하는 것까지 보여줘서 믿을 수 있었음 | #16 |
| EC2 1대 + RDS 배포 구성 | 비용을 실제 단가로 먼저 보여줘서 판단할 수 있었음 | #21 |

### 고쳐 쓴 것 (AI 추천과 다르게 정함)
| AI 추천 | 실제 결정 | 기록 |
|---------|-----------|------|
| Gradle, Vite | **Maven, Vue CLI** | #4 |
| 기능 단위 패키지(`extension/`) | **레이어 단위** `controller/service/dao/dto` | #17 |
| 테이블 구조 선택지 | **DDL을 직접 작성**해서 답함 | #15 |
| 로컬 디스크 저장 | **S3** | #18 |
| 한 페이지, fetch | **탭 분리, axios** | #19 |
| 예외 처리 구조 유지 권장 | **ErrorCode enum으로 정리** (구조가 마음에 들지 않아 직접 요청) | #27 |

### 버린 것
| AI 결과 | 버린 이유 | 기록 |
|---------|-----------|------|
| CONSIDERATIONS 초안 424줄 | 너무 길어 읽기 힘듦 → 줄이고, 다시 핵심 6개 항목만 남김 | #28, #31 |
| CONSIDERATIONS의 운영 항목(UUID 저장, S3 권한) | 직접 설명할 자신이 있는 범위만 남김 | #31 |

### AI가 놓쳤거나 틀렸는데 직접 잡은 것
| 잡은 것 | AI가 틀린 부분 | 기록 |
|---------|----------------|------|
| `우리는 동시편집기능없어` | CONSIDERATIONS에 "새로고침/동시 편집 ✅"로 기능이 있는 것처럼 씀 | #28 |
| `우리는 로그도없어` | 콘솔 출력 몇 줄을 "로그/모니터링 ✅"로 부풀림 | #28 |
| `이중확장자 등록안되고 대소문자도 다 소문자로 저장하고` | 등록 규칙과 업로드 검사를 섞어 써서 반대로 읽힘 | #28 |
| `지금 considerations에있는것들은 다 검증된거야?` | 코드만 보고 쓴 주장 4개가 있었음 → 실제 확인 후 1개(파일 여러 개 전송 시 동작)는 문장 수정 | #32 |
| `이중 확장자는 처리안하고있는것같은데` | 동작은 맞았지만 문서가 "기본값 해제", "등록 불가"를 설명하지 않아 오해 소지 | #34 |

### AI가 스스로 잡은 것 (참고)
- 체크박스 저장 실패 시 화면이 되돌아가지 않던 버그 (#19)
- 없는 경로·잘못된 ID가 500으로 떨어지던 문제 (#16)
- `MZ`로 시작하는 텍스트를 실행 파일로 오탐할 가능성 → PE 헤더까지 확인 (#18)
- 로컬(Windows)과 배포(Linux) MySQL의 테이블명 대소문자 차이 (#15, #21)
- 긴 한글 파일명이 1바이트여도 413이 나는 문제 (#26, 미조치)
