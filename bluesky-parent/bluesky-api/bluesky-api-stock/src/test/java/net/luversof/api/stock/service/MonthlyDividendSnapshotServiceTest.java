package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.MonthlyDividendSnapshot;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendSnapshotResponse;

@ExtendWith(MockitoExtension.class)
class MonthlyDividendSnapshotServiceTest {

  @Mock private MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository;

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockPriceService stockPriceService;

  @InjectMocks private MonthlyDividendSnapshotService monthlyDividendSnapshotService;

  @Test
  void upsertNewSnapshotDoesNotPreassignIdBeforeSave() {
    UUID userId = UUID.randomUUID();
    StockItem stockItem = createStockItem("O");
    UUID generatedId = UUID.randomUUID();

    MonthlyDividendSnapshotUpsertRequest request = new MonthlyDividendSnapshotUpsertRequest();
    request.setUserId(userId);
    request.setSymbol("O");
    request.setAsOfDate(LocalDate.of(2026, 3, 31));
    request.setLatestMonthlyDividendPerShare(new BigDecimal("1.20"));
    request.setAverageMonthlyDividendPerShare1y(new BigDecimal("1.00"));
    request.setAverageTaxableBaseRatio1y(new BigDecimal("15.00"));
    request.setHeldQuantity(10);
    request.setAverageBuyPrice(new BigDecimal("42.50"));

    when(stockItemRepository.findBySymbol("O")).thenReturn(stockItem);
    when(monthlyDividendSnapshotRepository.findByUserIdAndStockItemId(userId, stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendSnapshotRepository.save(any(MonthlyDividendSnapshot.class)))
        .thenAnswer(
            invocation -> {
              MonthlyDividendSnapshot snapshot = invocation.getArgument(0);
              assertThat(snapshot.getId()).isNull();
              assertThat(snapshot.getCreatedDate()).isNotNull();
              snapshot.setId(generatedId);
              return snapshot;
            });
    when(stockPriceService.getCurrentPrice(stockItem.getId())).thenReturn(new BigDecimal("50.00"));

    MonthlyDividendSnapshotResponse response = monthlyDividendSnapshotService.upsert(request);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.stockItemSymbol()).isEqualTo("O");
    assertThat(response.currentPrice()).isEqualByComparingTo("50.00");
  }

  private StockItem createStockItem(String symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setId(UUID.randomUUID());
    stockItem.setSymbol(symbol);
    stockItem.setName(symbol + " Inc.");
    stockItem.setMarket("NYSE");
    return stockItem;
  }
}
