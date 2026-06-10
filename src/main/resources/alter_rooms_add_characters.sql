-- =====================================================
-- rooms 테이블 characters JSON 컬럼 추가
-- character_name(기존, 단일) → characters JSON(신규, 최대 2명)
-- =====================================================

ALTER TABLE `rooms`
    ADD COLUMN `characters` JSON NULL
        COMMENT '등장인물 목록 (최대 2명). [{\"name\":\"Sarah\",\"personality\":\"친절하고 빠른 서비스\"}]'
    AFTER `character_name`;

-- =====================================================
-- 사용 예시
-- =====================================================
-- 단독 캐릭터 (1명)
-- INSERT rooms ... characters = '[{"name":"Sarah","personality":"친절하고 빠른 서비스"}]'

-- 멀티 캐릭터 (2명)
-- INSERT rooms ... characters = '[{"name":"Sarah","personality":"친절하고 빠른 서비스"},
--                                  {"name":"손님","personality":"정중하고 신중함"}]'
