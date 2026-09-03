package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;

/**
 * 매매 내역을 달/해 단위로 쪼갠 줄.
 *
 * <p>매매 화면은 고른 기간 전체를 카드 몇 장으로 답하고, 그 아래는 <b>거래 하나하나</b>다. 그 사이가 비어 있어 "어느 해에 얼마나 사고팔았나" 는 월별 매매 금액
 * 막대 차트를 눈으로 훑어야만 알 수 있었다. 차트는 모양은 주지만 수를 주지 않는다 &mdash; 그 해 수수료가 얼마였는지, 실현손익이 얼마였는지는 읽을 수 없다.
 *
 * <p>집계는 <b>게이트에서 한다</b>. 화면이 이미 고른 기간의 매매 원장을 통째로 받고 있어(실측 2026-09-03: 251 건) 원격을 한 번 더 부를 까닭이 없다.
 *
 * <p>실측 2026-09-03(전체 기간): 해 단위 14 줄 · 매수 1,785,623,822 · 매도 1,374,924,880 · 수수료 98,836 · 증권거래세
 * 1,885,967 · 실현손익 225,584,549. 뒤 세 값은 연도별 세금·비용 표와 <b>정확히 같다</b> &mdash; 같은 원장을 다른 길로 더한 값이므로 같아야
 * 한다.
 */
public final class StockTradePeriodUtil {

  private StockTradePeriodUtil() {}

  /**
   * 달 단위로 쪼갤 수 있는 최대 개월. 넘으면 해 단위로 묶는다.
   *
   * <p>api-stock 의 성과 쪼갬과 같은 기준이다 &mdash; 두 화면이 같은 기간에서 다른 단위를 고르면 읽는 사람이 둘을 견줄 수 없다. 전 구간을 달 단위로
   * 내면 60 줄이 된다(실측 2026-09-03).
   */
  static final long MAX_MONTHLY_MONTHS = 36L;

  /**
   * 한 구간.
   *
   * @param count 그 구간의 매매 건수(매수 + 매도)
   * @param realizedProfit 매도에만 붙는다. 매수 행의 값은 더하지 않는다 &mdash; 화면 위 요약 카드가 쓰는 규칙과 같아야 두 자리가 같은 뜻을
   *     말한다.
   */
  public record TradePeriod(
      String unit,
      String label,
      LocalDate fromDate,
      LocalDate toDate,
      int count,
      int buyCount,
      int sellCount,
      BigDecimal buyAmount,
      BigDecimal sellAmount,
      BigDecimal fee,
      BigDecimal tax,
      BigDecimal realizedProfit) {}

  private static BigDecimal nz(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  /** 구간이 3 년을 넘으면 해 단위. 줄이 하나도 없으면 달 단위로 둔다(그릴 것이 없어 뜻이 없다). */
  static String resolveUnit(List<LocalDate> dates) {
    if (dates == null || dates.size() < 2) {
      return "MONTH";
    }
    LocalDate first = dates.stream().min(Comparator.naturalOrder()).orElse(null);
    LocalDate last = dates.stream().max(Comparator.naturalOrder()).orElse(null);
    if (first == null || last == null) {
      return "MONTH";
    }
    return ChronoUnit.MONTHS.between(first.withDayOfMonth(1), last.withDayOfMonth(1))
            > MAX_MONTHLY_MONTHS
        ? "YEAR"
        : "MONTH";
  }

  /**
   * 매매가 <b>있는</b> 구간만 낸다.
   *
   * <p>거래가 없던 달을 0 으로 채워 넣지 않는다 &mdash; 여기는 매매 '내역' 이라 아무 일도 없던 구간에는 적을 내역이 없다(보유가 이어지는 자산 성장 표와 다른
   * 점이다). 최신이 위로 온다.
   */
  public static List<TradePeriod> of(List<TradeResponse> trades, ZoneId zoneId) {
    if (trades == null || trades.isEmpty()) {
      return List.of();
    }
    ZoneId zone = zoneId != null ? zoneId : ZoneId.of("Asia/Seoul");
    List<LocalDate> dates = new ArrayList<>();
    for (TradeResponse trade : trades) {
      if (trade != null && trade.tradeDate() != null) {
        dates.add(trade.tradeDate().atZone(zone).toLocalDate());
      }
    }
    if (dates.isEmpty()) {
      return List.of();
    }
    String unit = resolveUnit(dates);
    boolean yearly = "YEAR".equals(unit);

    // 라벨 -> 누적. 순서는 아래에서 다시 세우므로 여기서는 모으기만 한다.
    Map<String, Object[]> buckets = new LinkedHashMap<>();
    for (TradeResponse trade : trades) {
      if (trade == null || trade.tradeDate() == null) {
        continue;
      }
      LocalDate date = trade.tradeDate().atZone(zone).toLocalDate();
      String label =
          yearly
              ? String.format(Locale.ROOT, "%04d", date.getYear())
              : String.format(Locale.ROOT, "%04d-%02d", date.getYear(), date.getMonthValue());
      Object[] slot =
          buckets.computeIfAbsent(
              label,
              key ->
                  new Object[] {
                    date,
                    date,
                    0,
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
                  });
      if (date.isBefore((LocalDate) slot[0])) {
        slot[0] = date;
      }
      if (date.isAfter((LocalDate) slot[1])) {
        slot[1] = date;
      }
      boolean sell = trade.type() == TradeType.SELL;
      if (sell) {
        slot[3] = (int) slot[3] + 1;
        slot[5] = ((BigDecimal) slot[5]).add(nz(trade.amount()));
        // 실현손익은 매도에만 붙는다. 매수 행에도 값이 실려 오면 두 번 세게 된다.
        slot[8] = ((BigDecimal) slot[8]).add(nz(trade.realizedProfit()));
      } else {
        slot[2] = (int) slot[2] + 1;
        slot[4] = ((BigDecimal) slot[4]).add(nz(trade.amount()));
      }
      slot[6] = ((BigDecimal) slot[6]).add(nz(trade.fee()));
      slot[7] = ((BigDecimal) slot[7]).add(nz(trade.tax()));
    }

    List<TradePeriod> result = new ArrayList<>();
    buckets.forEach(
        (label, slot) ->
            result.add(
                new TradePeriod(
                    unit,
                    label,
                    (LocalDate) slot[0],
                    (LocalDate) slot[1],
                    (int) slot[2] + (int) slot[3],
                    (int) slot[2],
                    (int) slot[3],
                    (BigDecimal) slot[4],
                    (BigDecimal) slot[5],
                    (BigDecimal) slot[6],
                    (BigDecimal) slot[7],
                    (BigDecimal) slot[8])));
    // 최신이 위로. 표는 늘 최근부터 읽는다(다른 성과 표와 같은 차례).
    result.sort(Comparator.comparing(TradePeriod::label).reversed());
    return result;
  }

  /**
   * 합계 줄.
   *
   * <p>여덟 값이 모두 그냥 더하면 되는 값이다 &mdash; 성과 표처럼 곱해서 이어야 하는 수익률도, 합계라는 것이 없는 기말 평가액도 없다. 그래서 줄 이름도 '전체
   * 기간' 이 아니라 '합계' 다.
   *
   * <p>이 값은 화면 위 요약 카드와 <b>같아야 한다</b> &mdash; 같은 원장을 같은 규칙으로 더한 값이기 때문이다. 실측 2026-09-03(251 건, 해 단위
   * 14 줄): 매수 1,785,623,822 · 매도 1,374,924,880 · 수수료 98,836 · 증권거래세 1,885,967 · 실현손익 225,584,549.
   */
  public record TradeTotals(
      int count,
      int buyCount,
      int sellCount,
      BigDecimal buyAmount,
      BigDecimal sellAmount,
      BigDecimal fee,
      BigDecimal tax,
      BigDecimal realizedProfit) {}

  /** 줄 순서를 타지 않는다. */
  public static TradeTotals total(List<TradePeriod> rows) {
    int count = 0;
    int buyCount = 0;
    int sellCount = 0;
    BigDecimal buyAmount = BigDecimal.ZERO;
    BigDecimal sellAmount = BigDecimal.ZERO;
    BigDecimal fee = BigDecimal.ZERO;
    BigDecimal tax = BigDecimal.ZERO;
    BigDecimal realizedProfit = BigDecimal.ZERO;
    if (rows != null) {
      for (TradePeriod row : rows) {
        if (row == null) {
          continue;
        }
        count += row.count();
        buyCount += row.buyCount();
        sellCount += row.sellCount();
        buyAmount = buyAmount.add(nz(row.buyAmount()));
        sellAmount = sellAmount.add(nz(row.sellAmount()));
        fee = fee.add(nz(row.fee()));
        tax = tax.add(nz(row.tax()));
        realizedProfit = realizedProfit.add(nz(row.realizedProfit()));
      }
    }
    return new TradeTotals(
        count, buyCount, sellCount, buyAmount, sellAmount, fee, tax, realizedProfit);
  }
}
