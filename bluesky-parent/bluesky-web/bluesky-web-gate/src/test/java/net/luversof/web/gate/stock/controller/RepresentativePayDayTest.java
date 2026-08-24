package net.luversof.web.gate.stock.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.MonthlyDividendPayoutResponse;

/**
 * 월배당 "다가올 지급일"의 대표 일자 산출을 고정한다.
 *
 * <p>실제 지급일은 영업일 보정 때문에 달마다 흔들린다(실측: 중순 지급 17/19/20 일, 월말 지급 2~7 일). 예전 구현은 심볼별 <b>최신 지급 1건</b>의
 * 일자를 평균해서, 어쩌다 늦게 지급된 달이 마지막이면 예상일이 통째로 밀렸다.
 *
 * <p>지급이력으로 되짚어 본 결과(그 달 이전 자료만 써서 그 달을 맞히기, 중순 22 개월 + 월말 51 개월) 최신 1개월 방식이 두 시기 모두 가장 부정확했고, 전체
 * 최빈값이 정확히 맞히는 달이 훨씬 많았다(중순 5→12 of 22, 월말 7→14 of 51).
 */
class RepresentativePayDayTest {

  private static final Map<String, String> WINDOWS =
      Map.of("498400", "MID_MONTH", "475720", "MID_MONTH", "0104P0", "MONTH_END");

  private MonthlyDividendPayoutResponse payout(String symbol, String payDate) {
    return new MonthlyDividendPayoutResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        symbol,
        "이름",
        LocalDate.parse(payDate).minusDays(1),
        LocalDate.parse(payDate),
        null,
        BigDecimal.ONE,
        BigDecimal.ZERO,
        Instant.now());
  }

  private List<MonthlyDividendPayoutResponse> payouts(String symbol, String... payDates) {
    List<MonthlyDividendPayoutResponse> list = new ArrayList<>();
    for (String date : payDates) {
      list.add(payout(symbol, date));
    }
    return list;
  }

  @Test
  void 가장_자주_나온_일자를_쓴다() {
    var history =
        payouts("498400", "2026-03-17", "2026-04-17", "2026-05-19", "2026-06-17", "2026-07-20");

    assertEquals(
        17,
        StockSummaryHtmxController.representativePayDay(history, WINDOWS, "MID_MONTH", 15),
        "최신 한 달(20일)이 아니라 가장 흔한 17일이어야 한다");
  }

  @Test
  void 같은_횟수면_최근에_나온_일자를_쓴다() {
    var history = payouts("498400", "2026-05-19", "2026-06-17", "2026-07-19", "2026-08-17");

    // 17일 2회 / 19일 2회 — 마지막 등장은 17일(2026-08-17)이 더 최근이다.
    assertEquals(
        17, StockSummaryHtmxController.representativePayDay(history, WINDOWS, "MID_MONTH", 15));
  }

  @Test
  void 다른_시기의_지급은_섞이지_않는다() {
    var history = new ArrayList<>(payouts("498400", "2026-06-17", "2026-07-17"));
    history.addAll(payouts("0104P0", "2026-06-02", "2026-07-02", "2026-08-02"));

    assertEquals(
        17, StockSummaryHtmxController.representativePayDay(history, WINDOWS, "MID_MONTH", 15));
    assertEquals(
        2, StockSummaryHtmxController.representativePayDay(history, WINDOWS, "MONTH_END", 31));
  }

  @Test
  void 이력이_없으면_기본값이다() {
    assertEquals(
        15, StockSummaryHtmxController.representativePayDay(List.of(), WINDOWS, "MID_MONTH", 15));
    assertEquals(
        31, StockSummaryHtmxController.representativePayDay(List.of(), WINDOWS, "MONTH_END", 31));
  }
}
