package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * 배당 화면의 필터 판정 규칙을 고정한다.
 *
 * <p>{@code null} 은 "필터 없음"(전부 통과), <b>빈 목록</b>은 "필터를 걸었는데 해당이 없음"(전부 제외)이다. 목록은 요청한 id 를 사용자가 실제로
 * 가진 것과 교집합한 결과라, 남의 계좌를 고르거나 매칭 종목이 없는 태그를 고르면 빈 목록이 된다.
 *
 * <p>이 규칙이 한 화면 안에서 갈려 있었다 &mdash; 이번 기간은 빈 목록을 "해당 없음" 으로, 전기 비교는 {@code || isEmpty()} 가 붙어 "필터
 * 없음" 으로 봤다. 그래서 해당이 없는 필터를 고르면 <b>이번 기간 0 원 vs 전기 전체 금액</b> 이 되어 증감이 통째로 잘못 나왔다.
 */
class DividendFilterRuleTest {

  private static final UUID A = UUID.randomUUID();
  private static final UUID B = UUID.randomUUID();

  @Test
  void 필터가_null이면_전부_통과한다() {
    assertThat(StockDividendHtmxController.matchesFilter(null, A)).isTrue();
    assertThat(StockDividendHtmxController.matchesFilter(null, B)).isTrue();
    assertThat(StockDividendHtmxController.matchesFilter(null, null)).isTrue();
  }

  @Test
  void 빈_목록이면_전부_제외한다() {
    assertThat(StockDividendHtmxController.matchesFilter(List.of(), A))
        .as("빈 목록을 '필터 없음' 으로 보면 필터를 걸었는데 전체가 나온다")
        .isFalse();
    assertThat(StockDividendHtmxController.matchesFilter(List.of(), null)).isFalse();
  }

  @Test
  void 목록에_있으면_통과_없으면_제외() {
    List<UUID> filter = List.of(A);
    assertThat(StockDividendHtmxController.matchesFilter(filter, A)).isTrue();
    assertThat(StockDividendHtmxController.matchesFilter(filter, B)).isFalse();
  }

  /** 값이 없는 행은 필터가 걸려 있으면 통과시키지 않는다. */
  @Test
  void id가_null인_행은_필터가_있으면_제외한다() {
    assertThat(StockDividendHtmxController.matchesFilter(List.of(A), null)).isFalse();
    List<UUID> withNull = new ArrayList<>(Arrays.asList(A, null));
    assertThat(StockDividendHtmxController.matchesFilter(withNull, null))
        .as("목록에 null 이 섞여 있어도 값 없는 행을 통과시키지 않는다")
        .isFalse();
  }

  /**
   * 이번 기간과 전기가 같은 규칙을 쓰는지 소스로 확인한다.
   *
   * <p>판정이 다시 인라인으로 흩어지면 두 구간의 기준이 또 갈린다.
   */
  @Test
  void 이번기간과_전기가_같은_판정을_쓴다() throws java.io.IOException {
    java.nio.file.Path source =
        java.nio.file.Path.of(
            "src/main/java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java");
    assertThat(source).as("파일이 옮겨졌다: " + source).exists();
    String text = java.nio.file.Files.readString(source, java.nio.charset.StandardCharsets.UTF_8);

    long uses = text.split("matchesFilter\\(", -1).length - 1;
    assertThat(uses).as("계좌/종목 x 이번기간/전기2곳 = 6 회 + 정의 1 회").isGreaterThanOrEqualTo(7);

    assertThat(text)
        .as("빈 목록을 '필터 없음' 으로 되돌리는 예외가 다시 생기면 전기 비교가 어긋난다")
        .doesNotContain("|| finalAccountIdList.isEmpty()")
        .doesNotContain("|| finalStockItemIdList.isEmpty()");
  }
}
