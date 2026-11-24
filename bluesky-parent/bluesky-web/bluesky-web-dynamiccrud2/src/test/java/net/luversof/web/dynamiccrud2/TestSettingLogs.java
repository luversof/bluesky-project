package net.luversof.web.dynamiccrud2;

import java.time.Instant;
import java.util.UUID;

import net.luversof.web.dynamiccrud2.repository.SettingLogRepository;

/**
 * 시험이 만든 프로젝트의 설정 변경 이력 지우기. 전체 설정 변경 이력 화면(2026-11-24)이 생기면서 지운 시험 프로젝트의 이력이 그대로 보이게 됐다 - 프로젝트를 지운
 * 직후 부른다(프로젝트 · 메인 메뉴 · 메뉴 · 쿼리 · 검색 변경은 모두 그 프로젝트 범위로 남는다).
 */
public final class TestSettingLogs {

  private TestSettingLogs() {}

  public static void purge(SettingLogRepository repository, UUID projectId) {
    repository.deleteAll(
        repository.search(null, null, projectId, null, null, null, null, 10_000, 0));
  }

  /** 시험이 공유 테이블 필드를 바꿨다 되돌린 이력(필드는 테이블 단위라 프로젝트 범위에 없다). since 뒤의 FIELD 이력만 */
  public static void purgeFields(
      SettingLogRepository repository, UUID menuId, UUID tableId, Instant since) {
    repository.deleteAll(
        repository.search(menuId, tableId, null, null, "FIELD", since, null, 10_000, 0));
  }
}
