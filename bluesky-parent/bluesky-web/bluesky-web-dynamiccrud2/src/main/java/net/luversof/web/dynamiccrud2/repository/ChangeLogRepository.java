package net.luversof.web.dynamiccrud2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.ChangeLog;

/** 데이터 변경 이력(사용 화면 실행). 화면 조회(거르기 · 쪽)는 {@link ChangeLogQueries} */
public interface ChangeLogRepository extends ListCrudRepository<ChangeLog, UUID>, ChangeLogQueries {

  List<ChangeLog> findByMenuIdOrderByCreatedDateDesc(UUID menuId);
}
