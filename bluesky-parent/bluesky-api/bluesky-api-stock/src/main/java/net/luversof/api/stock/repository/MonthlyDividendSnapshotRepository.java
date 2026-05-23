package net.luversof.api.stock.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.MonthlyDividendSnapshot;

public interface MonthlyDividendSnapshotRepository
    extends CrudRepository<MonthlyDividendSnapshot, UUID> {

  Optional<MonthlyDividendSnapshot> findByUserIdAndStockItemId(UUID userId, UUID stockItemId);

  List<MonthlyDividendSnapshot> findByStockItemIdOrderByUpdatedDateDesc(UUID stockItemId);

  List<MonthlyDividendSnapshot> findByUserIdOrderByUpdatedDateDesc(UUID userId);
}
