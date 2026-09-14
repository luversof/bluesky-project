package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Every sort key the monthly dividend screens print must be one the resolver knows.
 *
 * <p>An unknown key does not stop the request - it falls back to the display order, and that is the
 * recorded decision ({@code
 * MonthlyDividendViewSupportTest.resolveRowSort_invalidFallsBackToDisplayOrder}). The cost of that
 * decision is that a key which drops out of the allow list makes a header link do nothing at all,
 * in silence: it already happened once with {@code combined-return}, and the class under test
 * carries the story - the comparator was there, the key was not, and clicking the last column
 * simply repeated the display order.
 *
 * <p>So the fallback stays and this guard covers the hole it leaves: the printed links and the
 * accepted keys must be the same set. Measured 2026-09-12: the reference table prints 6 keys and
 * the simulator table 9, all accepted.
 */
class MonthlyDividendSortKeyReachTest {

  private static final String REFERENCE =
      "src/main/jte/stock/fragments/monthlyDividendReference.jte";
  private static final String SIMULATOR =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";

  private final MonthlyDividendViewSupport support = new MonthlyDividendViewSupport();

  @Test
  void 프로필_표가_내보내는_키는_모두_받아들여진다() throws IOException {
    Set<String> keys = keysIn(REFERENCE, "profileSort=");
    assertThat(keys).as("the reference table prints sort links").isNotEmpty();
    for (String key : keys) {
      assertThat(support.resolveProfileSort(key))
          .as("profileSort=" + key + " must not fall back to the display order")
          .isEqualTo(key);
    }
  }

  @Test
  void 시뮬레이터_표가_내보내는_키는_모두_받아들여진다() throws IOException {
    Set<String> keys = keysIn(SIMULATOR, "sort=");
    assertThat(keys).as("the simulator table prints sort links").isNotEmpty();
    for (String key : keys) {
      assertThat(support.resolveRowSort(key))
          .as("sort=" + key + " must not fall back to the display order")
          .isEqualTo(key);
    }
  }

  /**
   * The comparator has to exist too - an accepted key that hits the default sorts by something
   * else.
   */
  @Test
  void 받아들인_키는_비교기까지_닿는다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/service/MonthlyDividendViewSupport.java"),
            StandardCharsets.UTF_8);
    for (String key : keysIn(SIMULATOR, "sort=")) {
      if (key.equals("display-order") || key.equals("symbol") || key.equals("combined-return")) {
        continue; // 이 셋은 상수로 적혀 있다
      }
      assertThat(source)
          .as(key + " needs its own comparator branch in sortRows")
          .contains("case " + quoted(key) + " ->");
    }
    for (String constant :
        new String[] {"SORT_DISPLAY_ORDER", "SORT_SYMBOL", "SORT_COMBINED_RETURN"}) {
      assertThat(source).as(constant + " branch").contains("case " + constant + " ->");
    }
  }

  private String quoted(String value) {
    char q = (char) 34;
    return q + value + q;
  }

  /** Reads the values a template prints after {@code marker}, e.g. {@code sort=symbol}. */
  private Set<String> keysIn(String template, String marker) throws IOException {
    String jte = Files.readString(Path.of(template), StandardCharsets.UTF_8);
    Set<String> keys = new LinkedHashSet<>();
    int at = jte.indexOf(marker);
    while (at >= 0) {
      int from = at + marker.length();
      int to = from;
      while (to < jte.length()
          && (Character.isLetter(jte.charAt(to)) || jte.charAt(to) == (char) 45)) {
        to++;
      }
      String key = jte.substring(from, to);
      if (!key.isEmpty()) {
        keys.add(key);
      }
      at = jte.indexOf(marker, to);
    }
    return keys;
  }
}
