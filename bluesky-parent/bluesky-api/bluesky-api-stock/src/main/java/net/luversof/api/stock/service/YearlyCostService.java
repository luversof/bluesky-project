package net.luversof.api.stock.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.api.stock.domain.YearlyDividendIncome;
import net.luversof.api.stock.domain.YearlyTradeCost;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.YearlyCostSummary;

/**
 * 연도별 세금·비용 요약.
 *
 * <p>합계는 DB 에서 낸다 &mdash; 화면이 원장을 통째로 받아 더하면 응답이 원장 크기를 따라간다(실측 2026-09-01: 거래 251 행 80.7 KB + 배당
 * 194 행 78.4 KB = 159 KB 를 요약 몇 줄 만들자고 실어 보내게 된다).
 */
@Service
public class YearlyCostService {

  @Autowired private TradeRepository tradeRepository;

  @Autowired private DividendRepository dividendRepository;

  public void setTradeRepository(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  public void setDividendRepository(DividendRepository dividendRepository) {
    this.dividendRepository = dividendRepository;
  }

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /**
   * @param zoneId 해를 가르는 존. 세금은 그 나라 기준이라 화면이 보는 존과 같아야 한다(기본 Asia/Seoul).
   */
  public List<YearlyCostSummary> findYearlyCost(
      UUID userId, Instant startDate, Instant endDate, ZoneId zoneId) {
    return findYearlyCost(userId, startDate, endDate, zoneId, null, null);
  }

  /**
   * 계좌/종목을 좁혀서 본다.
   *
   * <p>2026-09-12 까지 이 집계에는 좁히는 수단이 없었다. 자산 성장 화면은 계좌·종목 필터를 갖고 있고 다른 구역(종목별 기여·연도별 성과·매매 내역)은 전부 그
   * 필터를 따르는데, 연도별 세금·비용 표만 늘 전 계좌 합계를 그렸다(실측: 한 계좌로 좁혀도 14 행 · 합계 +225,630,135 원이 <b>바이트 단위로
   * 같았다</b>). 같은 화면에서 한 표만 다른 범위를 말하면 어느 쪽이 맞는지 화면으로는 알 수 없다.
   *
   * <p>목록은 쉼표로 이어 한 문자열로 넘긴다 &mdash; {@code IN (:list)} 는 빈 목록에서 {@code IN ()} 이 되어 문법 오류가 나므로, 이
   * 저장소가 이미 쓰는 {@code string_to_array(...)::uuid[]} 방식을 따른다. 빈 목록은 "좁히지 않음"(null)과 같게 둔다.
   */
  public List<YearlyCostSummary> findYearlyCost(
      UUID userId,
      Instant startDate,
      Instant endDate,
      ZoneId zoneId,
      List<UUID> accountIdList,
      List<UUID> stockItemIdList) {
    if (userId == null) {
      return List.of();
    }
    String accountIds = joinIds(accountIdList);
    String stockItemIds = joinIds(stockItemIdList);
    String zone = (zoneId != null ? zoneId : ZoneId.of("Asia/Seoul")).getId();
    Map<Integer, BigDecimal[]> byYear = new LinkedHashMap<>();
    // 매도 건수는 금액이 아니라 세는 값이라 따로 담는다. 배당만 있는 해는 매매가 없으니 0 이다.
    Map<Integer, Long> sellCountByYear = new LinkedHashMap<>();
    for (YearlyTradeCost row :
        tradeRepository.findYearlyCost(
            userId, startDate, endDate, zone, accountIds, stockItemIds)) {
      BigDecimal[] slot = byYear.computeIfAbsent(row.year(), k -> newSlot());
      slot[0] = nz(row.fee());
      slot[1] = nz(row.tax());
      slot[2] = nz(row.realizedProfit());
      sellCountByYear.put(row.year(), row.sellCount());
    }
    for (YearlyDividendIncome row :
        dividendRepository.findYearlyIncome(
            userId, startDate, endDate, zone, accountIds, stockItemIds)) {
      BigDecimal[] slot = byYear.computeIfAbsent(row.year(), k -> newSlot());
      slot[3] = nz(row.grossAmount());
      slot[4] = nz(row.taxableAmount());
      slot[5] = nz(row.tax());
      // 세후 = 세전 - 세금 - 수수료. Dividend.getNetAmount() 와 같은 식이다.
      slot[6] = nz(row.grossAmount()).subtract(nz(row.tax())).subtract(nz(row.fee()));
    }
    List<YearlyCostSummary> result = new ArrayList<>();
    byYear.forEach(
        (year, slot) ->
            result.add(
                new YearlyCostSummary(
                    year,
                    slot[0],
                    slot[1],
                    slot[2],
                    slot[3],
                    slot[4],
                    slot[5],
                    slot[6],
                    sellCountByYear.getOrDefault(year, 0L))));
    // 최신이 위로. 표는 늘 최근부터 읽는다.
    result.sort(Comparator.comparingInt(YearlyCostSummary::year).reversed());
    return result;
  }

  /** 빈 목록은 좁히지 않는 것과 같게 둔다 - 화면이 "선택 없음" 을 빈 목록으로 보내기 때문이다. */
  private static String joinIds(List<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for (UUID id : ids) {
      if (id == null) {
        continue;
      }
      if (sb.length() > 0) {
        sb.append(',');
      }
      sb.append(id);
    }
    return sb.length() == 0 ? null : sb.toString();
  }

  private static BigDecimal[] newSlot() {
    return new BigDecimal[] {
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO,
      BigDecimal.ZERO
    };
  }
}
