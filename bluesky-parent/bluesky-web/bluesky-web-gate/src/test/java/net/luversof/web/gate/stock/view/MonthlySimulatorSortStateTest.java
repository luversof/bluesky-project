package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 정렬되는 열은 그 사실을 상태로 알려야 한다.
 *
 * <p>실측 2026-09-12(월배당 시뮬레이터 표, 머리 14칸 · 정렬 링크 9개): {@code sort=symbol} · {@code
 * sort=combined-return} · 기본({@code display-order}) 셋 다 <b>aria-sort 가 켜진 칸이 0 개</b>였다. 어느 열로
 * 정렬됐는지는 링크 이름에 섞여 들어간 "↑" 글자로만 알 수 있었고, 그 글자는 상태가 아니라 이름의 일부로 읽힌다. 나머지 네 칸(예상 월 배당금 · 과세 표준 비중 ·
 * 수익률 둘)은 이미 제대로 켜졌다 &mdash; 두 칸만 빠져 있었다.
 */
class MonthlySimulatorSortStateTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8);
  }

  /** 정렬 링크를 품은 머리칸은 모두 aria-sort 를 달아야 한다. */
  @Test
  void 정렬_링크가_있는_머리칸은_상태를_단다() throws IOException {
    String jte = read();
    for (String key :
        new String[] {
          "displayOrderAriaSort",
          "combinedReturnAriaSort",
          "marketYieldAriaSort",
          "onCostYieldAriaSort"
        }) {
      assertThat(jte).as(key + " 가 정의돼야 한다").contains("String " + key + " =");
      assertThat(jte).as(key + " 가 머리칸에 쓰여야 한다").contains("aria-sort=\"${" + key + "}\"");
    }
  }

  /** 한 칸에 정렬 링크가 둘이면(표시 순서 / 종목코드) 어느 쪽으로 정렬해도 그 칸이 켜져야 한다. */
  @Test
  void 링크가_둘인_칸은_두_키_모두에_반응한다() throws IOException {
    String jte = flatten(read());
    assertThat(jte)
        .contains(
            flatten(
                "String displayOrderAriaSort = (\"display-order\".equals(monthlyDividendSort) || \"symbol\".equals(monthlyDividendSort))"));
  }

  /**
   * 방향 기호는 이름이 아니라 상태다.
   *
   * <p>실측: 정렬된 링크 이름이 "종목코드 ↑" 처럼 읽혔다. 상태는 aria-sort 가 전하므로 기호는 낭독에서 뺀다.
   */
  @Test
  void 방향_기호는_낭독에서_뺀다() throws IOException {
    String jte = read();
    int total = count(jte, "${currentDirectionIndicator}");
    int hidden = count(jte, "aria-hidden=\"true\">${currentDirectionIndicator}");
    assertThat(total).as("표시 기호가 있어야 시각 사용자에게 방향이 보인다").isGreaterThan(0);
    assertThat(hidden).as("기호 " + total + " 개 중 낭독 제외된 것").isEqualTo(total);
  }

  private static int count(String source, String needle) {
    int n = 0, at = source.indexOf(needle);
    while (at >= 0) {
      n++;
      at = source.indexOf(needle, at + needle.length());
    }
    return n;
  }

  /** spotless·들여쓰기에 묶지 않는다. */
  private static String flatten(String source) {
    StringBuilder sb = new StringBuilder();
    boolean space = false;
    for (char c : source.toCharArray()) {
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && sb.length() > 0) {
        sb.append(' ');
      }
      space = false;
      sb.append(c);
    }
    return sb.toString();
  }
}
