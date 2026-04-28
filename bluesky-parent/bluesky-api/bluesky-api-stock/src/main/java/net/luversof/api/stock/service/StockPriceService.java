package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.StockPriceHistory;
import net.luversof.api.stock.repository.StockPriceHistoryRepository;

@Service
public class StockPriceService {

  @Autowired private StockPriceHistoryRepository stockPriceHistoryRepository;

  public void setStockPriceHistoryRepository(
      StockPriceHistoryRepository stockPriceHistoryRepository) {
    this.stockPriceHistoryRepository = stockPriceHistoryRepository;
  }

  public BigDecimal getCurrentPrice(UUID stockItemId) {
    return stockPriceHistoryRepository
        .findTopByStockItemIdOrderByTradeDateDesc(stockItemId)
        .map(StockPriceHistory::getClosePrice)
        .orElse(BigDecimal.ZERO);
  }

  public BigDecimal getPriceAt(UUID stockItemId, LocalDate at) {
    if (at == null) return getCurrentPrice(stockItemId);
    return stockPriceHistoryRepository
        .findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(stockItemId, at)
        .map(StockPriceHistory::getClosePrice)
        .orElseGet(() -> getCurrentPrice(stockItemId));
  }

  public List<StockPriceHistory> getPriceHistory(
      Collection<UUID> stockItemIdList, LocalDate start, LocalDate end) {
    return stockPriceHistoryRepository.findByStockItemIdInAndTradeDateBetween(
        stockItemIdList, start, end);
  }
}
