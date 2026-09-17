package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.StockCashFlowResponse;

/**
 * 보유 기간 · 연평균(XIRR) · 배당 상쇄율.
 *
 * <p>세 값 모두 자산 현황 표 · 종목 상세에 그대로 찍히는 숫자다. 식을 바꾸면 화면의 숫자가 조용히 달라지므로 손으로 풀리는 흐름과 실측 자료로 고정한다. 연평균은
 * 2026-09-17 사용자 결정으로 복리 환산에서 XIRR 로 바뀌었다 &mdash; 기대값은 파이썬 이분법 구현으로 따로 계산해 맞췄다.
 */
class StockHoldingReturnUtilTest {

  private static final LocalDate TODAY = LocalDate.parse("2026-09-14");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private static StockCashFlowResponse flow(String date, String amount) {
    return new StockCashFlowResponse(LocalDate.parse(date), bd(amount));
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
    var row =
        StockHoldingReturnUtil.of(
            null, TODAY, List.of(flow("2025-09-14", "-1000000")), bd("1200000"), null, null);

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

  // ---------------------------------------------------------------- 연평균(XIRR)

  /** 1 년을 꼭 채운 +20% 는 연 20.0%. 한 번 사서 들고만 있으면 XIRR 도 복리 환산과 같다. */
  @Test
  void 일_년이면_기간_수익률_그대로다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "-1000000")), TODAY, bd("1200000")))
        .isEqualByComparingTo(bd("20.0"));
  }

  /** 117 일짜리 +20% 는 연 76.6% 다(단순 환산 x 365/117 이면 62.4%). */
  @Test
  void 짧은_보유는_복리로_펼친다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2026-05-20", "-1000000")), TODAY, bd("1200000")))
        .isEqualByComparingTo(bd("76.6"));
  }

  /**
   * XIRR 로 바꾼 까닭 &mdash; 나중에 들어간 돈은 들어간 날부터 센다. 2021 년 1,000,000 · 2022 년 1,100,000 을 넣어 2023 년에
   * 2,420,000 이면 두 돈 모두 해마다 10% 씩 불었다(1,000,000 x 1.1^2 + 1,100,000 x 1.1). 옛 복리 환산은 2,100,000 이
   * 처음부터 2 년 있었다고 봐서 7.3% 로 낮게 냈다. 실측 2026-09-17 삼성전자(17 번 분할 매수): 옛 식 23.6% vs XIRR 31.8%.
   */
  @Test
  void 나눠_산_돈은_들어간_날부터_센다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2021-01-01", "-1000000"), flow("2022-01-01", "-1100000")),
                LocalDate.parse("2023-01-01"),
                bd("2420000")))
        .isEqualByComparingTo(bd("10.0"));
  }

  /** 배당은 받은 날의 돈이다. 1,000,000 이 해마다 5% 를 벌어 1 년 뒤 50,000 을 받고 2 년 뒤 1,050,000 이면 연 5.0%. */
  @Test
  void 배당은_받은_날에_들어온_돈이다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2021-01-01", "-1000000"), flow("2022-01-01", "50000")),
                LocalDate.parse("2023-01-01"),
                bd("1050000")))
        .isEqualByComparingTo(bd("5.0"));
  }

  /** 같은 날의 흐름은 더한 것과 같다 - 오늘 받은 배당은 오늘의 평가액과 한날이다. 오늘 뒤 날짜의 기록은 아직 오간 돈이 아니다. */
  @Test
  void 같은_날은_더하고_오늘_뒤는_세지_않는다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(
                    flow("2021-01-01", "-600000"),
                    flow("2021-01-01", "-400000"),
                    flow("2022-01-01", "50000"),
                    flow("2023-01-01", "50000"),
                    flow("2023-03-01", "-999999999")),
                LocalDate.parse("2023-01-01"),
                bd("1000000")))
        .isEqualByComparingTo(bd("5.0"));
  }

  /**
   * 실측 2026-09-14 삼성전자를 한 번에 샀다고 치면(매수 362,525,078.705 · 2,385 일 뒤 평가 1,482,568,282) XIRR 은 복리 환산과
   * 같은 연 24.1% 다. 단순 환산은 47.28% 라 두 식을 바꿔 쓰면 화면 숫자가 두 배 가까이 튄다.
   */
  @Test
  void 오래_보유한_종목에서_단순_환산과_갈린다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2020-03-04", "-362525078.705")), TODAY, bd("1482568282.000")))
        .isEqualByComparingTo(bd("24.1"));
    assertThat(StockYieldUtil.annualizedPct(bd("308.96"), 2385L))
        .as("단순 환산은 같은 자료로 47.28% 다 - 두 식을 바꿔 쓰면 안 된다")
        .isEqualByComparingTo(bd("47.28"));
  }

  /** 1,000,000 을 넣어 1 년 뒤 100,000 이 남았으면 연 -90.0%. 손실도 그대로 적는다. */
  @Test
  void 손실도_날짜대로_펼친다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "-1000000")), TODAY, bd("100000")))
        .isEqualByComparingTo(bd("-90.0"));
  }

  /** 나간 돈만 있으면(평가액 0 · 받은 것 없음) 근이 없다. 억지로 -100% 를 적으면 "딱 전액 손실" 로 읽힌다. 들어온 돈만 있어도 마찬가지다. */
  @Test
  void 나간_돈이나_들어온_돈만_있으면_연평균이_없다() {
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "-1000000")), TODAY, BigDecimal.ZERO))
        .isNull();
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "-1000000")), TODAY, null))
        .isNull();
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "50000")), TODAY, bd("1000")))
        .isNull();
  }

  @Test
  void 흐름이나_기준일이_없으면_연평균이_없다() {
    assertThat(StockHoldingReturnUtil.annualizedPct(null, TODAY, bd("1200000"))).isNull();
    assertThat(StockHoldingReturnUtil.annualizedPct(List.of(), TODAY, bd("1200000"))).isNull();
    assertThat(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "-1000000")), null, bd("1200000")))
        .isNull();
  }

  /** 기간을 모르면(최초 매수일 없음) 흐름이 있어도 연평균을 적지 않는다 - 기간 칸과 연평균 칸이 서로 다른 말을 하면 안 된다. */
  @Test
  void 기간이_없으면_흐름이_있어도_연평균이_없다() {
    var row =
        StockHoldingReturnUtil.of(
            LocalDate.parse("2026-09-14"),
            TODAY,
            List.of(flow("2025-09-14", "-1000000")),
            bd("1200000"),
            null,
            null);

    assertThat(row.hasPeriod()).isFalse();
    assertThat(row.annualizedPct()).isNull();
  }

  /** 1 년 미만이면 화면이 약하게 표시한다. 경계(365 일)는 확대가 아니다. */
  @Test
  void 보유_일_년_미만만_짧은_보유다() {
    assertThat(
            StockHoldingReturnUtil.of(
                    LocalDate.parse("2026-05-20"), TODAY, List.of(), bd("1200000"), null, null)
                .shortTerm())
        .isTrue();
    assertThat(
            StockHoldingReturnUtil.of(
                    LocalDate.parse("2025-09-14"), TODAY, List.of(), bd("1200000"), null, null)
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

  // ---------------------------------------------------------------- 계좌: 여러 종목의 흐름

  /**
   * 계좌 카드의 연평균은 종목별로 온 흐름을 합쳐 푼다(사용자 선택 2026-09-17). 같은 날 두 종목을 산 계좌는 그날 한 번 낸 것과 같아야 한다 &mdash; 두
   * 종목을 100 씩 사서 1 년 뒤 220 이면 10%.
   */
  @Test
  void 계좌는_종목_흐름을_합쳐_한_번에_푼다() {
    Map<UUID, List<StockCashFlowResponse>> byItem = new LinkedHashMap<>();
    byItem.put(UUID.randomUUID(), List.of(flow("2025-09-14", "-100")));
    byItem.put(UUID.randomUUID(), List.of(flow("2025-09-14", "-100")));

    List<StockCashFlowResponse> combined = StockHoldingReturnUtil.combine(byItem);

    assertThat(combined).hasSize(2);
    assertThat(StockHoldingReturnUtil.annualizedPct(combined, TODAY, bd("220")))
        .isEqualByComparingTo("10.0")
        .isEqualByComparingTo(
            StockHoldingReturnUtil.annualizedPct(
                List.of(flow("2025-09-14", "-200")), TODAY, bd("220")));
  }

  /** 합친 목록은 날짜순이고, 비어 있는 종목 · 날짜나 금액이 없는 항목은 뺀다. 계좌의 기간은 가장 이른 흐름(= 최초 매수)부터다. */
  @Test
  void 합친_흐름은_날짜순이고_빈_항목을_뺀다() {
    Map<UUID, List<StockCashFlowResponse>> byItem = new LinkedHashMap<>();
    byItem.put(UUID.randomUUID(), List.of(flow("2026-03-02", "-300"), flow("2026-06-30", "12")));
    byItem.put(UUID.randomUUID(), null);
    byItem.put(
        UUID.randomUUID(),
        Arrays.asList(
            flow("2025-11-18", "-500"),
            null,
            new StockCashFlowResponse(null, bd("1")),
            new StockCashFlowResponse(LocalDate.parse("2026-01-05"), null)));

    List<StockCashFlowResponse> combined = StockHoldingReturnUtil.combine(byItem);

    assertThat(combined)
        .extracting(StockCashFlowResponse::date)
        .containsExactly(
            LocalDate.parse("2025-11-18"),
            LocalDate.parse("2026-03-02"),
            LocalDate.parse("2026-06-30"));
    assertThat(StockHoldingReturnUtil.firstDate(combined)).isEqualTo(LocalDate.parse("2025-11-18"));
    assertThat(
            StockHoldingReturnUtil.firstDate(
                List.of(flow("2026-06-30", "12"), flow("2025-11-18", "-500"))))
        .as("순서와 무관하게 가장 이른 날")
        .isEqualTo(LocalDate.parse("2025-11-18"));
  }

  @Test
  void 흐름이_없으면_합칠_것도_시작일도_없다() {
    assertThat(StockHoldingReturnUtil.combine(null)).isEmpty();
    assertThat(StockHoldingReturnUtil.combine(Map.of())).isEmpty();
    assertThat(StockHoldingReturnUtil.firstDate(List.of())).isNull();
    assertThat(StockHoldingReturnUtil.firstDate(null)).isNull();
  }
}
