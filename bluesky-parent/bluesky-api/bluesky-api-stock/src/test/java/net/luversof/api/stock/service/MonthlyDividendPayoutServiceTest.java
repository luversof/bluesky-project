package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.domain.MonthlyDividendSnapshot;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendPayoutResponse;

@ExtendWith(MockitoExtension.class)
class MonthlyDividendPayoutServiceTest {

  @Mock private MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;

  @Mock private MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository;

  @Mock private StockItemRepository stockItemRepository;

  @InjectMocks private MonthlyDividendPayoutService monthlyDividendPayoutService;

  @Test
  void upsertNewPayoutDoesNotPreassignIdBeforeSave() {
    StockItem stockItem = createStockItem("O");
    UUID generatedId = UUID.randomUUID();

    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol("O");
    request.setRecordDate(LocalDate.of(2026, 3, 20));
    request.setPayDate(LocalDate.of(2026, 3, 31));
    request.setDistributionRatePct(new BigDecimal("5.00"));
    request.setDividendAmountPerShare(new BigDecimal("1.20"));
    request.setTaxableBasePerShare(new BigDecimal("0.18"));

    when(stockItemRepository.findBySymbol("O")).thenReturn(stockItem);
    when(monthlyDividendPayoutRepository.findByStockItemIdAndRecordDateAndPayDate(
            stockItem.getId(), request.getRecordDate(), request.getPayDate()))
        .thenReturn(Optional.empty());
    when(monthlyDividendPayoutRepository.save(any(MonthlyDividendPayout.class)))
        .thenAnswer(
            invocation -> {
              MonthlyDividendPayout payout = invocation.getArgument(0);
              assertThat(payout.getId()).isNull();
              assertThat(payout.getCreatedDate()).isNotNull();
              payout.setId(generatedId);
              return payout;
            });
    when(monthlyDividendSnapshotRepository.findByStockItemIdOrderByUpdatedDateDesc(
            stockItem.getId()))
        .thenReturn(List.of());

    MonthlyDividendPayoutResponse response = monthlyDividendPayoutService.upsert(request);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.stockItemSymbol()).isEqualTo("O");
  }

  @Test
  void upsertRefreshesAllSnapshotsForSameStock() {
    StockItem stockItem = createStockItem("O");
    MonthlyDividendSnapshot snapshotOne = createSnapshot(stockItem.getId(), 10, "42.50");
    MonthlyDividendSnapshot snapshotTwo = createSnapshot(stockItem.getId(), 20, "43.10");
    Instant originalUpdatedDate = snapshotOne.getUpdatedDate();

    MonthlyDividendPayout latestPayout =
        createPayout(stockItem.getId(), "2026-03-20", "2026-03-31", "1.20", "0.18");
    MonthlyDividendPayout olderPayout =
        createPayout(stockItem.getId(), "2026-02-20", "2026-02-28", "0.80", "0.08");
    MonthlyDividendPayout oldestPayout =
        createPayout(stockItem.getId(), "2026-01-20", "2026-01-31", "1.00", "0.20");

    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol("O");
    request.setRecordDate(LocalDate.of(2026, 3, 20));
    request.setPayDate(LocalDate.of(2026, 3, 31));
    request.setDistributionRatePct(new BigDecimal("5.00"));
    request.setDividendAmountPerShare(new BigDecimal("1.20"));
    request.setTaxableBasePerShare(new BigDecimal("0.18"));

    when(stockItemRepository.findBySymbol("O")).thenReturn(stockItem);
    when(monthlyDividendPayoutRepository.findByStockItemIdAndRecordDateAndPayDate(
            stockItem.getId(), request.getRecordDate(), request.getPayDate()))
        .thenReturn(Optional.empty());
    when(monthlyDividendPayoutRepository.save(any(MonthlyDividendPayout.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(monthlyDividendSnapshotRepository.findByStockItemIdOrderByUpdatedDateDesc(
            stockItem.getId()))
        .thenReturn(List.of(snapshotOne, snapshotTwo));
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItem.getId()))
        .thenReturn(List.of(latestPayout, olderPayout, oldestPayout));

    monthlyDividendPayoutService.upsert(request);

    assertSnapshotRefreshed(snapshotOne, originalUpdatedDate);
    assertSnapshotRefreshed(snapshotTwo, originalUpdatedDate);
    verify(monthlyDividendSnapshotRepository).saveAll(List.of(snapshotOne, snapshotTwo));
    verify(monthlyDividendSnapshotRepository, never()).deleteAll(any());
  }

  @Test
  void deleteRemovesAllSnapshotsWhenNoPayoutsRemain() {
    StockItem stockItem = createStockItem("O");
    MonthlyDividendPayout payout =
        createPayout(stockItem.getId(), "2026-03-20", "2026-03-31", "1.20", "0.18");
    List<MonthlyDividendSnapshot> snapshots =
        List.of(createSnapshot(stockItem.getId(), 10, "42.50"));

    when(stockItemRepository.findBySymbol("O")).thenReturn(stockItem);
    when(monthlyDividendPayoutRepository.findByStockItemIdAndRecordDateAndPayDate(
            stockItem.getId(), LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 31)))
        .thenReturn(Optional.of(payout));
    when(monthlyDividendSnapshotRepository.findByStockItemIdOrderByUpdatedDateDesc(
            stockItem.getId()))
        .thenReturn(snapshots);
    when(monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItem.getId()))
        .thenReturn(List.of());

    monthlyDividendPayoutService.deleteBySymbolAndDates(
        "O", LocalDate.of(2026, 3, 20), LocalDate.of(2026, 3, 31));

    verify(monthlyDividendPayoutRepository).delete(payout);
    verify(monthlyDividendSnapshotRepository).deleteAll(snapshots);
    verify(monthlyDividendSnapshotRepository, never()).saveAll(any());
  }

  private void assertSnapshotRefreshed(
      MonthlyDividendSnapshot snapshot, Instant originalUpdatedDate) {
    assertThat(snapshot.getAsOfDate()).isEqualTo(LocalDate.of(2026, 3, 31));
    assertThat(snapshot.getLatestMonthlyDividendPerShare()).isEqualByComparingTo("1.2000");
    assertThat(snapshot.getAverageMonthlyDividendPerShare1y()).isEqualByComparingTo("1.0000");
    assertThat(snapshot.getAverageTaxableBaseRatio1y()).isEqualByComparingTo("15.00");
    assertThat(snapshot.getUpdatedDate()).isAfter(originalUpdatedDate);
  }

  private StockItem createStockItem(String symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setId(UUID.randomUUID());
    stockItem.setSymbol(symbol);
    stockItem.setName(symbol + " Inc.");
    stockItem.setMarket("NYSE");
    return stockItem;
  }

  private MonthlyDividendSnapshot createSnapshot(
      UUID stockItemId, int heldQuantity, String averageBuyPrice) {
    MonthlyDividendSnapshot snapshot = new MonthlyDividendSnapshot();
    snapshot.setId(UUID.randomUUID());
    snapshot.setUserId(UUID.randomUUID());
    snapshot.setStockItemId(stockItemId);
    snapshot.setHeldQuantity(heldQuantity);
    snapshot.setAverageBuyPrice(new BigDecimal(averageBuyPrice));
    snapshot.setAsOfDate(LocalDate.of(2025, 12, 31));
    snapshot.setLatestMonthlyDividendPerShare(new BigDecimal("0.10"));
    snapshot.setAverageMonthlyDividendPerShare1y(new BigDecimal("0.10"));
    snapshot.setAverageTaxableBaseRatio1y(new BigDecimal("1.00"));
    snapshot.setCreatedDate(Instant.parse("2026-01-01T00:00:00Z"));
    snapshot.setUpdatedDate(Instant.parse("2026-01-02T00:00:00Z"));
    return snapshot;
  }

  private MonthlyDividendPayout createPayout(
      UUID stockItemId,
      String recordDate,
      String payDate,
      String dividendAmountPerShare,
      String taxableBasePerShare) {
    MonthlyDividendPayout payout = new MonthlyDividendPayout();
    payout.setId(UUID.randomUUID());
    payout.setStockItemId(stockItemId);
    payout.setRecordDate(LocalDate.parse(recordDate));
    payout.setPayDate(LocalDate.parse(payDate));
    payout.setDistributionRatePct(BigDecimal.ZERO);
    payout.setDividendAmountPerShare(new BigDecimal(dividendAmountPerShare));
    payout.setTaxableBasePerShare(new BigDecimal(taxableBasePerShare));
    payout.setCreatedDate(Instant.parse("2026-01-01T00:00:00Z"));
    payout.setUpdatedDate(Instant.parse("2026-01-02T00:00:00Z"));
    return payout;
  }
}
