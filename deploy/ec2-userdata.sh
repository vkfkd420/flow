#!/bin/bash
# EC2(Amazon Linux 2023) 최초 부팅 시 1회 실행되는 초기화 스크립트 (run-instances --user-data)
set -e
dnf install -y java-17-amazon-corretto-headless mariadb105

# t4g.micro 메모리(1GB) 보완용 스왑 1GB
dd if=/dev/zero of=/swapfile bs=1M count=1024
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile
echo '/swapfile swap swap defaults 0 0' >> /etc/fstab

# 앱 실행 전용 계정 (로그인 불가), 앱 파일과 환경 변수 파일 위치
useradd --system --no-create-home --shell /sbin/nologin flow
mkdir -p /opt/flow /etc/flow
chown flow:flow /opt/flow
chmod 700 /etc/flow
