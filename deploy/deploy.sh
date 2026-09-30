#!/bin/bash
# 빌드한 jar를 EC2에 올리고 서비스를 재시작한다.
# 사용: deploy/deploy.sh <EC2 IP>   (SSH 키: ~/.ssh/flow-key.pem)
set -e
cd "$(dirname "$0")/.."

HOST="ec2-user@${1:?EC2 IP를 인자로 주세요}"
SSH="ssh -i $HOME/.ssh/flow-key.pem -o StrictHostKeyChecking=accept-new"
JAR=$(ls backend/target/backend-*.jar | head -1)

scp -i "$HOME/.ssh/flow-key.pem" "$JAR" deploy/flow.service "$HOST:/tmp/"
$SSH "$HOST" 'sudo install -o flow -g flow -m 644 /tmp/backend-*.jar /opt/flow/app.jar \
  && sudo install -m 644 /tmp/flow.service /etc/systemd/system/flow.service \
  && rm /tmp/backend-*.jar /tmp/flow.service \
  && sudo systemctl daemon-reload && sudo systemctl enable flow && sudo systemctl restart flow \
  && sleep 2 && systemctl is-active flow'
