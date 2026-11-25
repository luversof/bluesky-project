package net.luversof.web.dynamiccrud2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.ChangeLog;

/** 데이터 변경 이력(사용 화면 실행). 화면 조회(거르기 · 쪽)는 {@link ChangeLogQueries} */
public interface ChangeLogRepository extends ListCrudRepository<ChangeLog, UUID>, ChangeLogQueries {

  List<ChangeLog> findByMenuIdOrderByCreatedDateDesc(UUID menuId);

  /** 이 메뉴에서 이미 되돌린 이력 id(되돌리기 이력의 bindValues.revertOf) - 같은 변경을 두 번 되돌리지 않게 */
  @Query(
      "SELECT \"bindValues\" ->> 'revertOf' FROM \"ChangeLog\""
          + " WHERE \"menu_id\" = :menuId AND \"bindValues\" ->> 'revertOf' IS NOT NULL")
  List<String> findRevertedIds(UUID menuId);
}
