package net.luversof.api.stock.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import net.luversof.api.stock.domain.StockItemTradeDate;
import net.luversof.api.stock.domain.StockPriceHistory;

public interface StockPriceHistoryRepository extends CrudRepository<StockPriceHistory, UUID> {

  // 장중에 저장되어 값이 아직 확정되지 않았을 수 있는 레코드(거래일 == 갱신일)만 재조회 대상으로 본다.
  // 거래량은 장중에도 0이 아닐 수 있어 "거래량 == 0"은 장중/미확정 여부의 올바른 신호가 아니므로 제외한다.
  // (재조회 후 API 값이 기존과 다르면 갱신, 같으면 갱신하지 않는다.)
  @Query(
      """
                    SELECT "stockItem_id" AS stock_item_id,
                                 "tradeDate" AS trade_date
                    FROM "StockPriceHistory"
                    WHERE "stockItem_id" IS NOT NULL
                        AND "tradeDate" IS NOT NULL
                        AND "updatedDate" IS NOT NULL
                        AND "tradeDate" = ("updatedDate" AT TIME ZONE 'Asia/Seoul')::date
                    ORDER BY "stockItem_id", "tradeDate"
                """)
  List<StockItemTradeDate> findRefreshTargetTradeDates();

  Optional<StockPriceHistory> findTopByStockItemIdAndTradeDateLessThanEqualOrderByTradeDateDesc(
      UUID stockItemId, LocalDate tradeDate);

  List<StockPriceHistory> findByStockItemIdAndTradeDateBetween(
      UUID stockItemId, LocalDate start, LocalDate end);

  List<StockPriceHistory> findByStockItemIdInAndTradeDateBetween(
      Collection<UUID> stockItemId, LocalDate start, LocalDate end);

  Optional<StockPriceHistory> findByStockItemIdAndTradeDate(UUID stockItemId, LocalDate tradeDate);

  Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateDesc(UUID stockItemId);

  Optional<StockPriceHistory> findTopByStockItemIdOrderByTradeDateAsc(UUID stockItemId);
}
