package net.luversof.api.stock.service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.DailyAccountSnapshotRepository;
import net.luversof.api.stock.repository.TradeRepository;

@Service
@Transactional(transactionManager = "stockTransactionManager")
public class TradeService {

  @Autowired private TradeRepository tradeRepository;

  @Autowired private AccountService accountService;

  @Autowired private DailyAccountSnapshotRepository snapshotRepository;

  public void setTradeRepository(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  public Trade createTrade(Trade trade) {
    Trade savedTrade = tradeRepository.save(trade);

    // 스냅샷 무효화 처리
    accountService
        .findById(trade.getAccountId())
        .ifPresent(
            account -> {
              snapshotRepository.deleteByUserIdAndDateGreaterThanEqual(
                  account.getUserId(),
                  trade.getTradeDate().atZone(ZoneId.systemDefault()).toLocalDate());
            });

    return savedTrade;
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
