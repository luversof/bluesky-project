package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockMarketDaily;

public interface StockMarketDailyRepository extends CrudRepository<StockMarketDaily, UUID> {

  /** uk_stockMarketDaily_code_tradeDate 를 탄다. */
  List<StockMarketDaily> findByCodeAndTradeDateBetweenOrderByTradeDate(
      String code, LocalDate from, LocalDate to);
}
