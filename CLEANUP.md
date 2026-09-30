# CLEANUP - 과제 종료 후 AWS 과금 정리

과제가 끝나면 아래 순서대로 지워야 더 이상 과금되지 않습니다. (서울 리전 `ap-northeast-2`)

- 현재 예상 비용: 월 약 $32.8 (EC2 $7.6, RDS $18.3, RDS 스토리지 $2.6, EBS $0.7, 퍼블릭 IPv4 $3.7)
- 만든 리소스에는 모두 태그 `Project=flow`가 붙어 있습니다.
- 명령은 `flow-deploy` 프로필 기준입니다. 콘솔에서 지워도 됩니다.

```bash
export AWS_PROFILE=flow-deploy AWS_REGION=ap-northeast-2
```

> **중지로는 과금이 멈추지 않습니다.** EC2를 중지해도 디스크(EBS)와 Elastic IP 요금이 계속 나가고, RDS는 중지해도 **7일 후 자동으로 다시 시작**됩니다. 완전히 끝났다면 삭제하세요.

---

## 0. (필요하면) 남길 데이터 백업

삭제하면 되돌릴 수 없습니다. 정책 데이터를 남기려면 먼저 EC2에서 덤프를 받아 두세요.

```bash
ssh -i ~/.ssh/flow-key.pem ec2-user@43.202.189.27 \
  'mysqldump -h flow-db.c7agi0seiho8.ap-northeast-2.rds.amazonaws.com -u admin -p flow' > flow-backup.sql
```

## 1. EC2 종료 (디스크도 함께 삭제됨)

```bash
aws ec2 terminate-instances --instance-ids i-020284f62d2094fbf
aws ec2 wait instance-terminated --instance-ids i-020284f62d2094fbf
```

- 루트 디스크는 `DeleteOnTermination=true`로 만들어서 인스턴스와 함께 삭제됩니다.

## 2. Elastic IP 해제 ⚠️ 가장 놓치기 쉬움

인스턴스를 지워도 **Elastic IP는 남아서 계속 과금**됩니다.

```bash
aws ec2 release-address --allocation-id eipalloc-0b28c9996557d1ad1
```

## 3. RDS 삭제

```bash
aws rds delete-db-instance --db-instance-identifier flow-db \
  --skip-final-snapshot --delete-automated-backups
aws rds wait db-instance-deleted --db-instance-identifier flow-db
```

- 최종 스냅샷을 남기려면 `--skip-final-snapshot` 대신 `--final-db-snapshot-identifier flow-db-final`. **스냅샷도 저장 용량만큼 과금**되므로 필요 없어지면 따로 삭제하세요.
- 5~10분 걸립니다.
- 백업 보존 기간(1일) 동안 생긴 **자동 스냅샷(`rds:flow-db-...`)과 자동 백업은 `--delete-automated-backups`로 함께 삭제**됩니다. 이 옵션을 빼면 보존 기간 동안 남아 과금될 수 있습니다.

## 4. 보안 그룹 삭제

EC2와 RDS가 **완전히 삭제된 뒤**에 지울 수 있습니다. `flow-rds-sg`가 `flow-ec2-sg`를 참조하므로 **RDS 쪽부터** 지웁니다.

```bash
aws ec2 delete-security-group --group-id sg-052d854029158e6ee   # flow-rds-sg
aws ec2 delete-security-group --group-id sg-09bdd750e2fee638b   # flow-ec2-sg
```

- `DependencyViolation`이 나오면 아직 삭제가 진행 중인 것이니 잠시 후 다시 실행하세요.

## 5. 키 페어 삭제

```bash
aws ec2 delete-key-pair --key-name flow-key
rm ~/.ssh/flow-key.pem
```

## 6. EC2용 IAM 역할 삭제

```bash
aws iam remove-role-from-instance-profile --instance-profile-name flow-ec2-role --role-name flow-ec2-role
aws iam delete-instance-profile --instance-profile-name flow-ec2-role
aws iam delete-role-policy --role-name flow-ec2-role --policy-name flow-s3-upload
aws iam delete-role --role-name flow-ec2-role
```

## 7. S3 버킷 비우고 삭제 (콘솔)

`flow-deploy`와 `flow-app`에는 S3 삭제 권한이 없으므로 콘솔에서 합니다.

S3 콘솔 → `min420-flow-uploads` → **비우기** → **삭제**

## 8. IAM 사용자 삭제 (콘솔, 마지막에)

`flow-deploy`는 위 단계에서 쓰므로 **맨 마지막**에 지웁니다.

IAM 콘솔 → 사용자 → `flow-app`, `flow-deploy` 각각 **삭제** (액세스 키와 인라인 정책도 함께 삭제됨)

## 9. 로컬 정리

```bash
rm ~/.flow-deploy.env ~/.flow-deploy.ids
```

- `~/.aws/credentials`, `~/.aws/config`에서 `default`(flow-app), `flow-deploy` 항목 삭제
- 다른 컴퓨터에도 `aws configure`를 했다면 그 컴퓨터의 `~/.aws`도 정리 (키는 8번에서 이미 무효화됨)
- 로컬 MySQL은 과금과 무관

---

## 10. 남은 것이 없는지 확인

8번 전에 `flow-deploy`로 확인 (모두 빈 결과여야 함):

```bash
aws ec2 describe-instances --filters Name=tag:Project,Values=flow Name=instance-state-name,Values=pending,running,stopping,stopped --query 'Reservations[].Instances[].InstanceId'
aws ec2 describe-addresses --query 'Addresses[].PublicIp'
aws ec2 describe-volumes --filters Name=tag:Project,Values=flow --query 'Volumes[].VolumeId'
aws ec2 describe-security-groups --filters Name=group-name,Values=flow-* --query 'SecurityGroups[].GroupId'
aws rds describe-db-instances --query 'DBInstances[].DBInstanceIdentifier'
aws rds describe-db-snapshots --query 'DBSnapshots[].DBSnapshotIdentifier'
aws rds describe-db-instance-automated-backups --query 'DBInstanceAutomatedBackups[].DBInstanceIdentifier'
```

콘솔에서도 확인:

- **Resource Groups & Tag Editor** → 리전 서울, 태그 `Project=flow` 검색 → 결과 없음
- **결제(Billing)** → 다음 날 청구서에서 EC2, RDS, VPC(퍼블릭 IPv4) 항목이 더 늘지 않는지 확인
- 기본 VPC는 처음부터 있던 것이고 과금되지 않으니 지우지 않습니다.
