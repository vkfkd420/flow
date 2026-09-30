-- 초기 데이터: 고정 확장자 7개 (기본값은 모두 체크 해제 = 허용)

INSERT INTO FILE_EXTENSION_POLICY (EXTENSION, TYPE, IS_BLOCKED) VALUES
    ('bat', 'FIXED', 0),
    ('cmd', 'FIXED', 0),
    ('com', 'FIXED', 0),
    ('cpl', 'FIXED', 0),
    ('exe', 'FIXED', 0),
    ('scr', 'FIXED', 0),
    ('js',  'FIXED', 0);
