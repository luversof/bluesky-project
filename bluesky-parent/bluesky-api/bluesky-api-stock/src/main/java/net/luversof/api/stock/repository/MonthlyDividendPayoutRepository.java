package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.MonthlyDividendPayout;

public interface MonthlyDividendPayoutRepository
    extends CrudRepository<MonthlyDividendPayout, UUID> {

  Optional<MonthlyDividendPayout> findByStockItemIdAndRecordDateAndPayDate(
      UUID stockItemId, LocalDate recordDate, LocalDate payDate);

  List<MonthlyDividendPayout> findAllByOrderByPayDateDescRecordDateDesc();

  List<MonthlyDividendPayout> findByStockItemIdOrderByPayDateDescRecordDateDesc(UUID stockItemId);
}
