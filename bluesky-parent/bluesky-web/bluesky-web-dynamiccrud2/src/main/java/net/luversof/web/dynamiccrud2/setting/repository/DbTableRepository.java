package net.luversof.web.dynamiccrud2.setting.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.setting.domain.DbTable;

public interface DbTableRepository extends ListCrudRepository<DbTable, UUID> {

  /** catalog · schema 는 NULL 일 수 있어 uk_dbTable_location 과 같은 COALESCE 로 비교한다 */
  @Query(
      """
      SELECT "id" FROM "DbTable"
      WHERE "connectionName" = :connectionName
        AND COALESCE("catalogName", '') = COALESCE(:catalogName, '')
        AND COALESCE("schemaName", '') = COALESCE(:schemaName, '')
        AND "tableName" = :tableName
      """)
  Optional<UUID> findIdByLocation(
      String connectionName, String catalogName, String schemaName, String tableName);
}
