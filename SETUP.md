# SETUP - 다른 컴퓨터에서 받아서 실행하기

GitHub에서 저장소를 받은 뒤 로컬에서 동작시키기까지의 절차입니다. (Windows 기준)

**저장소에 없는 것** (컴퓨터마다 직접 준비): `backend/.env`, AWS 자격 증명(`~/.aws`), 로컬 MySQL 데이터, 배포용 SSH 키, Claude 지침(`~/.claude/CLAUDE.md`)

---

## 한 번에 설정하기 (setup.ps1)

아래 1~7번을 자동으로 합니다. Git으로 저장소를 받은 뒤 루트에서 실행하세요.

```bash
winget install --id Git.Git -e
git clone https://github.com/vkfkd420/flow.git
cd flow
powershell -ExecutionPolicy Bypass -File .\setup.ps1
```

- **실행 중 직접 하는 것**: MySQL root 비밀번호와 앱 계정(flow) 비밀번호 입력, 관리자 권한 확인 창에서 [예] (MySQL 서비스 등록 시 1번), S3 액세스 키 입력 (건너뛸 수 있음)
- 입력한 비밀번호는 화면에 보이지 않고 `backend/.env`에만 저장됩니다.
- **다시 실행해도 안전합니다.** 이미 된 단계는 건너뛰고, 기존 MySQL 데이터 폴더와 `.env`는 덮어쓰지 않습니다. 중간에 실패하면 원인을 해결한 뒤 다시 실행하면 이어서 진행됩니다.
- 마지막에 단계별 결과(설치/건너뜀/완료)와 남은 수동 작업을 보여줍니다.
- MySQL을 Configurator 등으로 이미 설치·구성했다면(서비스 `MySQL84` 또는 3306 사용 중) 초기화는 건너뛰고 root 비밀번호만 묻습니다.
- 스크립트로 해결되지 않으면 아래 수동 절차를 따르세요.

> 확인 범위: 이미 구성된 PC에서 모든 단계가 건너뛰어지고 테스트가 통과하는 것까지 확인했습니다. 새 PC에서의 설치·MySQL 초기화·계정 생성 경로는 아직 실제로 실행해 보지 않았습니다.

---

## 1. 도구 설치

```bash
winget install --id Git.Git -e
winget install --id EclipseAdoptium.Temurin.17.JDK -e
winget install --id OpenJS.NodeJS.22 -e
winget install --id Oracle.MySQL -e --version 8.4.9
winget install --id Amazon.AWSCLI -e
```

- DB 관리 도구는 DBeaver (`winget install --id DBeaver.DBeaver.Community -e`)
- **설치 후 터미널(또는 Claude 앱)을 다시 시작**해야 PATH가 적용됩니다.

설치 확인:

```bash
java -version
node -v
aws --version
echo $JAVA_HOME
```

- `java -version`이 17이 아니거나 `JAVA_HOME`이 다른 JDK(예: 8)를 가리키면 `mvnw`가 실패합니다. 사용자 환경 변수 `JAVA_HOME`을 Temurin 17 경로(`C:\Program Files\Eclipse Adoptium\jdk-17...`)로 바꾸세요.

## 2. 저장소 받기

```bash
git config --global user.name "강현민"
git config --global user.email "vkfkd420@naver.com"
git clone https://github.com/vkfkd420/flow.git
cd flow
```

## 3. 로컬 MySQL 구성

1. `C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql_configurator.exe` 실행
   - Development Computer, 포트 3306, root 비밀번호 설정, Windows 서비스 등록(자동 시작)
   - winget 설치는 프로그램 파일만 설치하므로 이 단계가 꼭 필요합니다.
2. DBeaver에서 root로 접속해 DB와 앱 전용 계정 생성

```sql
CREATE DATABASE flow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'flow'@'localhost' IDENTIFIED BY '원하는_비밀번호';
GRANT ALL PRIVILEGES ON flow.* TO 'flow'@'localhost';
```

3. 테이블과 초기 데이터 적용 (**순서대로**, DBeaver에서 `flow` DB에 실행하거나 아래 명령)

```bash
mysql -u flow -p flow < db/schema.sql
mysql -u flow -p flow < db/seed.sql
```

- `mysql` 명령이 없으면 `"/c/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe"`로 실행

## 4. S3 자격 증명 (업로드 기능용)

앱은 AWS SDK 기본 체인으로 자격 증명을 찾습니다. 액세스 키는 `.env`에 넣지 않습니다.

1. IAM 콘솔 → 사용자 `flow-app` → 보안 자격 증명 → **액세스 키 만들기** (컴퓨터마다 새 키를 권장. 한 컴퓨터를 잃어버려도 그 키만 비활성화하면 됨)
2. 등록 (키는 직접 입력)

```bash
aws configure
```

- Default region: `ap-northeast-2`, Output: `json`
- 확인: `aws sts get-caller-identity` 결과가 `user/flow-app`
- 이 단계를 건너뛰어도 정책 관리와 업로드 차단은 동작합니다. 정상 파일 업로드만 `503 STORAGE_UNAVAILABLE`이 됩니다.

## 5. 접속 정보 파일

```bash
cp backend/.env.example backend/.env
```

`backend/.env`를 열어 채웁니다. (`.env`는 Git에 올라가지 않음)

```
DB_PASSWORD=3번에서 정한 flow 계정 비밀번호
AWS_S3_BUCKET=min420-flow-uploads
```

## 6. 실행

백엔드 (**반드시 `backend` 폴더에서** 실행해야 `.env`를 읽음):

```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```

- PowerShell에서는 `.\mvnw.cmd clean test`, `.\mvnw.cmd spring-boot:run`
- 테스트는 로컬 MySQL에 3번의 스키마가 있어야 통과합니다. (56개)

프론트 (새 터미널):

```bash
cd frontend
npm ci
npm run serve
```

- 화면: http://localhost:3000 (API는 `localhost:8080`으로 프록시)

## 7. 동작 확인

- [ ] http://localhost:3000 에 고정 확장자 7개가 보임
- [ ] `exe` 체크 → 새로고침해도 유지
- [ ] 커스텀에 `sh` 추가 → `1/200`, `X`로 삭제
- [ ] 파일 업로드 탭에서 텍스트 파일 업로드 → 업로드 완료 (4번을 했다면)
- [ ] `exe`를 체크한 뒤 `setup.exe` 업로드 → 차단 안내

## 8. (선택) 이 컴퓨터에서 AWS에 다시 배포하기

1. SSH 키 `flow-key.pem`을 기존 컴퓨터에서 **Git이 아닌 방법**(USB 등)으로 옮겨 `~/.ssh/flow-key.pem`에 두기
2. EC2 보안 그룹은 SSH(22)를 등록된 IP만 허용하므로 이 컴퓨터의 IP 추가 (`flow-deploy` 프로필 필요)

```bash
aws ec2 authorize-security-group-ingress --profile flow-deploy --region ap-northeast-2 \
  --group-id sg-09bdd750e2fee638b --protocol tcp --port 22 --cidr "$(curl -s https://checkip.amazonaws.com)/32"
```

3. 빌드와 배포

```bash
bash deploy/build.sh
bash deploy/deploy.sh 43.202.189.27
```

## 9. 문제 해결

| 증상 | 원인 / 해결 |
|------|-------------|
| `release version 17 not supported` 등 컴파일 오류 | `JAVA_HOME`이 17이 아님 → 1번 확인 |
| 테스트가 `ExtensionPolicyMapper.xml` 파싱 오류로 실패 | 이전 이름의 XML이 `target/`에 남음 → `./mvnw clean test` |
| 테스트가 테이블이 없다며 실패 | 3번 스키마·seed 미적용 |
| `Could not resolve placeholder 'DB_URL'` | `backend` 폴더가 아닌 곳에서 실행했거나 `.env`가 없음 |
| `Access denied for user 'flow'@'localhost'` | `.env`의 `DB_PASSWORD`가 3번에서 정한 값과 다름 |
| 업로드가 `503 STORAGE_UNAVAILABLE` | `AWS_S3_BUCKET` 누락 또는 `aws configure` 안 함 |
| `aws`, `java` 명령을 못 찾음 | 설치 후 터미널을 다시 시작하지 않음 |
| Git Bash에서 curl로 올린 한글 파일명이 깨짐 | Windows curl이 파일명을 CP949로 보냄. 앱 문제 아님 → 브라우저로 확인 |
| Git Bash에서 `aws` 명령의 경로 인자가 `D:/Git/...`로 바뀜 | 명령 앞에 `MSYS_NO_PATHCONV=1` |

## 10. Claude Code로 이어서 작업한다면

- 기존 컴퓨터의 `C:\Users\<사용자>\.claude\CLAUDE.md`를 새 컴퓨터의 같은 위치에 복사 (한국어 응답, 커밋 메시지 형식 등 작업 규칙)
- 새 세션에서: `HANDOFF.md와 PROMPT_LOG.md 읽고 이어서 작업하자`
