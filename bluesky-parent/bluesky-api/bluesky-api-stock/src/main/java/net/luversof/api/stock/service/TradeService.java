package net.luversof.api.stock.service;

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

  @Autowired private net.luversof.api.stock.repository.TradeQuery tradeQuery;

  public List<Trade> findByAccountId(UUID accountId) {
    return tradeRepository.findByAccountId(accountId);
  }

  /** 사용자의 모든 거래(계좌 조인). */
  public List<Trade> findByUserId(UUID userId) {
    return tradeQuery.findByUserId(userId);
  }

  public List<Trade> findByAccountIdIn(List<UUID> accountIdList) {
    return tradeQuery.findByAccountIdIn(accountIdList);
  }

  public List<Trade> findByAccountIdInAndStockItemIdIn(
      List<UUID> accountIdList, List<UUID> stockItemIdList) {
    return tradeRepository.findByAccountIdInAndStockItemIdIn(accountIdList, stockItemIdList);
  }
}
