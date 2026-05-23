package net.luversof.api.stock.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.MonthlyDividendProfile;

public interface MonthlyDividendProfileRepository
    extends CrudRepository<MonthlyDividendProfile, UUID> {

  Optional<MonthlyDividendProfile> findByStockItemId(UUID stockItemId);

  Optional<MonthlyDividendProfile> findFirstByOrderByDisplayOrderDescUpdatedDateDesc();

  List<MonthlyDividendProfile> findAllByOrderByDisplayOrderAscUpdatedDateDesc();

  List<MonthlyDividendProfile> findByActiveOrderByDisplayOrderAscUpdatedDateDesc(boolean active);
}
