package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;
import net.luversof.web.gate.stock.util.StockTradePeriodUtil.TradePeriod;

/**
 * 매매 내역을 달/해 단위로 쪼갠 줄.
 *
 * <p>매매 화면은 고른 기간 전체를 카드 몇 장으로 답하고 그 아래는 거래 하나하나다. 그 사이가 비어 있어 "어느 해에 얼마나 사고팔았나" 는 월별 매매 금액 차트를 눈으로
 * 훑어야만 알 수 있었다 &mdash; 차트는 모양은 주지만 수를 주지 않는다.
 */
class StockTradePeriodUtilTest {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  /** 한국 시장 개장 시각(09:00 KST)으로 만든 거래. 존을 잘못 쓰면 날짜가 하루 밀린다. */
  private static TradeResponse trade(
      String date, TradeType type, String amount, String fee, String tax, String realized) {
    return new TradeResponse(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        "테스트종목",
        type,
        1,
        bd("1000"),
        bd(fee),
        bd(tax),
        bd(amount),
        realized == null ? null : bd(realized),
        LocalDate.parse(date).atTime(9, 0).atZone(KST).toInstant());
  }

  private static TradePeriod find(List<TradePeriod> rows, String label) {
    return rows.stream().filter(r -> label.equals(r.label())).findFirst().orElseThrow();
  }

  /** 같은 해 안의 거래는 달 단위로 쪼갠다. */
  private static List<TradeResponse> shortSpan() {
    return List.of(
        trade("2026-08-27", TradeType.SELL, "3000", "30", "18", "500"),
        trade("2026-08-03", TradeType.BUY, "1000", "10", "0", null),
        trade("2026-07-15", TradeType.BUY, "2000", "20", "0", null));
  }

  @Test
  void 구간마다_매수와_매도를_따로_모은다() {
    List<TradePeriod> rows = StockTradePeriodUtil.of(shortSpan(), KST);

    TradePeriod august = find(rows, "2026-08");
    assertThat(august.count()).isEqualTo(2);
    assertThat(august.buyCount()).isEqualTo(1);
    assertThat(august.sellCount()).isEqualTo(1);
    assertThat(august.buyAmount()).isEqualByComparingTo(bd("1000"));
    assertThat(august.sellAmount()).isEqualByComparingTo(bd("3000"));
    assertThat(august.fee()).isEqualByComparingTo(bd("40"));
    assertThat(august.tax()).isEqualByComparingTo(bd("18"));
  }

  /**
   * 실현손익은 <b>매도에만</b> 붙는다.
   *
   * <p>위 요약 카드가 쓰는 규칙과 같아야 두 자리가 같은 뜻을 말한다. 매수 행에도 값이 실려 오면 두 번 세게 된다 &mdash; 실측 2026-09-03 실데이터에서
   * 매도만 더한 합 225,584,549 가 연도별 세금·비용 표의 실현손익 합과 정확히 같았다.
   */
  @Test
  void 실현손익은_매도에만_붙인다() {
    List<TradeResponse> trades = new ArrayList<>(shortSpan());
    // 매수 행에 값이 실려 와도 더하지 않는다.
    trades.add(trade("2026-08-10", TradeType.BUY, "1000", "0", "0", "999999"));

    List<TradePeriod> rows = StockTradePeriodUtil.of(trades, KST);

    assertThat(find(rows, "2026-08").realizedProfit())
        .as("매수 행의 실현손익을 더하면 요약 카드와 어긋난다")
        .isEqualByComparingTo(bd("500"));
  }

  /** 최신이 위로. 다른 성과 표와 같은 차례여야 나란히 놓고 읽을 수 있다. */
  @Test
  void 최신_구간이_위로_온다() {
    List<TradePeriod> rows = StockTradePeriodUtil.of(shortSpan(), KST);

    assertThat(rows).extracting(TradePeriod::label).containsExactly("2026-08", "2026-07");
  }

  /** 매매가 없던 구간은 줄을 내지 않는다. 여기는 매매 '내역' 이라 아무 일도 없던 구간에는 적을 내역이 없다. */
  @Test
  void 매매가_없던_구간은_줄을_내지_않는다() {
    List<TradeResponse> trades =
        List.of(
            trade("2026-08-27", TradeType.BUY, "1000", "0", "0", null),
            // 사이의 2026-06 · 2026-07 은 거래가 없다.
            trade("2026-05-02", TradeType.BUY, "2000", "0", "0", null));

    List<TradePeriod> rows = StockTradePeriodUtil.of(trades, KST);

    assertThat(rows).extracting(TradePeriod::label).containsExactly("2026-08", "2026-05");
  }

  /**
   * 3 년을 넘으면 해 단위로 묶는다.
   *
   * <p>api-stock 의 성과 쪼갬과 같은 기준이다 &mdash; 두 화면이 같은 기간에서 다른 단위를 고르면 견줄 수 없다. 실측 2026-09-03: 전 구간을 달
   * 단위로 내면 60 줄, 해 단위면 14 줄이다.
   */
  @Test
  void 삼년을_넘으면_해_단위로_묶는다() {
    List<TradeResponse> longSpan =
        List.of(
            trade("2026-08-27", TradeType.SELL, "3000", "30", "18", "500"),
            trade("2020-01-06", TradeType.BUY, "1000", "10", "0", null));

    List<TradePeriod> rows = StockTradePeriodUtil.of(longSpan, KST);

    assertThat(rows).extracting(TradePeriod::unit).containsOnly("YEAR");
    assertThat(rows).extracting(TradePeriod::label).containsExactly("2026", "2020");
  }

  /** 딱 3 년이면 아직 달 단위다. 경계에서 단위가 갈리는 자리라 못을 박아 둔다. */
  @Test
  void 삼년_이하면_달_단위로_둔다() {
    List<TradeResponse> exactly =
        List.of(
            trade("2026-08-27", TradeType.BUY, "1000", "0", "0", null),
            trade("2023-08-01", TradeType.BUY, "1000", "0", "0", null));

    assertThat(StockTradePeriodUtil.of(exactly, KST))
        .extracting(TradePeriod::unit)
        .containsOnly("MONTH");
  }

  /**
   * 넘겨받은 존으로 구간을 가른다.
   *
   * <p>적재가 거래 시각을 <b>09:00 KST</b> 로 맞춰 넣기 때문에(= 00:00 UTC) 실데이터에서는 UTC 로 읽어도 날짜가 같다 &mdash; 처음 쓴
   * 검사는 09:00 거래를 썼고, 그래서 존을 UTC 로 바꿔 놓아도 통과했다(검사가 무력했다).
   *
   * <p>그러나 <b>존을 무시해도 되는 것은 아니다</b>. 화면은 사용자 존을 넘기고, 적재 시각이 바뀌거나 다른 존을 고르면 바로 갈린다. 그래서 실제로 갈리는
   * 시각(08:00 KST = 전날 23:00 UTC)으로 넘겨받은 존을 쓰는지 못 박는다.
   */
  @Test
  void 넘겨받은_존으로_구간을_가른다() {
    // 08:00 KST 는 전날 23:00 UTC 다. 존을 무시하면 7 월로 밀린다.
    TradeResponse earlyMorning =
        new TradeResponse(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "테스트종목",
            TradeType.BUY,
            1,
            bd("1000"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            bd("1000"),
            null,
            LocalDate.parse("2026-08-01").atTime(8, 0).atZone(KST).toInstant());

    List<TradePeriod> rows =
        StockTradePeriodUtil.of(
            List.of(earlyMorning, trade("2026-07-15", TradeType.BUY, "2000", "0", "0", null)), KST);

    assertThat(find(rows, "2026-08").buyAmount())
        .as("존을 무시하면 8/1 08:00 KST 가 7/31 23:00 UTC 로 읽혀 7 월에 들어간다")
        .isEqualByComparingTo(bd("1000"));
    assertThat(find(rows, "2026-07").buyAmount()).isEqualByComparingTo(bd("2000"));
  }

  /** 빈 목록과 null 은 빈 결과다. */
  @Test
  void 자료가_없으면_줄이_없다() {
    assertThat(StockTradePeriodUtil.of(List.of(), KST)).isEmpty();
    assertThat(StockTradePeriodUtil.of(null, KST)).isEmpty();
  }

  /**
   * 합계는 여덟 값을 모두 더한 것이다.
   *
   * <p>이 줄은 화면 위 요약 카드와 <b>같아야 한다</b> &mdash; 같은 원장을 같은 규칙으로 더한 값이기 때문이다. 한쪽만 어긋나면 같은 화면의 두 자리가 다른
   * 답을 하게 된다.
   */
  @Test
  void 합계는_모든_구간을_더한_값이다() {
    var totals = StockTradePeriodUtil.total(StockTradePeriodUtil.of(shortSpan(), KST));

    assertThat(totals.count()).isEqualTo(3);
    assertThat(totals.buyCount()).isEqualTo(2);
    assertThat(totals.sellCount()).isEqualTo(1);
    assertThat(totals.buyAmount()).as("1,000 + 2,000").isEqualByComparingTo(bd("3000"));
    assertThat(totals.sellAmount()).isEqualByComparingTo(bd("3000"));
    assertThat(totals.fee()).as("30 + 10 + 20").isEqualByComparingTo(bd("60"));
    assertThat(totals.tax()).isEqualByComparingTo(bd("18"));
    assertThat(totals.realizedProfit()).isEqualByComparingTo(bd("500"));
  }

  /** 합계도 실현손익은 매도에만 붙인 값을 쓴다. 구간 줄이 이미 그렇게 세었으므로 그대로 더하면 된다. */
  @Test
  void 합계의_실현손익도_매도에만_붙는다() {
    List<TradeResponse> trades = new ArrayList<>(shortSpan());
    trades.add(trade("2026-06-10", TradeType.BUY, "1000", "0", "0", "999999"));

    var totals = StockTradePeriodUtil.total(StockTradePeriodUtil.of(trades, KST));

    assertThat(totals.realizedProfit()).isEqualByComparingTo(bd("500"));
  }

  /** 줄 순서를 타지 않는다. */
  @Test
  void 합계는_줄_순서를_타지_않는다() {
    List<TradePeriod> rows = StockTradePeriodUtil.of(shortSpan(), KST);
    List<TradePeriod> reversed = new ArrayList<>(rows);
    java.util.Collections.reverse(reversed);

    assertThat(StockTradePeriodUtil.total(reversed).buyAmount())
        .isEqualByComparingTo(StockTradePeriodUtil.total(rows).buyAmount());
  }

  /** 빈 목록과 null 은 0 으로 답한다. */
  @Test
  void 합계는_자료가_없으면_0_이다() {
    List<List<TradePeriod>> inputs = new java.util.ArrayList<>();
    inputs.add(List.of());
    inputs.add(null);
    for (List<TradePeriod> input : inputs) {
      var totals = StockTradePeriodUtil.total(input);
      assertThat(totals.count()).isZero();
      assertThat(totals.realizedProfit()).isEqualByComparingTo(BigDecimal.ZERO);
    }
  }
}
