package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.web.dto.response.TradeProfitTimeSeriesPoint;

/** 종목 상세 캔들 차트 · 그 시점 평균 단가(사용자 요청 2026-10-02)의 계산. 숫자는 NAVER 2018 년 5:1 분할 꼴이다. */
class StockPriceChartServiceTest {

  private static BigDecimal d(String value) {
    return new BigDecimal(value);
  }

  @Test
  void 시가_고가_저가는_그날_차트_종가_배율로_옮긴다() {
    // 분할 전 날: 수정 종가 140,999 (분배금까지 조금 깎임) · 차트 종가(원주가 704,000 / 5) 140,800. 고가 수정 142,000 ->
    // 141,799.59
    assertEquals(
        0,
        d("141799.59")
            .compareTo(StockPriceChartService.scale(d("142000"), d("140800"), d("140999"))));
    // 배율이 1 이면 그대로(정수 모양 유지)
    BigDecimal same = StockPriceChartService.scale(d("143500"), d("142000"), d("142000"));
    assertEquals(0, d("143500").compareTo(same));
    assertEquals(0, same.scale());
    assertNull(StockPriceChartService.scale(null, d("1"), d("1")));
  }

  private static TradeProfitTimeSeriesPoint holdings(String value, String cost) {
    return new TradeProfitTimeSeriesPoint(
        Instant.EPOCH,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        0L,
        0L,
        0L,
        d(value),
        d(cost),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        LocalDate.of(2018, 10, 11));
  }

  @Test
  void 평균_단가는_원가_x_차트_종가_나누기_평가액() {
    // 분할 전 실제 44 주 x 원주가 704,000 = 평가 30,976,000, 원가 6,881,600(220 주 x 156,400 원장). 차트 종가 140,800
    // ->
    // 6,881,600 x 140,800 / 30,976,000 = 31,280 = 원가 / 실제 주식 수(156,400) 를 분할 뒤 단위(/5)로.
    assertEquals(
        0,
        d("31280")
            .compareTo(
                StockPriceChartService.averageCost(holdings("30976000", "6881600"), d("140800"))));
    assertNull(
        StockPriceChartService.averageCost(holdings("0", "0"), d("140800")), "보유가 없던 날은 선을 끊는다");
    assertNull(StockPriceChartService.averageCost(null, d("140800")));
  }

  @Test
  void 나눠_떨어지면_정수_모양() {
    assertEquals(0, StockPriceChartService.normalize(d("140800.00")).scale());
    assertEquals(2, StockPriceChartService.normalize(d("141799.59")).scale());
  }

  @Test
  void 분배금은_지급기준일_전_마지막_거래일에_싣는다() {
    // 0094M0 2026-09-15 기준일 540 원 - 9-12 · 9-13 은 주말이라 시세가 없고 분배락일은 9-14(기준일 전 마지막 거래일)
    var rows =
        java.util.List.of(
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 9, 11), d("1"), d("1"), d("1"), d("18085"), d("18085")),
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 9, 14), d("1"), d("1"), d("1"), d("16995"), d("16995")),
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 9, 15), d("1"), d("1"), d("1"), d("16770"), d("16770")));
    var payout = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    payout.setRecordDate(LocalDate.of(2026, 9, 15));
    payout.setDividendAmountPerShare(d("540"));
    var byExDate = StockPriceChartService.exDateDistributions(rows, java.util.List.of(payout));
    assertEquals(1, byExDate.size());
    assertEquals(0, d("540").compareTo(byExDate.get(LocalDate.of(2026, 9, 14))));
    // 분할 단위: 원주가 704,000 · 차트 종가 140,800 (5:1 뒤 단위) -> 주당 1,000 원은 200 원
    var row =
        new net.luversof.api.stock.domain.StockOhlcRow(
            LocalDate.of(2018, 10, 11), d("1"), d("1"), d("1"), d("140999"), d("704000"));
    assertEquals(
        0, d("200").compareTo(StockPriceChartService.distribution(d("1000"), d("140800"), row)));
    assertNull(StockPriceChartService.distribution(null, d("1"), row));
  }

  @Test
  void 시세_끝보다_뒤의_기준일은_오늘_봉에_싣지_않는다() {
    // 시세 끝 2026-10-02(금). 10-15 기준일(미리 공시)은 분배락일이 아직 없다 - 옛 규칙은 "기준일 전 마지막 시세 날" 이라 10-02 에 실었다.
    // 10-05(월) 기준일이면 10-02 가 바로 직전 평일이라 분배락일이 맞다(그 사이 평일 없음).
    var rows =
        java.util.List.of(
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 10, 1), d("1"), d("1"), d("1"), d("100"), d("100")),
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 10, 2), d("1"), d("1"), d("1"), d("100"), d("100")));
    var future = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    future.setRecordDate(LocalDate.of(2026, 10, 15));
    future.setDividendAmountPerShare(d("50"));
    var nextMonday = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    nextMonday.setRecordDate(LocalDate.of(2026, 10, 5));
    nextMonday.setDividendAmountPerShare(d("30"));
    assertEquals(
        0, StockPriceChartService.exDateDistributions(rows, java.util.List.of(future)).size());
    var byExDate = StockPriceChartService.exDateDistributions(rows, java.util.List.of(nextMonday));
    assertEquals(0, d("30").compareTo(byExDate.get(LocalDate.of(2026, 10, 2))));
  }

  @Test
  void 시세_안쪽의_빈_평일은_휴장일로_보고_싣는다() {
    // 2026-09-24(목)~09-28(월) 추석 연휴로 시세가 없고 기준일 09-29(화) - 분배락일은 연휴 전 마지막 거래일 09-23.
    var rows =
        java.util.List.of(
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 9, 23), d("1"), d("1"), d("1"), d("100"), d("100")),
            new net.luversof.api.stock.domain.StockOhlcRow(
                LocalDate.of(2026, 9, 29), d("1"), d("1"), d("1"), d("100"), d("100")));
    var payout = new net.luversof.api.stock.domain.MonthlyDividendPayout();
    payout.setRecordDate(LocalDate.of(2026, 9, 29));
    payout.setDividendAmountPerShare(d("40"));
    var byExDate = StockPriceChartService.exDateDistributions(rows, java.util.List.of(payout));
    assertEquals(0, d("40").compareTo(byExDate.get(LocalDate.of(2026, 9, 23))));
  }
}
