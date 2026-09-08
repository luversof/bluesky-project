package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.util.StockCombinedProfitUtil.Breakdown;

/**
 * "배당이 평가손실을 얼마나 메웠나" &mdash; 보유 종목별 누적 배당과 평가손익을 한 줄에 놓는다.
 *
 * <p>자산 현황 표에는 평가 손익 · 누적 배당 · 합산 손익 열이 다 있었지만, 커버드콜 ETF 보유자의 핵심 질문(원금이 깎이는 만큼 배당이 들어오고 있나)에 답하려면
 * 숫자 세 열을 눈으로 빼야 했다. 실측 2026-09-08: 보유 ETF 9 종 중 5 종이 배당을 다 더해도 합산 손익이 마이너스였는데, 표만 보고는 그 사실이 눈에 띄지
 * 않았다.
 *
 * <p>막대는 <b>줄 안에서만</b> 견준다(그 줄의 손실과 배당 중 큰 쪽을 100 으로). 종목끼리 절대 길이를 비교하게 두면 삼성전자(평가익 9.99 억, 실측)가
 * 나머지를 1px 로 눌러 버린다 &mdash; 자산 현황의 다른 차트들이 이미 그 문제를 안고 있다. 이 카드가 답하는 것은 "그 종목에서 배당이 손실의 몇 %를 덮었나"
 * 이지 종목 간 크기 비교가 아니다.
 */
public final class StockDividendCoverageUtil {

  private StockDividendCoverageUtil() {}

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /**
   * 한 종목.
   *
   * @param coveragePct 배당 ÷ |평가손실| (%). 평가손실이 없으면 null.
   * @param lossBarPct 막대 길이(0~100). 그 줄의 손실과 배당 중 큰 쪽이 100.
   * @param dividendBarPct 막대 길이(0~100)
   */
  public record Row(
      UUID stockItemId,
      String stockItemName,
      BigDecimal evaluationProfit,
      BigDecimal dividendTotal,
      BigDecimal combinedProfit,
      Integer coveragePct,
      int lossBarPct,
      int dividendBarPct) {

    public boolean hasLoss() {
      return evaluationProfit.signum() < 0;
    }

    /** 배당이 손실을 다 덮고도 남았는지. */
    public boolean fullyCovered() {
      return coveragePct != null && coveragePct >= 100;
    }
  }

  /**
   * 배당을 받았거나 평가손실이 있는 보유 종목만. 손실이 큰 종목부터, 손실 없는 종목은 뒤에 배당 큰 순.
   *
   * @param stockAggregated 자산 현황의 종목별 집계(보유 중인 것)
   * @param breakdownByStock 종목별 실현·배당·합산({@link StockCombinedProfitUtil#byStockItem})
   */
  public static List<Row> of(
      List<TradeProfit> stockAggregated, Map<UUID, Breakdown> breakdownByStock) {
    List<Row> rows = new ArrayList<>();
    if (stockAggregated == null) {
      return rows;
    }
    for (TradeProfit item : stockAggregated) {
      if (item == null || item.stockItemId() == null) {
        continue;
      }
      Breakdown breakdown =
          breakdownByStock != null ? breakdownByStock.get(item.stockItemId()) : null;
      BigDecimal evaluationProfit = nz(item.evaluationProfit());
      BigDecimal dividend = breakdown != null ? nz(breakdown.dividendTotal()) : BigDecimal.ZERO;
      if (evaluationProfit.signum() >= 0 && dividend.signum() <= 0) {
        continue; // 손실도 배당도 없으면 이 카드가 할 말이 없다
      }
      BigDecimal loss = evaluationProfit.signum() < 0 ? evaluationProfit.abs() : BigDecimal.ZERO;
      BigDecimal scale = loss.max(dividend);
      Integer coverage = null;
      if (loss.signum() > 0) {
        coverage =
            dividend
                .multiply(BigDecimal.valueOf(100))
                .divide(loss, 0, RoundingMode.HALF_UP)
                .intValue();
      }
      BigDecimal combined =
          breakdown != null ? nz(breakdown.combinedProfit()) : evaluationProfit.add(dividend);
      rows.add(
          new Row(
              item.stockItemId(),
              item.stockItemName(),
              evaluationProfit,
              dividend,
              combined,
              coverage,
              barPct(loss, scale),
              barPct(dividend, scale)));
    }
    rows.sort(
        Comparator.comparing((Row r) -> r.evaluationProfit().signum() < 0 ? 0 : 1)
            .thenComparing(Row::evaluationProfit)
            .thenComparing(Row::dividendTotal, Comparator.reverseOrder()));
    return rows;
  }

  private static int barPct(BigDecimal value, BigDecimal scale) {
    if (scale.signum() <= 0 || value.signum() <= 0) {
      return 0;
    }
    return value
        .multiply(BigDecimal.valueOf(100))
        .divide(scale, 0, RoundingMode.HALF_UP)
        .intValue();
  }
}
