-- 확장자 차단 정책 테이블 정의 (MySQL 8.4)
--
-- 고정 확장자와 커스텀 확장자를 테이블 하나에 저장한다.
-- - EXTENSION에 UNIQUE가 있어서 커스텀 확장자가 고정 확장자와 겹치는 것도 DB에서 막힌다.
-- - 업로드 검사는 IS_BLOCKED = 1인 행만 조회하면 된다.
--
-- EXTENSION은 앱에서 정규화한 값만 저장한다.
-- (앞뒤 공백 제거 → 앞의 '.' 제거 → 소문자 변환 → 영문 소문자/숫자 1~20자만 허용)

CREATE TABLE FILE_EXTENSION_POLICY (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    EXTENSION VARCHAR(20) NOT NULL,
    TYPE VARCHAR(10) NOT NULL,                 -- FIXED: 고정 확장자, CUSTOM: 커스텀 확장자
    IS_BLOCKED TINYINT(1) NOT NULL DEFAULT 1,  -- 1: 차단, 0: 허용 (고정 확장자의 체크 상태)
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT UQ_FILE_EXTENSION UNIQUE (EXTENSION),
    CONSTRAINT CK_FILE_EXTENSION_TYPE
        CHECK (TYPE IN ('FIXED', 'CUSTOM')),
    CONSTRAINT CK_FILE_EXTENSION_BLOCKED
        CHECK (IS_BLOCKED IN (0, 1)),
    -- 정규화 규칙을 DB에서도 강제 (앱 검증을 거치지 않은 값 방지)
    CONSTRAINT CK_FILE_EXTENSION_FORMAT
        CHECK (REGEXP_LIKE(EXTENSION, '^[a-z0-9]{1,20}$', 'c')),
    -- 커스텀 확장자는 추가 = 차단이므로 항상 1
    CONSTRAINT CK_FILE_EXTENSION_CUSTOM_BLOCKED
        CHECK (TYPE = 'FIXED' OR IS_BLOCKED = 1)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
