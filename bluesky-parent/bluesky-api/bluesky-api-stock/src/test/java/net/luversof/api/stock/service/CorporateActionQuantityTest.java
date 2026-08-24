package net.luversof.api.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

/**
 * 액면분할/병합 추정이 언제 켜지고 언제 꺼지는지 고정한다.
 *
 * <p>시계열은 거래가를 그 날 수정주가와 비교해, 비율이 정수배(±15%)면 분할/병합으로 보고 평가 수량을 환산한다. 이 판정이 잘못 켜지면 보유 수량이 조용히 몇 배로
 * 늘거나 1/N 로 줄어 차트 평가액이 통째로 틀어진다.
 *
 * <p>실측(사용자 거래 250건 전수): 환산이 켜지는 거래는 0건이고, 공모주 매수 2건이 임계값 바로 옆에 있다 — 카카오게임즈 24,000원(상장일 종가 62,400)과
 * SK바이오사이언스 65,000원(종가 169,000)은 비율 0.3846 으로 1/3(0.3333) 대비 편차 15.4% 라 15% 기준을 0.4%p 차이로 벗어난다.
 * 공모가는 시장가가 아니므로 환산 대상이 아니며, 이 경계가 흔들리면 그 매수분 수량이 1/3 로 줄어든다.
 */
class CorporateActionQuantityTest {

  private static BigDecimal ratio(String tradePrice, String referenceClose) {
    return new BigDecimal(tradePrice)
        .divide(new BigDecimal(referenceClose), 10, java.math.RoundingMode.HALF_UP);
  }

  @Test
  void 공모주_매수는_병합으로_보지_않는다() {
    // 카카오게임즈 2020-09-10 (공모가 24,000 / 상장일 종가 62,400)
    assertNull(TradeProfitService.detectLikelyCorporateActionFactor(ratio("24000", "62400")));
    // SK바이오사이언스 2021-03-18 (공모가 65,000 / 상장일 종가 169,000)
    assertNull(TradeProfitService.detectLikelyCorporateActionFactor(ratio("65000", "169000")));

    // 수량도 그대로여야 한다.
    assertEquals(
        0,
        new BigDecimal("7")
            .compareTo(
                TradeProfitService.resolveEvaluationQuantity(
                    7, new BigDecimal("24000"), new BigDecimal("62400"))));
  }

  @Test
  void 보통_거래는_환산하지_않는다() {
    // 종가와 몇 % 차이 나는 평범한 체결
    assertNull(TradeProfitService.detectLikelyCorporateActionFactor(ratio("71500", "71000")));
    assertNull(TradeProfitService.detectLikelyCorporateActionFactor(ratio("4085", "4210")));
    // 1.5 배는 정수배가 아니라 대상이 아니다.
    assertNull(TradeProfitService.detectLikelyCorporateActionFactor(ratio("150000", "100000")));
  }

  @Test
  void 액면분할_전_거래는_배수로_환산한다() {
    // 삼성전자 50:1 분할 전 체결가 2,650,000 / 분할 반영 종가 53,000 -> 50 배
    BigDecimal factor =
        TradeProfitService.detectLikelyCorporateActionFactor(ratio("2650000", "53000"));
    assertEquals(0, new BigDecimal("50").compareTo(factor));
    assertEquals(
        0,
        new BigDecimal("500")
            .compareTo(
                TradeProfitService.resolveEvaluationQuantity(
                    10, new BigDecimal("2650000"), new BigDecimal("53000"))));

    // 계수는 '가장 가까운 정수'로 스냅한다. 체결가가 종가 대비 -8% 면 비율이 46.0 이라 50 이 아니라 46 이 된다
    // (허용 오차 15% 는 반올림한 정수와의 거리를 보므로, 큰 배수에서는 사실상 항상 통과한다).
    assertEquals(
        0,
        new BigDecimal("46")
            .compareTo(
                TradeProfitService.detectLikelyCorporateActionFactor(ratio("2438000", "53000"))));
  }

  @Test
  void 액면병합_전_거래는_분수로_환산한다() {
    // 5:1 병합 전 체결가 1,000 / 병합 반영 종가 5,000 -> 1/5
    BigDecimal factor = TradeProfitService.detectLikelyCorporateActionFactor(ratio("1000", "5000"));
    assertEquals(0, new BigDecimal("0.2").compareTo(factor));
    assertEquals(
        0,
        new BigDecimal("20")
            .compareTo(
                TradeProfitService.resolveEvaluationQuantity(
                    100, new BigDecimal("1000"), new BigDecimal("5000"))));
  }

  @Test
  void 값이_없거나_0이면_원수량을_그대로_쓴다() {
    assertEquals(
        0,
        new BigDecimal("10")
            .compareTo(
                TradeProfitService.resolveEvaluationQuantity(10, null, new BigDecimal("100"))));
    assertEquals(
        0,
        new BigDecimal("10")
            .compareTo(
                TradeProfitService.resolveEvaluationQuantity(
                    10, new BigDecimal("100"), BigDecimal.ZERO)));
    assertNull(TradeProfitService.detectLikelyCorporateActionFactor(BigDecimal.ZERO));
  }
}
