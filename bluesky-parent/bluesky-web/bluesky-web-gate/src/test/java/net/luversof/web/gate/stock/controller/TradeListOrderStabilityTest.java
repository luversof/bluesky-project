package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.constant.TradeType;
import net.luversof.web.gate.stock.dto.response.TradeResponse;

/**
 * 매매 목록의 행 순서가 들어온 순서에 휘둘리지 않는지 본다.
 *
 * <p>{@code /api/trade} 는 <b>ORDER BY 가 없다</b> &mdash; {@code TradeQuery} 에 "붙이지 않은 것은 의도적이다"라고 적혀
 * 있다. 그래서 응답 순서는 저장 순서일 뿐이고, 한 건만 고쳐도 그 행이 뒤로 밀린다. 목록은 {@code List.sort}(안정 정렬)로 한 열만 보고 정렬하므로 동점
 * 행의 순서가 그 저장 순서를 그대로 물려받는다 &mdash; 편집 한 번에 화면 순서가 바뀐다.
 *
 * <p>실측 2026-08-24: 거래 250 건 중 <b>155 건(62.0%)</b> 이 같은 날짜에 다른 거래와 묶여 있다(한 날 최대 7 건: 2025-10-22,
 * 2022-04-22). 컬럼 정렬은 더 심하다 &mdash; 수수료 동점 147 건, 실현손익 동점 196 건(매수는 실현손익이 0 이라 전부 묶인다).
 *
 * <p>종목명 &rarr; 매수/매도 &rarr; id 로 끊으면 어떤 두 행도 같지 않아 순서가 완전히 정해진다.
 */
class TradeListOrderStabilityTest {

  private static TradeResponse trade(String date, String name, TradeType type, String id) {
    return new TradeResponse(
        UUID.fromString(id),
        UUID.fromString("00000000-0000-0000-0000-0000000000aa"),
        UUID.fromString("00000000-0000-0000-0000-0000000000bb"),
        name,
        type,
        10,
        new BigDecimal("1000"),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        new BigDecimal("10000"),
        BigDecimal.ZERO,
        Instant.parse(date + "T00:00:00Z"));
  }

  /** 화면의 기본 정렬: 날짜 내림차순 + 동점 끊기. */
  private List<String> sortedNames(List<TradeResponse> incoming) {
    List<TradeResponse> rows = new ArrayList<>(incoming);
    rows.sort(
        Comparator.comparing(
                TradeResponse::tradeDate, Comparator.nullsLast(Comparator.reverseOrder()))
            .thenComparing(StockTradeHtmxController.TRADE_TIE_BREAKER));
    return rows.stream().map(r -> r.stockItemName() + "/" + r.id()).toList();
  }

  @Test
  void 같은_날짜_행의_순서가_들어온_순서에_휘둘리지_않는다() {
    List<TradeResponse> rows =
        List.of(
            trade("2025-10-22", "가종목", TradeType.BUY, "00000000-0000-0000-0000-000000000001"),
            trade("2025-10-22", "가종목", TradeType.SELL, "00000000-0000-0000-0000-000000000002"),
            trade("2025-10-22", "나종목", TradeType.BUY, "00000000-0000-0000-0000-000000000003"),
            trade("2025-10-21", "가종목", TradeType.BUY, "00000000-0000-0000-0000-000000000004"));

    List<String> expected = sortedNames(rows);

    // 저장 순서가 어떻게 바뀌어도 같은 결과여야 한다. 한 건을 고치면 그 행이 힙 끝으로 밀리는 것과 같다.
    List<TradeResponse> shuffled = new ArrayList<>(rows);
    Collections.reverse(shuffled);
    assertThat(sortedNames(shuffled)).as("들어온 순서를 뒤집었더니 화면 순서가 달라졌다").isEqualTo(expected);

    List<TradeResponse> rotated = new ArrayList<>(rows);
    Collections.rotate(rotated, 2);
    assertThat(sortedNames(rotated)).as("들어온 순서를 돌렸더니 화면 순서가 달라졌다").isEqualTo(expected);

    // 결과가 실제로 뜻이 있는 순서인지도 본다(최신 날짜 먼저, 같은 날은 종목명·매수/매도 순).
    assertThat(expected.get(3))
        .as("가장 오래된 거래가 맨 뒤여야 한다")
        .startsWith("가종목/00000000-0000-0000-0000-000000000004");
    assertThat(expected.subList(0, 3))
        .containsExactly(
            "가종목/00000000-0000-0000-0000-000000000001",
            "가종목/00000000-0000-0000-0000-000000000002",
            "나종목/00000000-0000-0000-0000-000000000003");
  }

  @Test
  void 모든_열이_같은_행도_id_로_갈린다() {
    // 실현손익 동점 196 건처럼, 표시값이 전부 같은 행이 실제로 있다.
    List<TradeResponse> rows =
        List.of(
            trade("2026-08-19", "같은종목", TradeType.BUY, "00000000-0000-0000-0000-00000000000b"),
            trade("2026-08-19", "같은종목", TradeType.BUY, "00000000-0000-0000-0000-00000000000a"));

    assertThat(sortedNames(rows))
        .as("id 까지 가지 않으면 두 행의 순서가 정해지지 않는다")
        .isEqualTo(sortedNames(List.of(rows.get(1), rows.get(0))));
    assertThat(sortedNames(rows).get(0)).endsWith("0000000a");
  }
}
