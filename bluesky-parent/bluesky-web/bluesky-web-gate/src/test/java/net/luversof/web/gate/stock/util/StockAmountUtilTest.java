package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 표의 합계를 내는 공용 규칙. 화면 여러 곳이 이 넷을 함께 쓴다(실측 2026-09-13: 호출 28 곳 - {@code sum} 22 · {@code count} 3 ·
 * {@code nz} 2 · {@code chain} 1).
 *
 * <p>테스트가 하나도 없었다. 규칙이 조용히 바뀌면 합계 줄만 틀리는데, 합계는 원래 눈으로 검산하지 않는 자리라 오래 남는다.
 *
 * <p>고정하는 것은 넷이다.
 *
 * <ol>
 *   <li>{@code sum} 은 <b>널 줄과 널 값을 0 으로</b> 읽는다 - 빼먹거나 터지지 않는다.
 *   <li>{@code count} 도 같은 규칙(널 줄 건너뛰기).
 *   <li>{@code chain} 은 수익률을 <b>곱해서</b> 잇는다({@code Π(1+r)-1}). 더하면 복리를 놓쳐 실제보다 작다.
 *   <li>{@code chain} 은 낼 수 있는 구간이 하나도 없으면 <b>null</b> - 0% 로 내면 "원금 그대로" 라는 거짓말이 된다.
 * </ol>
 */
class StockAmountUtilTest {

  private record Row(BigDecimal amount, Integer times, Double rate) {}

  private Row row(String amount, Integer times, Double rate) {
    return new Row(amount == null ? null : new BigDecimal(amount), times, rate);
  }

  @Test
  void 널을_0으로_읽는다() {
    assertThat(StockAmountUtil.nz(null)).isEqualByComparingTo("0");
    assertThat(StockAmountUtil.nz(new BigDecimal("-3"))).isEqualByComparingTo("-3");
  }

  @Test
  void 합계는_널_줄과_널_값을_0으로_읽는다() {
    List<Row> rows =
        Arrays.asList(row("1000", 1, null), null, row(null, 1, null), row("-250", 1, null));

    assertThat(StockAmountUtil.sum(rows, Row::amount)).isEqualByComparingTo("750");
    assertThat(StockAmountUtil.sum(null, Row::amount)).isEqualByComparingTo("0");
    assertThat(StockAmountUtil.sum(List.<Row>of(), Row::amount)).isEqualByComparingTo("0");
  }

  /** 소수 자리를 잃으면 합계 줄만 1 원씩 어긋난다. */
  @Test
  void 합계는_소수를_그대로_더한다() {
    List<Row> rows = List.of(row("0.5", 1, null), row("0.25", 1, null));

    assertThat(StockAmountUtil.sum(rows, Row::amount)).isEqualByComparingTo("0.75");
  }

  @Test
  void 건수도_같은_규칙이다() {
    List<Row> rows = Arrays.asList(row("0", 3, null), null, row("0", 4, null));

    assertThat(StockAmountUtil.count(rows, r -> r.times())).isEqualTo(7);
    assertThat(StockAmountUtil.count(null, (Row r) -> r.times())).isZero();
  }

  /** 더하면 복리를 놓친다 - 10% 와 10% 는 20% 가 아니라 21% 다. */
  @Test
  void 수익률은_곱해서_잇는다() {
    List<Row> rows = List.of(row(null, 1, 10.0d), row(null, 1, 10.0d));

    assertThat(StockAmountUtil.chain(rows, Row::rate))
        .isCloseTo(21.0d, org.assertj.core.data.Offset.offset(1e-9));
  }

  /** 손실 구간이 섞여도 곱셈이다 - +50% 뒤 -50% 는 0% 가 아니라 -25% 다. */
  @Test
  void 손실_구간도_곱한다() {
    List<Row> rows = List.of(row(null, 1, 50.0d), row(null, 1, -50.0d));

    assertThat(StockAmountUtil.chain(rows, Row::rate))
        .isCloseTo(-25.0d, org.assertj.core.data.Offset.offset(1e-9));
  }

  /** 자본이 없던 구간(null)은 배수 1 이라 건너뛴다 - 0% 로 세면 결과가 달라진다. */
  @Test
  void 값이_없는_구간은_건너뛴다() {
    List<Row> rows =
        Arrays.asList(row(null, 1, 10.0d), row(null, 1, null), null, row(null, 1, 10.0d));

    assertThat(StockAmountUtil.chain(rows, Row::rate))
        .isCloseTo(21.0d, org.assertj.core.data.Offset.offset(1e-9));
  }

  /** 낼 수 있는 구간이 하나도 없으면 0% 가 아니라 null 이다("원금 그대로" 라는 거짓말을 피한다). */
  @Test
  void 낼_수_있는_구간이_없으면_널이다() {
    assertThat(StockAmountUtil.chain(null, (Row r) -> r.rate())).isNull();
    assertThat(StockAmountUtil.chain(List.<Row>of(), Row::rate)).isNull();
    assertThat(StockAmountUtil.chain(Arrays.asList(row(null, 1, null), null), Row::rate)).isNull();
  }

  /** 진짜 0% 구간은 값이 있는 것이라 null 이 아니다. */
  @Test
  void 진짜_0퍼센트는_널이_아니다() {
    assertThat(StockAmountUtil.chain(List.of(row(null, 1, 0.0d)), Row::rate))
        .isCloseTo(0.0d, org.assertj.core.data.Offset.offset(1e-9));
  }
}
