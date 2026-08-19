package net.luversof.api.stock.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.TradeRepository;

@Service
@Transactional(transactionManager = "stockTransactionManager")
public class TradeService {

  @Autowired private TradeRepository tradeRepository;

  @Autowired private AccountService accountService;

  public void setTradeRepository(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  public Trade createTrade(Trade trade) {
    // 보유 스냅샷 캐시가 사라져(시계열과 동일 시뮬레이션으로 계산) 무효화 처리도 필요 없다.
    return tradeRepository.save(trade);
  }

  public List<Trade> findByAccountId(UUID accountId) {
    return tradeRepository.findByAccountId(accountId);
  }

  public List<Trade> findByAccountIdIn(List<UUID> accountIdList) {
    return tradeRepository.findByAccountIdIn(accountIdList);
  }

  public List<Trade> findByAccountIdInAndTradeDateBetween(
      List<UUID> accountIdList, Instant startDate, Instant endDate) {
    return tradeRepository.findByAccountIdInAndTradeDateBetween(accountIdList, startDate, endDate);
  }

  public List<Trade> findByAccountIdInAndStockItemIdIn(
      List<UUID> accountIdList, List<UUID> stockItemIdList) {
    return tradeRepository.findByAccountIdInAndStockItemIdIn(accountIdList, stockItemIdList);
  }

  public List<Trade> findByAccountIdInAndStockItemIdInAndTradeDateBetween(
      List<UUID> accountIdList, List<UUID> stockItemIdList, Instant startDate, Instant endDate) {
    return tradeRepository.findByAccountIdInAndStockItemIdInAndTradeDateBetween(
        accountIdList, stockItemIdList, startDate, endDate);
  }
}
