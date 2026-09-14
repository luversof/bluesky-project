package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The asset-growth card that answers with money when the rate is meaningless.
 *
 * <p>Measured 2026-09-12 on the live screen: a period starting 2009-10-07 has an opening value of
 * 389,790 and a closing value of 1,622,109,770. The rate is suppressed there (the opening is under
 * 1% of opening + inflow), and the card printed the <b>closing value</b> under a title that says
 * "rate" in Korean. Two things were wrong at once - the unit did not match the title, and the
 * number was the level, not the increase.
 */
class AssetGrowthAmountCardTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/htmx/fragments/assetGrowthPeriodReturnSummary.jte";

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

  @Test
  void 금액으로_답할_때는_제목도_금액을_말한다() throws IOException {
    String jte = flat(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));
    assertThat(jte)
        .as("card title must switch with the unit")
        .contains("${growthShowsAmount ? growthAmountTitle : growthTitle}");
    assertThat(jte)
        .as("the amount branch is exactly the branch that is not the rate branch")
        .contains(
            "boolean growthShowsAmount = !(returnCalculable && periodReturnRatePct != null)"
                + " && amountFallbackAvailable;");
  }

  @Test
  void 값은_기말_평가액이_아니라_증가액이다() throws IOException {
    String jte = flat(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));
    assertThat(jte)
        .as("the increase, not the level")
        .contains(
            "BigDecimal growthAmountValue = amountFallbackAvailable ?"
                + " closingValue.subtract(openingValue) : null;");
    assertThat(jte)
        .as("sign goes before the currency mark, as everywhere else")
        .contains(
            "${growthAmountValue.signum() < 0 ? "
                + (char) 34
                + "-"
                + (char) 34
                + " : "
                + (char) 34
                + (char) 34
                + "}&#8361;${amountFormat.format("
                + "growthAmountValue.abs())}");
    assertThat(jte)
        .as("the start-to-end line still shows both endpoints")
        .contains(
            "${growthAmountLabel} <span class="
                + (char) 34
                + "amount-value"
                + (char) 34
                + ">&#8361;${amountFormat.format(openingValue)}</span>");
  }

  @Test
  void 두_묶음_모두에_제목이_있다() throws IOException {
    String key = "stock.asset.growth.summary.period.growth.amount.title";
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8);
      assertThat(text).as(bundle).contains(key);
    }
  }

  /** The two Korean titles must not be the same word - one says rate, the other says amount. */
  @Test
  void 한국어_제목이_비율_제목과_다르다() throws IOException {
    String ko =
        Files.readString(
            Path.of("src/main/resources/uiMessage_ko.properties"), StandardCharsets.UTF_8);
    String rate = valueOf(ko, "stock.asset.growth.summary.period.growth");
    String amount = valueOf(ko, "stock.asset.growth.summary.period.growth.amount.title");
    assertThat(amount).as("amount title must exist").isNotEmpty();
    assertThat(amount).as("must not repeat the rate title").isNotEqualTo(rate);
    assertThat(rate).as("rate title ends with the syllable for percent").contains("uC728");
    assertThat(amount).as("amount title ends with the syllable for amount").contains("uC561");
  }

  private String valueOf(String bundle, String key) {
    for (String line : bundle.split(String.valueOf((char) 10))) {
      String trimmed = line.trim();
      int eq = trimmed.indexOf((char) 61);
      if (eq < 0) {
        continue;
      }
      if (trimmed.substring(0, eq).trim().equals(key)) {
        return trimmed.substring(eq + 1).trim();
      }
    }
    return "";
  }
}
