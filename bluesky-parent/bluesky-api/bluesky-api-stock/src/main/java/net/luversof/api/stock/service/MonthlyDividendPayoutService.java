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
    UUID stockItemId = request != null ? request.getStockItemId() : null;
    String symbol = request != null ? request.getSymbol() : null;
    if (stockItemId == null && StringUtils.hasText(symbol)) {
      // 조회에서 모르는 심볼은 '해당 없음'이 정답이다. 예외를 던지면 화면이 통째로 오류가 된다
      // (실측: 시뮬레이터에서 등록되지 않은 심볼을 고르면 페이지 전체가 오류 화면). 그렇다고 null 로
      // 두면 아래 분기가 '전체 조회'로 빠져 필터가 사라지므로, 여기서 빈 결과로 끊는다.
      // 저장/삭제 경로(upsert, deleteBySymbolAndDates)는 종전대로 모르는 심볼을 거부한다.
      StockItem resolved = stockItemRepository.findBySymbol(symbol.trim());
      if (resolved == null) {
        return List.of();
      }
      stockItemId = resolved.getId();
    }

    List<MonthlyDividendPayout> payouts =
        stockItemId != null
            ? monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
                stockItemId)
            : monthlyDividendPayoutRepository.findAllByOrderByPayDateDescRecordDateDesc();

    List<MonthlyDividendPayout> filtered =
        payouts.stream()
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
            .toList();

    // 종목 정보는 1회 일괄 조회한다. (행마다 findById 하면 행 수만큼 SELECT 가 나가는 N+1)
    Map<UUID, StockItem> stockItemById = loadStockItems(filtered);
    return filtered.stream()
        .map(payout -> toResponse(payout, stockItemById.get(payout.getStockItemId())))
        .toList();
  }

  private Map<UUID, StockItem> loadStockItems(List<MonthlyDividendPayout> payouts) {
    Set<UUID> ids =
        payouts.stream()
            .map(MonthlyDividendPayout::getStockItemId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    Map<UUID, StockItem> byId = new HashMap<>();
    if (!ids.isEmpty()) {
      stockItemRepository.findAllById(ids).forEach(item -> byId.put(item.getId(), item));
    }
    return byId;
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
    requireConsistentDates(request.getRecordDate(), request.getPayDate());
    requireTaxableWithinDividend(
        request.getTaxableBasePerShare(), request.getDividendAmountPerShare());

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

    SnapshotStats stats = computeSnapshotStats(stockItem.getId());
    if (stats == null) {
      monthlyDividendSnapshotRepository.deleteAll(snapshots);
      return;
    }

    Instant now = Instant.now();
    snapshots.forEach(
        snapshot -> {
          snapshot.setAsOfDate(
              stats.asOfDate() != null ? stats.asOfDate() : snapshot.getAsOfDate());
          snapshot.setLatestMonthlyDividendPerShare(stats.latestPerShare());
          snapshot.setAverageMonthlyDividendPerShare1y(stats.averagePerShare1y());
          snapshot.setAverageTaxableBaseRatio1y(stats.taxableBaseRatio1y());
          snapshot.setUpdatedDate(now);
        });
    monthlyDividendSnapshotRepository.saveAll(snapshots);
  }

  /**
   * 종목의 최근 12개월 지급 이력으로 월배당 통계(최신/평균 주당 배당, 과세표준 비율)를 계산한다. 지급 이력이 없으면 null을 반환한다. 스냅샷 갱신과 외부
   * import에서 동일한 계산을 재사용한다.
   */
  public SnapshotStats computeSnapshotStats(UUID stockItemId) {
    if (stockItemId == null) {
      return null;
    }

    List<MonthlyDividendPayout> payouts =
        monthlyDividendPayoutRepository.findByStockItemIdOrderByPayDateDescRecordDateDesc(
            stockItemId);
    if (payouts.isEmpty()) {
      return null;
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

    return new SnapshotStats(
        asOfDate,
        latestDividendAmountPerShare,
        averageDividendAmountPerShare1y,
        averageTaxableBaseRatio1y);
  }

  /** 월배당 스냅샷 통계 계산 결과(최근 1년 기준). */
  public record SnapshotStats(
      LocalDate asOfDate,
      BigDecimal latestPerShare,
      BigDecimal averagePerShare1y,
      BigDecimal taxableBaseRatio1y) {}

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

  /**
   * 지급일은 기준일보다 앞설 수 없다.
   *
   * <p>지급이력 전수(202건)의 기준일→지급일 간격은 2~8 일이었다. 뒤집힌 행이 한 번 들어오면 배당 달력의 "다가올 지급일" 과 월별 집계가 그 행 때문에
   * 어긋나는데, 값 자체는 그럴듯해 보여 원인을 찾기 어렵다.
   */
  private void requireConsistentDates(java.time.LocalDate recordDate, java.time.LocalDate payDate) {
    if (recordDate != null && payDate != null && payDate.isBefore(recordDate)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "payDate must not be before recordDate: " + payDate + " < " + recordDate);
    }
  }

  /**
   * 주당 과세표준은 주당 배당을 넘을 수 없다(배당 중 과세 대상 몫이므로).
   *
   * <p>넘어서면 시뮬레이터의 과세표준 비중이 100% 를 넘고, 세후 예상 배당이 실제보다 작게 나온다. 실측상 202건 모두 이 관계를 지키고 있어, 깨진 값이 들어오는
   * 것을 여기서 막는다.
   */
  private void requireTaxableWithinDividend(BigDecimal taxableBase, BigDecimal dividendAmount) {
    if (taxableBase != null
        && dividendAmount != null
        && taxableBase.compareTo(dividendAmount) > 0) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "taxableBasePerShare must not exceed dividendAmountPerShare: "
              + taxableBase
              + " > "
              + dividendAmount);
    }
  }

  private void requireNonNegative(BigDecimal value, String message) {
    if (value != null && value.signum() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
  }
}
