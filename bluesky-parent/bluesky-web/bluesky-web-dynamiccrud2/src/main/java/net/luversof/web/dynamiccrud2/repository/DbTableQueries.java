package net.luversof.web.dynamiccrud2.repository;

import java.util.Optional;
import java.util.UUID;

/** 구현은 {@link DbTableQueriesImpl} */
public interface DbTableQueries {

  /** catalog · schema 는 NULL 일 수 있어 uk_dbTable_location 과 같은 COALESCE 로 비교한다 */
  Optional<UUID> findIdByLocation(
      String connectionName, String catalogName, String schemaName, String tableName);
}
