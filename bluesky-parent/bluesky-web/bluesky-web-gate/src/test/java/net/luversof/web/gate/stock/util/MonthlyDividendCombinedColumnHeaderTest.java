package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The last column of the monthly-dividend simulator table carries two different numbers.
 *
 * <p>Measured 2026-09-12: the cell prints {@code totalReturnOnCostPct} and then {@code
 * expectedCombinedReturnPct} (api-stock computes the first as (price - cost) / cost and the second
 * as that plus the annual yield on cost - checked on one row: -13.92 + 7.32 = -6.60). The header
 * only named the second one. Its first line said "on cost", which is exactly the basis caption the
 * previous column uses, so the price return read as a qualifier rather than a name and the column
 * looked like one metric printed twice.
 */
class MonthlyDividendCombinedColumnHeaderTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";
  private static final String PRICE_RETURN_KEY =
      "stock.simulator.monthly.table.header.total.return.on.cost.line.one";
  private static final String BASIS_KEY = "stock.simulator.monthly.table.header.on.cost";
  private static final String Q = String.valueOf((char) 34);

  private String flat(String source) {
    StringBuilder out = new StringBuilder();
    boolean space = false;
    for (int i = 0; i < source.length(); i++) {
      char c = source.charAt(i);
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && out.length() > 0) {
        out.append((char) 32);
      }
      space = false;
      out.append(c);
    }
    return out.toString();
  }

  /** The basis caption and the price-return name must both be there, in that order. */
  @Test
  void 두_값이_모두_이름을_가진다() throws IOException {
    String jte = flat(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));
    String basisLine =
        "<div class="
            + Q
            + "text-[11px] font-medium leading-4 text-base-content/60"
            + Q
            + ">${onCostBasisLabel}</div>";
    String priceReturnLine =
        "<div class="
            + Q
            + "whitespace-nowrap"
            + Q
            + ">${MessageUtil.getMessage("
            + Q
            + PRICE_RETURN_KEY
            + Q
            + ")}</div>";
    int basisAt = jte.indexOf(basisLine + " " + priceReturnLine);
    assertThat(basisAt)
        .as("basis caption must sit right above the price-return name")
        .isNotNegative();
  }

  /** The price-return name must not be the same words as the basis caption. */
  @Test
  void 이름이_기준_문구와_같으면_안_된다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      String basis = valueOf(text, BASIS_KEY);
      String priceReturn = valueOf(text, PRICE_RETURN_KEY);
      assertThat(priceReturn).as(bundle + " price return name").isNotEmpty();
      assertThat(basis).as(bundle + " basis caption").isNotEmpty();
      assertThat(priceReturn)
          .as(bundle + ": the name must not repeat the basis caption")
          .isNotEqualTo(basis);
    }
  }

  private String valueOf(String bundle, String key) {
    for (String line : bundle.split(String.valueOf((char) 10))) {
      String trimmed = line.trim();
      int eq = trimmed.indexOf((char) 61);
      if (eq < 0) {
        continue;
      }
      if (trimmed.substring(0, eq).trim().equals(key)) {
        return unescape(trimmed.substring(eq + 1).trim());
      }
    }
    return "";
  }

  /**
   * Property values live in two forms in this repo - raw UTF-8 and unicode escapes. Comparing the
   * raw text would call the two forms of the same word different (that slipped through once: a
   * mutation that put the basis caption back was not caught).
   */
  private String unescape(String value) {
    StringBuilder out = new StringBuilder();
    int i = 0;
    while (i < value.length()) {
      char c = value.charAt(i);
      if (c == (char) 92 && i + 5 < value.length() && value.charAt(i + 1) == (char) 117) {
        String hex = value.substring(i + 2, i + 6);
        try {
          out.append((char) Integer.parseInt(hex, 16));
          i += 6;
          continue;
        } catch (NumberFormatException ignored) {
          // not an escape - fall through and copy the character
        }
      }
      out.append(c);
      i++;
    }
    return out.toString();
  }
}
