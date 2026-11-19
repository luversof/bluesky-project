package net.luversof.web.dynamiccrud2.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import net.luversof.web.dynamiccrud2.domain.SettingLog;

/**
 * 설정 변경 이력 화면 조회. 범위는 둘 중 하나: 메뉴(menuId + 그 메뉴 테이블의 필드 변경 tableId) 또는 프로젝트(projectId). 거르기는 null 이면
 * 안 씀, 기간은 [from, to). 구현은 {@link SettingLogQueriesImpl}
 */
public interface SettingLogQueries {

  List<SettingLog> search(
      UUID menuId,
      UUID tableId,
      UUID projectId,
      String username,
      String target,
      Instant fromDate,
      Instant toDate,
      int limit,
      long offset);

  long countSearch(
      UUID menuId,
      UUID tableId,
      UUID projectId,
      String username,
      String target,
      Instant fromDate,
      Instant toDate);

  List<String> findUsernames(UUID menuId, UUID tableId, UUID projectId);
}
