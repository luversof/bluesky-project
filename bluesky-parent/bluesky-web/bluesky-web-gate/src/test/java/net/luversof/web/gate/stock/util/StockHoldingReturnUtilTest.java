package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

/**
 * 보유 기간 · 연평균(복리) · 배당 상쇄율.
 *
 * <p>세 값 모두 자산 현황 표에 그대로 찍히는 숫자다. 식을 바꾸면 화면의 숫자가 조용히 달라지므로 실측 자료(2026-09-14)로 고정한다.
 */
class StockHoldingReturnUtilTest {

  private static final LocalDate TODAY = LocalDate.parse("2026-09-14");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  // ---------------------------------------------------------------- 보유 기간

  @Test
  void 최초_매수일부터_오늘까지를_센다() {
    var row =
        StockHoldingReturnUtil.of(LocalDate.parse("2020-03-04"), TODAY, null, null, null, null);

    assertThat(row.holdingDays()).isEqualTo(2385L);
    assertThat(row.years()).isEqualTo(6);
    assertThat(row.months()).isEqualTo(6);
    assertThat(row.hasPeriod()).isTrue();
    assertThat(row.daysOnly()).isFalse();
  }

  @Test
  void 한_달이_안_되면_일수로만_적는다() {
    var row =
        StockHoldingReturnUtil.of(LocalDate.parse("2026-09-01"), TODAY, null, null, null, null);

    assertThat(row.holdingDays()).isEqualTo(13L);
    assertThat(row.daysOnly()).isTrue();
  }

  /** 최초 매수일을 모르면 기간을 지어내지 않는다 - 0 일은 "오늘 샀다"가 아니라 "모른다"로 쓰인다. */
  @Test
  void 최초_매수일이_없으면_기간이_없다() {
    var row = StockHoldingReturnUtil.of(null, TODAY, bd("1000000"), bd("200000"), null, null);

    assertThat(row.hasPeriod()).isFalse();
    assertThat(row.annualizedPct()).as("기간을 모르면 연평균도 낼 수 없다").isNull();
  }

  /** 시계가 어긋나 최초 매수일이 오늘보다 뒤면 음수 일수가 나온다 - 기간 없음으로 둔다. */
  @Test
  void 최초_매수일이_오늘보다_뒤면_기간이_없다() {
    var row =
        StockHoldingReturnUtil.of(LocalDate.parse("2026-09-15"), TODAY, null, null, null, null);

    assertThat(row.hasPeriod()).isFalse();
  }

  // ---------------------------------------------------------------- 연평균(복리)

  /** 1 년을 꼭 채운 +20% 는 연 20.0%. 이 줄만으로는 단순 환산과 구분되지 않는다. */
  @Test
  void 일_년이면_기간_수익률_그대로다() {
    assertThat(StockHoldingReturnUtil.annualizedPct(bd("1000000"), bd("200000"), 365L))
        .isEqualByComparingTo(bd("20.0"));
  }

  /** 117 일짜리 +20% 는 복리로 연 76.6% 다. 단순 환산(x 365/117)이면 62.4% 가 나온다. */
  @Test
  void 짧은_보유는_복리로_펼친다() {
    assertThat(StockHoldingReturnUtil.annualizedPct(bd("1000000"), bd("200000"), 117L))
        .isEqualByComparingTo(bd("76.6"));
  }

  /**
   * 실측 2026-09-14 삼성전자(화면에서 그대로 읽은 값): 매수 금액 362,525,078.705 · 합산 손익 1,120,043,203.295(+308.96%) ·
   * 2,385 일. 복리로는 연 <b>24.05%</b> 인데 단순 환산은 47.28% 다 - 23%p 차이라 두 식을 바꿔 쓰면 화면 숫자가 두 배 가까이 튄다.
   */
  @Test
  void 오래_보유한_종목에서_단순_환산과_갈린다() {
    BigDecimal compound =
        StockHoldingReturnUtil.annualizedPct(bd("362525078.705"), bd("1120043203.295"), 2385L);

    assertThat(compound).isEqualByComparingTo(bd("24.1"));
    assertThat(StockYieldUtil.annualizedPct(bd("308.96"), 2385L))
        .as("단순 환산은 같은 자료로 47.28% 다 - 두 식을 바꿔 쓰면 안 된다")
        .isEqualByComparingTo(bd("47.28"));
  }

  /** 원금보다 더 잃으면(1 + 수익률 <= 0) 복리로 환산할 수 없다. 억지로 적으면 -100% 가 "딱 전액 손실"로 읽힌다. */
  @Test
  void 원금보다_더_잃으면_연평균이_없다() {
    assertThat(StockHoldingReturnUtil.annualizedPct(bd("2000000"), bd("-6152562"), 2385L)).isNull();
    assertThat(StockHoldingReturnUtil.annualizedPct(bd("2000000"), bd("-2000000"), 365L))
        .as("딱 전액 손실도 복리 환산의 밑이 0 이라 낼 수 없다")
        .isNull();
  }

  @Test
  void 분모가_없거나_기간이_없으면_연평균이_없다() {
    assertThat(StockHoldingReturnUtil.annualizedPct(null, bd("200000"), 365L)).isNull();
    assertThat(StockHoldingReturnUtil.annualizedPct(BigDecimal.ZERO, bd("200000"), 365L)).isNull();
    assertThat(StockHoldingReturnUtil.annualizedPct(bd("1000000"), null, 365L)).isNull();
    assertThat(StockHoldingReturnUtil.annualizedPct(bd("1000000"), bd("200000"), 0L)).isNull();
  }

  /** 1 년 미만이면 화면이 약하게 표시한다. 경계(365 일)는 확대가 아니다. */
  @Test
  void 보유_일_년_미만만_짧은_보유다() {
    assertThat(
            StockHoldingReturnUtil.of(
                    LocalDate.parse("2026-05-20"), TODAY, bd("1000000"), bd("200000"), null, null)
                .shortTerm())
        .isTrue();
    assertThat(
            StockHoldingReturnUtil.of(
                    LocalDate.parse("2025-09-14"), TODAY, bd("1000000"), bd("200000"), null, null)
                .shortTerm())
        .isFalse();
  }

  // ---------------------------------------------------------------- 배당 상쇄율

  /** 실측의 한 종목: 평가 -12,444,645 에 배당 5,385,714 -> 43%. */
  @Test
  void 배당이_평가손실의_몇_퍼센트를_덮었는지_센다() {
    assertThat(StockHoldingReturnUtil.coveragePct(bd("-12444645"), bd("5385714"))).isEqualTo(43);
  }

  /** 배당이 손실을 넘으면 100 을 넘는 값이 그대로 나온다 - 화면이 "전부 상쇄"로 바꿔 적는다. */
  @Test
  void 배당이_손실보다_크면_백을_넘는다() {
    assertThat(StockHoldingReturnUtil.coveragePct(bd("-148304"), bd("9149432"))).isEqualTo(6169);
  }

  /** 평가익이면 덮을 것이 없다. 0% 가 아니라 "해당 없음"이다. */
  @Test
  void 평가손실이_없으면_상쇄율이_없다() {
    assertThat(StockHoldingReturnUtil.coveragePct(bd("1000000"), bd("5385714"))).isNull();
    assertThat(StockHoldingReturnUtil.coveragePct(BigDecimal.ZERO, bd("5385714"))).isNull();
    assertThat(StockHoldingReturnUtil.coveragePct(null, bd("5385714"))).isNull();
  }

  /** 손실은 있는데 배당이 없으면 0% 다 - 이건 "해당 없음"과 다른 말이다. */
  @Test
  void 손실만_있고_배당이_없으면_영_퍼센트다() {
    assertThat(StockHoldingReturnUtil.coveragePct(bd("-1000000"), null)).isZero();
    assertThat(StockHoldingReturnUtil.coveragePct(bd("-1000000"), BigDecimal.ZERO)).isZero();
  }
}
