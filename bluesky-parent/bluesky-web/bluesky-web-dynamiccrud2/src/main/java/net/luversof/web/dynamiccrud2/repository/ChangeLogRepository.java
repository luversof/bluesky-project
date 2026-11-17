package net.luversof.web.dynamiccrud2.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.ListCrudRepository;

import net.luversof.web.dynamiccrud2.domain.ChangeLog;

public interface ChangeLogRepository extends ListCrudRepository<ChangeLog, UUID> {

  List<ChangeLog> findByMenuIdOrderByCreatedDateDesc(UUID menuId);
}
