-- 이미 만든 가계부 DB 를 최종 스키마(schema-create-postgresql-bookkeeping.sql)로 맞추는 변경 쿼리. 새 DB 는 create 파일만 실행하면 된다.
-- 위에서부터 순서대로, 아직 안 한 날짜 묶음만 실행한다. 모든 문장은 두 번 실행해도 안전하다.

-- =====================================================================
-- 2026-11-03 가계부 전면 개편(DESIGN.md)
-- =====================================================================

-- (1) 옛 스키마 삭제. 기존 데이터는 보존하지 않기로 했다(설계 합의).
--     옛 Asset · Entry 는 새 테이블과 이름이 같다. 그래서 옛 스키마 표식인 "Bookkeeping" 테이블이 있을 때만 지운다.
--     다시 실행해도 새 Asset · Entry(새 데이터)는 지우지 않는다.
DO $$
BEGIN
	IF to_regclass('"Bookkeeping"') IS NOT NULL THEN
		DROP TABLE IF EXISTS "Entry", "EntryType", "Asset", "AssetType", "Bookkeeping";
	END IF;
END
$$;

-- (2) 이어서 schema-create-postgresql-bookkeeping.sql 전체를 실행한다(모든 문장이 IF NOT EXISTS).

-- 확인(6 줄이 나오고, 옛 테이블 Bookkeeping · AssetType · EntryType 는 안 나오면 끝)
-- SELECT table_name FROM information_schema.tables
--  WHERE table_schema = current_schema() AND table_name IN ('Book', 'BookMember', 'BookInvite', 'Asset', 'Category', 'Entry', 'Bookkeeping', 'AssetType', 'EntryType')
--  ORDER BY table_name;
-- 새 Asset 인지(옛 Asset 에는 assetType_id 가 있다): 'kind' 한 줄이 나와야 한다
-- SELECT column_name FROM information_schema.columns WHERE table_name = 'Asset' AND column_name IN ('kind', 'assetType_id');

-- 되돌리기: 없음. 옛 데이터는 (1) 에서 사라진다. 필요하면 실행 전에 pg_dump 로 받아 둔다:
--   pg_dump -t '"Bookkeeping"' -t '"AssetType"' -t '"EntryType"' -t '"Asset"' -t '"Entry"' <db> > bookkeeping-old.sql
-- 새 스키마만 걷어 낼 때:
-- DROP TABLE IF EXISTS "Entry", "Category", "Asset", "BookInvite", "BookMember", "Book";
