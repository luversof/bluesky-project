package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.DividendResponse;

/**
 * 월별 최근 12 개월 배당 합.
 *
 * <p>월별 막대만으로는 들쭉날쭉만 보이고 추세가 안 보였다(실측 2026-09-08). 표시 기간이 '올해' 면 브라우저에 앞 11 개월이 없으므로 서버가 전체 원장으로
 * 낸다.
 */
class StockDividendTtmUtilTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  private static DividendResponse dividend(String payDateKst, String net) {
    return new DividendResponse(
        null,
        null,
        null,
        "종목",
        "DIVIDEND",
        1,
        null,
        null,
        null,
        null,
        null,
        null,
        new BigDecimal(net),
        null,
        Instant.parse(payDateKst + "T00:00:00Z").minusSeconds(9 * 3600));
  }

  /** 3 달치 배당: 1 월 100 · 2 월 200 · 4 월 400. 3 월은 배당이 없어도 달은 빠지지 않고, 12 개월 창이 굴러간다. */
  @Test
  void 달마다_그_달을_끝으로_하는_12개월_합을_낸다() {
    Map<String, BigDecimal> ttm =
        StockDividendTtmUtil.byMonth(
            List.of(
                dividend("2026-01-15", "100"),
                dividend("2026-02-15", "200"),
                dividend("2026-04-15", "400")),
            SEOUL);

    assertThat(ttm.keySet()).containsExactly("2026-01", "2026-02", "2026-03", "2026-04");
    assertThat(ttm.get("2026-01")).isEqualByComparingTo("100");
    assertThat(ttm.get("2026-02")).isEqualByComparingTo("300");
    assertThat(ttm.get("2026-03")).as("배당 없는 달도 앞 달까지의 합을 잇는다").isEqualByComparingTo("300");
    assertThat(ttm.get("2026-04")).isEqualByComparingTo("700");
  }

  /** 13 달 전 배당은 창 밖이다. 2025-01 의 100 은 2026-01 합에 들어가지 않는다(창은 2025-02 ~ 2026-01). */
  @Test
  void 열두_달_창_밖의_배당은_빠진다() {
    Map<String, BigDecimal> ttm =
        StockDividendTtmUtil.byMonth(
            List.of(
                dividend("2025-01-10", "100"),
                dividend("2025-12-10", "50"),
                dividend("2026-01-10", "7")),
            SEOUL);

    assertThat(ttm.get("2025-12")).as("2025-01 ~ 2025-12 창").isEqualByComparingTo("150");
    assertThat(ttm.get("2026-01"))
        .as("2025-02 ~ 2026-01 창 - 2025-01 은 빠진다")
        .isEqualByComparingTo("57");
  }

  /** 달의 기준은 요청 시간대의 실지급일이다. UTC 로 읽으면 하루 어긋나 달이 바뀔 수 있다(이 앱의 상습 함정). */
  @Test
  void 달은_요청_시간대의_실지급일로_나눈다() {
    // 서울 2026-02-01 00:30 = UTC 2026-01-31 15:30
    DividendResponse edge =
        new DividendResponse(
            null,
            null,
            null,
            "종목",
            "DIVIDEND",
            1,
            null,
            null,
            null,
            null,
            null,
            null,
            new BigDecimal("10"),
            null,
            Instant.parse("2026-01-31T15:30:00Z"));

    assertThat(StockDividendTtmUtil.byMonth(List.of(edge), SEOUL).keySet())
        .containsExactly("2026-02");
    assertThat(StockDividendTtmUtil.byMonth(List.of(edge), ZoneId.of("UTC")).keySet())
        .containsExactly("2026-01");
  }

  /** 실지급일 없는 배당은 차트와 같은 규칙으로 뺀다. 배당이 하나도 없으면 빈 맵. */
  @Test
  void 실지급일_없는_배당은_빼고_없으면_빈_맵이다() {
    DividendResponse noPayDate =
        new DividendResponse(
            null,
            null,
            null,
            "종목",
            "DIVIDEND",
            1,
            null,
            null,
            null,
            null,
            null,
            null,
            new BigDecimal("10"),
            Instant.parse("2026-01-10T00:00:00Z"),
            null);

    assertThat(StockDividendTtmUtil.byMonth(List.of(noPayDate), SEOUL)).isEmpty();
    assertThat(StockDividendTtmUtil.byMonth(null, SEOUL)).isEmpty();
    assertThat(StockDividendTtmUtil.toJs(Map.of())).isEqualTo("{}");
  }

  @Test
  void JS_객체_문자열은_달_순서를_지킨다() {
    Map<String, BigDecimal> ttm =
        StockDividendTtmUtil.byMonth(
            List.of(dividend("2026-02-15", "200"), dividend("2026-01-15", "100")), SEOUL);

    assertThat(StockDividendTtmUtil.toJs(ttm)).isEqualTo("{\"2026-01\":100,\"2026-02\":300}");
  }
}
