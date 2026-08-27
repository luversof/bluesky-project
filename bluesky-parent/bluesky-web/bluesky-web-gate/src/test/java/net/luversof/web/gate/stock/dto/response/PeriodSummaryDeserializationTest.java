package net.luversof.web.gate.stock.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * api-stock 이 내려준 기간 요약 JSON 을 게이트가 <b>빠짐없이</b> 읽는지 본다.
 *
 * <p>화면이 "계산 불가" 를 그리는 경로는 둘이다 &mdash; api-stock 이 값을 못 낸 경우와, 내려줬는데 게이트가 못 읽은 경우. 둘은 화면에서 구분되지
 * 않는다. 그래서 응답 본문을 그대로 넣어 읽히는지 못박는다.
 *
 * <p>아래 JSON 은 api-stock 응답의 모양 그대로다(금액은 표본값). 필드를 추가할 때 게이트 record 에 같은 이름을 넣는 것을 잊으면 이 검사가 깨진다.
 */
class PeriodSummaryDeserializationTest {

  private static final String JSON =
      """
      {
        "openingValue": 1000000000,
        "closingValue": 1500000000,
        "growthRatePct": -12.632402,
        "timeWeightedReturnPct": -13.25571463390588,
        "periodProfit": -200000000,
        "principalDelta": 5000000,
        "unrealizedStart": 100000000,
        "unrealizedEnd": 900000000,
        "unrealizedEndPct": 144.75,
        "recoveredAmount": 0,
        "netNewProfit": -200000000,
        "maxDrawdownPct": -39.80114743091786,
        "maxDrawdownPeakDate": "2026-06-18",
        "maxDrawdownTroughDate": "2026-07-30",
        "currentDrawdownPct": -26.8,
        "periodProfitRatePct": -13.14895,
        "peakValue": 2098800125,
        "peakValueDate": "2026-06-18",
        "troughValue": 1272702805,
        "troughValueDate": "2026-05-28"
      }
      """;

  @Test
  void 기간_요약_필드를_하나도_빠뜨리지_않고_읽는다() {
    ObjectMapper mapper = JsonMapper.builder().build();

    TradeProfitTimeSeriesSummary summary =
        mapper.readValue(JSON, TradeProfitTimeSeriesSummary.class);

    assertThat(summary.growthRatePct()).as("예전부터 있던 값").isEqualTo(-12.632402d);
    assertThat(summary.periodProfitRatePct())
        .as("기간 손익률을 못 읽으면 화면이 '계산 불가' 를 그린다 - api 가 못 낸 것과 구분되지 않는다")
        .isEqualTo(-13.14895d);
    assertThat(summary.peakValue()).isEqualByComparingTo("2098800125");
    assertThat(summary.peakValueDate()).hasToString("2026-06-18");
    assertThat(summary.troughValue()).isEqualByComparingTo("1272702805");
    assertThat(summary.troughValueDate()).hasToString("2026-05-28");
  }
}
