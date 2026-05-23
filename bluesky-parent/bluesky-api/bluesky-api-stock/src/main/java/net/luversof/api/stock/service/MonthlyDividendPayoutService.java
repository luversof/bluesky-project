package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.MonthlyDividendPayout;
import net.luversof.api.stock.domain.MonthlyDividendSnapshot;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendPayoutRepository;
import net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendPayoutRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendPayoutResponse;

@Service
public class MonthlyDividendPayoutService {

  @Autowired private MonthlyDividendPayoutRepository monthlyDividendPayoutRepository;

  @Autowired private MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository;

  @Autowired private StockItemRepository stockItemRepository;

  public List<MonthlyDividendPayoutResponse> findPayouts(MonthlyDividendPayoutRequest request) {
    UUID stockItemId =
        resolveStockItemId(
            request != null ? request.getStockItemId() : null,
            request != null ? request.getSymbol() : null);

    List<MonthlyDividendPayout> payouts =
        stockItemId != null
            ? monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
                stockItemId)
            : monthlyDividendPayoutRepository.findAllByOrderByPayDateDescRecordDateDesc();

    return payouts.stream()
        .filter(
            payout ->
                request == null
                    || request.getStartDate() == null
                    || !payout.getPayDate().isBefore(request.getStartDate()))
        .filter(
            payout ->
                request == null
                    || request.getEndDate() == null
                    || !payout.getPayDate().isAfter(request.getEndDate()))
        .map(payout -> toResponse(payout, null))
        .toList();
  }

  public MonthlyDividendPayoutResponse upsert(MonthlyDividendPayoutUpsertRequest request) {
    if (request == null || !StringUtils.hasText(request.getSymbol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }
    if (request.getRecordDate() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recordDate is required");
    }
    if (request.getPayDate() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payDate is required");
    }

    requireNonNegative(request.getDistributionRatePct(), "distributionRatePct must be >= 0");
    requireNonNegative(request.getDividendAmountPerShare(), "dividendAmountPerShare must be >= 0");
    requireNonNegative(request.getTaxableBasePerShare(), "taxableBasePerShare must be >= 0");

    StockItem stockItem = resolveStockItem(request.getSymbol());
    Instant now = Instant.now();
    MonthlyDividendPayout payout =
        monthlyDividendPayoutRepository
            .findByStockItemIdAndRecordDateAndPayDate(
                stockItem.getId(), request.getRecordDate(), request.getPayDate())
            .orElseGet(MonthlyDividendPayout::new);

    if (payout.getId() == null) {
      payout.setCreatedDate(now);
    }

    payout.setStockItemId(stockItem.getId());
    payout.setRecordDate(request.getRecordDate());
    payout.setPayDate(request.getPayDate());
    payout.setDistributionRatePct(request.getDistributionRatePct());
    payout.setDividendAmountPerShare(safe(request.getDividendAmountPerShare()));
    payout.setTaxableBasePerShare(safe(request.getTaxableBasePerShare()));
    payout.setUpdatedDate(now);

    MonthlyDividendPayout savedPayout = monthlyDividendPayoutRepository.save(payout);
    refreshSnapshotsForStockItem(stockItem);

    return toResponse(savedPayout, stockItem);
  }

  public void deleteBySymbolAndDates(
      String symbol, java.time.LocalDate recordDate, java.time.LocalDate payDate) {
    if (!StringUtils.hasText(symbol)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }
    if (recordDate == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recordDate is required");
    }
    if (payDate == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "payDate is required");
    }

    StockItem stockItem = resolveStockItem(symbol);
    MonthlyDividendPayout payout =
        monthlyDividendPayoutRepository
            .findByStockItemIdAndRecordDateAndPayDate(stockItem.getId(), recordDate, payDate)
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Monthly dividend payout not found: "
                            + symbol
                            + " / "
                            + recordDate
                            + " / "
                            + payDate));
    monthlyDividendPayoutRepository.delete(payout);
    refreshSnapshotsForStockItem(stockItem);
  }

  private void refreshSnapshotsForStockItem(StockItem stockItem) {
    if (stockItem == null || stockItem.getId() == null) {
      return;
    }

    List<MonthlyDividendSnapshot> snapshots =
        monthlyDividendSnapshotRepository.findByStockItemIdOrderByUpdatedDateDesc(
            stockItem.getId());
    if (snapshots.isEmpty()) {
      return;
    }

    List<MonthlyDividendPayout> payouts =
        monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItem.getId());
    if (payouts.isEmpty()) {
      monthlyDividendSnapshotRepository.deleteAll(snapshots);
      return;
    }

    List<MonthlyDividendPayout> lastYearRows = payouts.stream().limit(12).toList();
    MonthlyDividendPayout latestPayout = lastYearRows.get(0);
    LocalDate asOfDate =
        latestPayout.getPayDate() != null
            ? latestPayout.getPayDate()
            : latestPayout.getRecordDate();
    BigDecimal latestDividendAmountPerShare =
        safe(latestPayout.getDividendAmountPerShare()).setScale(4, RoundingMode.HALF_UP);
    BigDecimal averageDividendAmountPerShare1y =
        lastYearRows.stream()
            .map(MonthlyDividendPayout::getDividendAmountPerShare)
            .map(this::safe)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .divide(BigDecimal.valueOf(lastYearRows.size()), 4, RoundingMode.HALF_UP);

    List<BigDecimal> taxableBaseRatios =
        lastYearRows.stream()
            .filter(row -> safe(row.getDividendAmountPerShare()).signum() > 0)
            .map(
                row ->
                    safe(row.getTaxableBasePerShare())
                        .multiply(BigDecimal.valueOf(100))
                        .divide(safe(row.getDividendAmountPerShare()), 2, RoundingMode.HALF_UP))
            .toList();
    BigDecimal averageTaxableBaseRatio1y =
        taxableBaseRatios.isEmpty()
            ? BigDecimal.ZERO
            : taxableBaseRatios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(taxableBaseRatios.size()), 2, RoundingMode.HALF_UP);

    Instant now = Instant.now();
    snapshots.forEach(
        snapshot -> {
          snapshot.setAsOfDate(asOfDate != null ? asOfDate : snapshot.getAsOfDate());
          snapshot.setLatestMonthlyDividendPerShare(latestDividendAmountPerShare);
          snapshot.setAverageMonthlyDividendPerShare1y(averageDividendAmountPerShare1y);
          snapshot.setAverageTaxableBaseRatio1y(averageTaxableBaseRatio1y);
          snapshot.setUpdatedDate(now);
        });
    monthlyDividendSnapshotRepository.saveAll(snapshots);
  }

  private MonthlyDividendPayoutResponse toResponse(
      MonthlyDividendPayout payout, StockItem providedStockItem) {
    StockItem stockItem =
        providedStockItem != null
            ? providedStockItem
            : stockItemRepository.findById(payout.getStockItemId()).orElse(null);

    return new MonthlyDividendPayoutResponse(
        payout.getId(),
        payout.getStockItemId(),
        stockItem != null ? stockItem.getSymbol() : "",
        stockItem != null ? stockItem.getName() : "",
        payout.getRecordDate(),
        payout.getPayDate(),
        payout.getDistributionRatePct(),
        payout.getDividendAmountPerShare(),
        payout.getTaxableBasePerShare(),
        payout.getUpdatedDate());
  }

  private UUID resolveStockItemId(UUID stockItemId, String symbol) {
    if (stockItemId != null) {
      return stockItemId;
    }

    if (!StringUtils.hasText(symbol)) {
      return null;
    }

    return resolveStockItem(symbol).getId();
  }

  private StockItem resolveStockItem(String symbol) {
    StockItem stockItem = stockItemRepository.findBySymbol(symbol.trim());
    if (stockItem == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown stock symbol: " + symbol);
    }
    return stockItem;
  }

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private void requireNonNegative(BigDecimal value, String message) {
    if (value != null && value.signum() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }
}
