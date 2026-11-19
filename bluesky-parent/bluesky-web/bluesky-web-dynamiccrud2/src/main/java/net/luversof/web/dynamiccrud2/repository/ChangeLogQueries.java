package net.luversof.web.dynamiccrud2.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import net.luversof.web.dynamiccrud2.domain.ChangeLog;

/** 데이터 변경 이력 화면 조회(거르기는 null 이면 안 씀, 기간은 [from, to)). 구현은 {@link ChangeLogQueriesImpl} */
public interface ChangeLogQueries {

  List<ChangeLog> search(
      UUID menuId,
      String username,
      UUID queryId,
      Instant fromDate,
      Instant toDate,
      int limit,
      long offset);

  long countSearch(UUID menuId, String username, UUID queryId, Instant fromDate, Instant toDate);

  /** 사용자 거르기 후보 */
  List<String> findUsernames(UUID menuId);
}
