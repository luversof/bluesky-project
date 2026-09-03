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
    if (userId == null) {
      return List.of();
    }
    String zone = (zoneId != null ? zoneId : ZoneId.of("Asia/Seoul")).getId();
    Map<Integer, BigDecimal[]> byYear = new LinkedHashMap<>();
    for (YearlyTradeCost row : tradeRepository.findYearlyCost(userId, startDate, endDate, zone)) {
      BigDecimal[] slot = byYear.computeIfAbsent(row.year(), k -> newSlot());
      slot[0] = nz(row.fee());
      slot[1] = nz(row.tax());
      slot[2] = nz(row.realizedProfit());
    }
    for (YearlyDividendIncome row :
        dividendRepository.findYearlyIncome(userId, startDate, endDate, zone)) {
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
                    year, slot[0], slot[1], slot[2], slot[3], slot[4], slot[5], slot[6])));
    // 최신이 위로. 표는 늘 최근부터 읽는다.
    result.sort(Comparator.comparingInt(YearlyCostSummary::year).reversed());
    return result;
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
