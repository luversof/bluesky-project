package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.domain.StockRawClose;

/**
 * 원주가 평가(2026-10-02): 평가액 = 원주가 x 그날의 실제 주식 수. 실제 주식 수는 매매와 기업행위(분할 · 병합 · 무상증자) 배율로만 바뀐다.
 *
 * <p>숫자는 실제 시세다(보유 기간 원주가 13,785 행에서 가격제한폭 밖 변화는 4 건 - NAVER · 원티드랩 · 한화오션 2).
 */
class RawValuationTest {

  private static BigDecimal d(String value) {
    return new BigDecimal(value);
  }

  @Test
  void 분할은_단순_비율로_맞춘다() {
    // NAVER 2018-10-11 -> 10-12: 원주가 704,000 -> 142,000, 수정 종가 140,999 -> 142,000 (5:1 분할, 비
    // 4.9929)
    BigDecimal multiplier =
        TradeProfitService.shareMultiplier(d("704000"), d("142000"), d("140999"), d("142000"));
    assertEquals(0, d("5").compareTo(multiplier), multiplier.toPlainString());
  }

  @Test
  void 무상증자는_2배() {
    // 원티드랩 2021-10-07 -> 10-08 권리락: 53,400 -> 27,600, 수정 26,700 -> 27,600
    BigDecimal multiplier =
        TradeProfitService.shareMultiplier(d("53400"), d("27600"), d("26700"), d("27600"));
    assertEquals(0, d("2").compareTo(multiplier), multiplier.toPlainString());
  }

  @Test
  void 병합은_1보다_작다() {
    // 한화오션 2017-01-13 -> 01-16 (10:1 감자, 거래정지 중이라 수정 종가는 그대로)
    BigDecimal multiplier =
        TradeProfitService.shareMultiplier(d("4480"), d("44800"), d("19900"), d("19900"));
    assertEquals(0, d("0.1").compareTo(multiplier), multiplier.toPlainString());
  }

  @Test
  void 가격제한폭_안의_움직임은_기업행위가_아니다() {
    // 하한가 -30% 와 분배금 수정(수정 종가 쪽만 3% 다른 날)
    assertNull(TradeProfitService.shareMultiplier(d("10000"), d("7000"), d("9700"), d("6790")));
    assertNull(TradeProfitService.shareMultiplier(d("10000"), d("10100"), d("9700"), d("10100")));
  }

  @Test
  void 수정_종가도_같이_뛰면_배율을_정하지_않는다() {
    // 받은 때가 다른 두 구간의 경계에 분할이 걸치면 수정 종가도 5 배 뛴다 - 둘을 가를 수 없다.
    assertNull(
        TradeProfitService.shareMultiplier(d("700000"), d("140000"), d("700000"), d("140000")));
  }

  @Test
  void 원장이_분할_뒤_단위면_그날_실제_주식_수로_되돌린다() {
    // NAVER 2018-04-12 매수 220 주 @156,400 - 그날 원주가 약 782,000 · 수정 종가 156,400 -> 실제 44 주
    BigDecimal shares =
        TradeProfitService.rawShareQuantity(220, d("156400"), d("782000"), d("156400"));
    assertEquals(0, d("44").compareTo(shares.stripTrailingZeros()), shares.toPlainString());
  }

  @Test
  void 고쳐_적힌_원장의_배율은_단순_비율로_맞춘다() {
    // 실측 NAVER 2018-04-12: 원주가 757,000 · 수정 종가 151,190(분배금까지 조금 깎임, 배율 5.0069). 그대로 나누면 43.94 주 ->
    // 평단 156,179.
    // 단순 비율 5 로 맞추면 정확히 44 주.
    BigDecimal shares =
        TradeProfitService.rawShareQuantity(220, d("156400"), d("757000"), d("151190"));
    assertEquals(0, d("44").compareTo(shares.stripTrailingZeros()), shares.toPlainString());
    assertEquals(0, d("5").compareTo(TradeProfitService.snapToSimpleRatio(d("4.9929"))));
    assertEquals(
        0, d("4.45").compareTo(TradeProfitService.snapToSimpleRatio(d("4.45"))), "단순 비율에서 멀면 그대로");
  }

  @Test
  void 원장이_그날_단위면_수량_그대로() {
    // 카카오 2017-12-19 매수 221 주 @134,500 (분할 전 원주가 그대로 적힘, 수정 종가는 1/5)
    assertEquals(
        0,
        d("221")
            .compareTo(
                TradeProfitService.rawShareQuantity(221, d("134500"), d("135000"), d("27000"))));
    // 분배금 수정만 있는 ETF (배율 1.03) - 수정 종가에 더 가까워도 수량 그대로
    assertEquals(
        0,
        d("100")
            .compareTo(
                TradeProfitService.rawShareQuantity(100, d("10000"), d("10300"), d("10000"))));
  }

  @Test
  void 공모가는_수정_종가에_더_가까워도_고쳐_적힌_것이_아니다() {
    // 원티드랩 2021-08-11 상장일 공모 4 주 @35,000 - 원주가 91,000 · 수정 45,500(뒤의 1:1 무상증자). 2 주로 세면 무상증자 뒤 4 주가
    // 된다.
    assertEquals(
        0,
        d("4")
            .compareTo(TradeProfitService.rawShareQuantity(4, d("35000"), d("91000"), d("45500"))));
  }

  @Test
  void 스냅샷_합치기는_실제_주식_수와_원주가를_넘긴다() {
    // 같은 종목을 두 계좌에 - 스냅샷은 종목 단위 한 줄이라 합친다. 실제 주식 수를 빼먹으면 스냅샷이 수정 종가로 돌아가 시계열과 어긋난다.
    UUID id = UUID.randomUUID();
    var a = new TradeProfitService.WmaState();
    a.setStockItemId(id);
    a.setQuantity(d("10"));
    a.setRawShares(d("10"));
    a.setCapturedRawClose(d("27600"));
    var b = new TradeProfitService.WmaState();
    b.setStockItemId(id);
    b.setQuantity(d("5"));
    b.setRawShares(d("5"));
    b.setCapturedRawClose(d("27600"));
    var merged =
        TradeProfitService.mergeStatesByStockItem(Map.of("A:1|S:x", a, "A:2|S:x", b)).get(id);
    assertEquals(0, d("15").compareTo(merged.getRawShares()));
    assertEquals(0, d("27600").compareTo(merged.getCapturedRawClose()));
  }

  @Test
  void 원주가_행에서_꺼낸_일별_종가는_종가_범위_안의_수정_종가만() {
    // 한 번 읽은 원주가 행에서 일별 종가 맵도 만든다(2026-10-02, 두 번 읽던 조회를 하나로). 범위 밖 날 · 범위가 없는 종목은 빠져야
    // 예전 getDailyClosePricesGrouped 와 같다.
    UUID held = UUID.randomUUID();
    UUID noRange = UUID.randomUUID();
    LocalDate d1 = LocalDate.of(2026, 1, 2);
    var daily =
        TradeProfitService.dailyClosesFromRawRows(
            Map.of(
                held,
                    List.of(
                        row(held, d1, "500", "100"),
                        row(held, d1.plusDays(1), null, "101"),
                        row(held, d1.plusDays(2), "510", "102")),
                noRange, List.of(row(noRange, d1, "9", "9"))),
            Map.of(held, new LocalDate[] {d1.plusDays(1), d1.plusDays(2)}));
    assertNull(daily.get(d1), "종가 범위 앞(출력 시작 전)은 빠진다 - 원주가 범위만 첫 매매일부터");
    assertEquals(0, d("101").compareTo(daily.get(d1.plusDays(1)).get(held)), "원주가가 빈 날도 수정 종가는 있다");
    assertEquals(0, d("102").compareTo(daily.get(d1.plusDays(2)).get(held)), "값은 원주가가 아니라 수정 종가");
    assertEquals(2, daily.size());
  }

  @Test
  void 차트_가격은_원주가를_분할로만_맞춘다() {
    // NAVER 2018-10-11 -> 10-12 5:1 분할. 수정 종가는 일부 구간만 분배금까지 깎여 이어지지 않으므로 쓰지 않는다 - 앞날은 원주가 / 5.
    UUID id = UUID.randomUUID();
    var points =
        TradeProfitService.splitAdjustedCloses(
            List.of(
                row(id, LocalDate.of(2018, 10, 10), "700000", "139000"),
                row(id, LocalDate.of(2018, 10, 11), "704000", "140999"),
                row(id, LocalDate.of(2018, 10, 12), "142000", "142000"),
                row(id, LocalDate.of(2018, 10, 15), "143500", "143500")));
    assertEquals(4, points.size());
    assertEquals(0, d("140000").compareTo(points.get(0).closePrice()));
    assertEquals(0, d("140800").compareTo(points.get(1).closePrice()));
    assertEquals(0, d("142000").compareTo(points.get(2).closePrice()));
    assertEquals(0, d("143500").compareTo(points.get(3).closePrice()));
    assertEquals(0, points.get(1).closePrice().scale(), "나눠 떨어지면 정수로 - 140800.00 이 아니라");

    // 10:1 병합(배율 0.1): 앞날 4,480 -> 44,800
    UUID merged = UUID.randomUUID();
    var mergedPoints =
        TradeProfitService.splitAdjustedCloses(
            List.of(
                row(merged, LocalDate.of(2017, 1, 13), "4480", "19900"),
                row(merged, LocalDate.of(2017, 1, 16), "44800", "19900")));
    assertEquals(0, d("44800").compareTo(mergedPoints.get(0).closePrice()));
  }

  @Test
  void 원주가로_이을_수_없으면_null_이라_수정_종가로_돌아간다() {
    UUID id = UUID.randomUUID();
    assertNull(
        TradeProfitService.splitAdjustedCloses(
            List.of(
                row(id, LocalDate.of(2026, 1, 2), null, "100"),
                row(id, LocalDate.of(2026, 1, 5), "101", "101"))));
    assertNull(TradeProfitService.splitAdjustedCloses(List.of()));
  }

  private static StockRawClose row(UUID id, LocalDate day, String raw, String adjusted) {
    return new StockRawClose(id, day, raw == null ? null : d(raw), d(adjusted));
  }

  @Test
  void 첫날이_비면_원주가로_평가하지_않고_중간에_빈_날은_직전_배율로_메운다() {
    UUID complete = UUID.randomUUID();
    UUID gap = UUID.randomUUID();
    LocalDate day = LocalDate.of(2026, 1, 5);
    var valuation =
        TradeProfitService.RawValuation.of(
            Map.of(
                complete,
                    List.of(
                        row(complete, day, "100", "100"),
                        row(complete, day.plusDays(1), "101", "101")),
                gap, List.of(row(gap, day, null, "100"), row(gap, day.plusDays(1), "101", "101"))));
    assertTrue(valuation.covers(complete));
    assertFalse(valuation.covers(gap), "앞에 받은 날이 없으면 메울 배율이 없다");
    assertNull(valuation.rawClosesByDay().get(day).get(gap), "원주가로 평가하지 않는 종목은 날짜별 원주가에서도 뺀다");

    // 중간에 빈 날(받기 실패한 오늘 등): 직전 날 원주가 / 수정 종가(500 / 100 = 5) 를 그날 수정 종가에 곱한다 - 종목 전체를 되돌리지 않는다.
    UUID middle = UUID.randomUUID();
    var filled =
        TradeProfitService.RawValuation.of(
            Map.of(
                middle,
                List.of(
                    row(middle, day, "500", "100"),
                    row(middle, day.plusDays(1), null, "102"),
                    row(middle, day.plusDays(2), "505", "101"))));
    assertTrue(filled.covers(middle));
    assertEquals(0, d("510").compareTo(filled.rawClosesByDay().get(day.plusDays(1)).get(middle)));
    assertTrue(filled.shareEventsByDay().isEmpty(), "메운 날은 수정 종가와 같이 움직여 기업행위로 읽히지 않는다");
  }

  @Test
  void 무상증자_신주_입고는_배율_뒤_60일_안이다() {
    UUID id = UUID.randomUUID();
    List<StockRawClose> rows = new ArrayList<>();
    rows.add(row(id, LocalDate.of(2021, 10, 7), "53400", "26700"));
    rows.add(row(id, LocalDate.of(2021, 10, 8), "27600", "27600"));
    var valuation = TradeProfitService.RawValuation.of(Map.of(id, rows));
    assertEquals(
        0, d("2").compareTo(valuation.shareEventsByDay().get(LocalDate.of(2021, 10, 8)).get(id)));
    assertTrue(valuation.isListingOfPriorShareEvent(id, LocalDate.of(2021, 10, 28)));
    assertFalse(
        valuation.isListingOfPriorShareEvent(id, LocalDate.of(2021, 10, 7)),
        "배율 앞의 0 원 매수는 다른 일이다");
    assertFalse(valuation.isListingOfPriorShareEvent(id, LocalDate.of(2022, 1, 7)));
  }
}
