package net.luversof.api.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * id 묶음 → SQL 인자. 보유 기간 · 연도별 비용(목록)과 월배당 카탈로그 · 최근 종가 · 원주가 채우기(집합)가 함께 쓴다(2026-10-22). 조회 쪽은 null
 * 을 "거르지 않음" 으로 읽으므로 빈 결과가 빈 글자가 되면 안 된다.
 */
class StockIdsTest {

  private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
  private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

  @Test
  void 쉼표로_잇고_null_은_건너뛴다() {
    assertThat(StockIds.joinOrNull(Arrays.asList(A, null, B))).isEqualTo(A + "," + B);
    assertThat(StockIds.joinOrNull(new LinkedHashSet<>(List.of(B, A))))
        .as("집합은 그 차례대로")
        .isEqualTo(B + "," + A);
    // 카탈로그 · 최근 종가 · 원주가 채우기는 HashSet 을 넘긴다 - 차례는 정하지 않지만 다시 나누면 같은 원소다.
    assertThat(Set.of(StockIds.joinOrNull(new HashSet<>(List.of(A, B))).split(",")))
        .containsExactlyInAnyOrder(A.toString(), B.toString());
  }

  @Test
  void 남는_게_없으면_null() {
    assertThat(StockIds.joinOrNull(null)).isNull();
    assertThat(StockIds.joinOrNull(List.of())).isNull();
    assertThat(StockIds.joinOrNull(Arrays.asList((UUID) null)))
        .as("null 만 있으면 빈 글자가 아니라 null")
        .isNull();
  }
}
