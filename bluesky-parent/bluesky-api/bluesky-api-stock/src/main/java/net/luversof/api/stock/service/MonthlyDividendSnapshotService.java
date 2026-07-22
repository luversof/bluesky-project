package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.MonthlyDividendSnapshot;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendSnapshotRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendSnapshotResponse;

@Service
public class MonthlyDividendSnapshotService {

  @Autowired private MonthlyDividendSnapshotRepository monthlyDividendSnapshotRepository;

  @Autowired private StockItemRepository stockItemRepository;

  @Autowired private StockPriceService stockPriceService;

  public List<MonthlyDividendSnapshotResponse> findByUserId(UUID userId) {
    if (userId == null) {
      return List.of();
    }

    List<MonthlyDividendSnapshot> snapshots =
        monthlyDividendSnapshotRepository.findByUserIdOrderByUpdatedDateDesc(userId);

    // 종목 정보는 1회 일괄 조회한다. (행마다 findById 하면 행 수만큼 SELECT 가 나가는 N+1)
    Map<UUID, StockItem> stockItemById = new HashMap<>();
    Set<UUID> stockItemIds =
        snapshots.stream()
            .map(MonthlyDividendSnapshot::getStockItemId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    if (!stockItemIds.isEmpty()) {
      stockItemRepository
          .findAllById(stockItemIds)
          .forEach(item -> stockItemById.put(item.getId(), item));
    }

    return snapshots.stream()
        .map(snapshot -> toResponse(snapshot, stockItemById.get(snapshot.getStockItemId())))
        .sorted(this::compareResponses)
        .toList();
  }

  public MonthlyDividendSnapshotResponse upsert(MonthlyDividendSnapshotUpsertRequest request) {
    if (request == null || request.getUserId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
    }

    if (!StringUtils.hasText(request.getSymbol())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }

    StockItem stockItem = stockItemRepository.findBySymbol(request.getSymbol().trim());
    if (stockItem == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Unknown stock symbol: " + request.getSymbol());
    }

    Instant now = Instant.now();
    MonthlyDividendSnapshot snapshot =
        monthlyDividendSnapshotRepository
            .findByUserIdAndStockItemId(request.getUserId(), stockItem.getId())
            .orElseGet(MonthlyDividendSnapshot::new);

    if (snapshot.getId() == null) {
      snapshot.setCreatedDate(now);
    }

    snapshot.setUserId(request.getUserId());
    snapshot.setStockItemId(stockItem.getId());
    snapshot.setAsOfDate(request.getAsOfDate() != null ? request.getAsOfDate() : LocalDate.now());
    snapshot.setLatestMonthlyDividendPerShare(safe(request.getLatestMonthlyDividendPerShare()));
    snapshot.setAverageMonthlyDividendPerShare1y(
        safe(request.getAverageMonthlyDividendPerShare1y()));
    snapshot.setAverageTaxableBaseRatio1y(safe(request.getAverageTaxableBaseRatio1y()));
    snapshot.setHeldQuantity(request.getHeldQuantity() != null ? request.getHeldQuantity() : 0);
    snapshot.setAverageBuyPrice(safe(request.getAverageBuyPrice()));
    snapshot.setUpdatedDate(now);

    return toResponse(monthlyDividendSnapshotRepository.save(snapshot), stockItem);
  }

  public void deleteByUserIdAndSymbol(UUID userId, String symbol) {
    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
    }
    if (!StringUtils.hasText(symbol)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbol is required");
    }

    StockItem stockItem = stockItemRepository.findBySymbol(symbol.trim());
    if (stockItem == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown stock symbol: " + symbol);
    }

    monthlyDividendSnapshotRepository
        .findByUserIdAndStockItemId(userId, stockItem.getId())
        .ifPresent(monthlyDividendSnapshotRepository::delete);
  }

  private MonthlyDividendSnapshotResponse toResponse(
      MonthlyDividendSnapshot snapshot, StockItem providedStockItem) {
    StockItem stockItem =
        providedStockItem != null
            ? providedStockItem
            : stockItemRepository.findById(snapshot.getStockItemId()).orElse(null);

    BigDecimal latestMonthlyDividendPerShare = safe(snapshot.getLatestMonthlyDividendPerShare());
    BigDecimal averageMonthlyDividendPerShare1y =
        safe(snapshot.getAverageMonthlyDividendPerShare1y());
    BigDecimal averageTaxableBaseRatio1y = safe(snapshot.getAverageTaxableBaseRatio1y());
    BigDecimal averageBuyPrice = safe(snapshot.getAverageBuyPrice());
    int heldQuantity = snapshot.getHeldQuantity() != null ? snapshot.getHeldQuantity() : 0;
    BigDecimal quantity = BigDecimal.valueOf(heldQuantity);
    BigDecimal currentPrice = stockPriceService.getCurrentPrice(snapshot.getStockItemId());
    BigDecimal currentMarketValue = currentPrice.multiply(quantity);
    BigDecimal expectedMonthlyDividend = averageMonthlyDividendPerShare1y.multiply(quantity);
    BigDecimal expectedMonthlyYieldPct = percent(expectedMonthlyDividend, currentMarketValue);
    BigDecimal expectedAnnualYieldPct = annualize(expectedMonthlyYieldPct);
    BigDecimal totalCost = averageBuyPrice.multiply(quantity);
    BigDecimal expectedMonthlyYieldOnCostPct = percent(expectedMonthlyDividend, totalCost);
    BigDecimal expectedAnnualYieldOnCostPct = annualize(expectedMonthlyYieldOnCostPct);
    BigDecimal expectedTaxableBaseAmount =
        expectedMonthlyDividend
            .multiply(averageTaxableBaseRatio1y)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    BigDecimal totalReturnOnCostPct =
        percent(currentPrice.subtract(averageBuyPrice), averageBuyPrice);
    BigDecimal expectedCombinedReturnPct =
        expectedAnnualYieldOnCostPct.add(totalReturnOnCostPct).setScale(2, RoundingMode.HALF_UP);

    return new MonthlyDividendSnapshotResponse(
        snapshot.getId(),
        snapshot.getUserId(),
        snapshot.getStockItemId(),
        stockItem != null ? stockItem.getSymbol() : "",
        stockItem != null ? stockItem.getName() : "",
        snapshot.getAsOfDate(),
        latestMonthlyDividendPerShare,
        averageMonthlyDividendPerShare1y,
        averageTaxableBaseRatio1y,
        heldQuantity,
        averageBuyPrice,
        currentPrice,
        currentMarketValue,
        expectedMonthlyDividend,
        expectedMonthlyYieldPct,
        expectedAnnualYieldPct,
        expectedMonthlyYieldOnCostPct,
        expectedAnnualYieldOnCostPct,
        expectedTaxableBaseAmount,
        totalReturnOnCostPct,
        expectedCombinedReturnPct,
        snapshot.getUpdatedDate());
  }

  private int compareResponses(
      MonthlyDividendSnapshotResponse left, MonthlyDividendSnapshotResponse right) {
    int compare = compareDesc(left.expectedCombinedReturnPct(), right.expectedCombinedReturnPct());
    if (compare != 0) {
      return compare;
    }

    compare =
        compareDesc(left.expectedAnnualYieldOnCostPct(), right.expectedAnnualYieldOnCostPct());
    if (compare != 0) {
      return compare;
    }

    compare = compareDesc(left.expectedAnnualYieldPct(), right.expectedAnnualYieldPct());
    if (compare != 0) {
      return compare;
    }

    compare = compareAsc(left.averageTaxableBaseRatio1y(), right.averageTaxableBaseRatio1y());
    if (compare != 0) {
      return compare;
    }

    compare = compareDesc(left.expectedMonthlyDividend(), right.expectedMonthlyDividend());
    if (compare != 0) {
      return compare;
    }

    return String.valueOf(left.stockItemSymbol())
        .compareToIgnoreCase(String.valueOf(right.stockItemSymbol()));
  }

  private int compareDesc(BigDecimal left, BigDecimal right) {
    return safe(right).compareTo(safe(left));
  }

  private int compareAsc(BigDecimal left, BigDecimal right) {
    return safe(left).compareTo(safe(right));
  }

  private BigDecimal safe(BigDecimal value) {
    return value != null ? value : BigDecimal.ZERO;
  }

  private BigDecimal annualize(BigDecimal monthlyPercent) {
    return safe(monthlyPercent).multiply(BigDecimal.valueOf(12)).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal percent(BigDecimal numerator, BigDecimal denominator) {
    if (numerator == null || denominator == null || denominator.signum() <= 0) {
      return BigDecimal.ZERO;
    }

    return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 2, RoundingMode.HALF_UP);
  }
}
