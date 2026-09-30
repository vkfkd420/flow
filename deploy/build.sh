#!/bin/bash
# 프론트를 빌드해서 백엔드 jar에 포함한다. 결과: backend/target/backend-0.0.1-SNAPSHOT.jar
# 테스트는 로컬 MySQL(backend/.env)이 필요하다.
set -e
cd "$(dirname "$0")/.."

(cd frontend && npm ci && npm run build)
(cd backend && ./mvnw -B clean package)

ls -l backend/target/*.jar
