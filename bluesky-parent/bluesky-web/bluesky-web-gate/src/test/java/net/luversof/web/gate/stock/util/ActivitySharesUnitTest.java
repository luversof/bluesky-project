package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The share-count unit sits differently in the two languages.
 *
 * <p>Measured 2026-09-12 with locale=en: the dashboard's recent-activity rows read "111shares",
 * "42shares", "79shares" and the item-detail quantity card read "5,043shares" - no space. Korean
 * wants none ("111주"), English wants one, so a template that hard-codes either side is wrong in one
 * language. The spacing now lives in the message ({@code {0} shares} / {@code {0}주}) and every call
 * site splits it at the placeholder, the same way {@link StockMessageSplitUtil} is used elsewhere.
 */
class ActivitySharesUnitTest {

  private static final String KEY = "stock.activity.label.shares";
  private static final String Q = String.valueOf((char) 34);
  private static final String UTIL = "net.luversof.web.gate.stock.util.StockMessageSplitUtil";
  private static final String[] TEMPLATES = {
    "src/main/jte/stock/htmx/fragments/recentActivities.jte",
    "src/main/jte/stock/htmx/fragments/activityList.jte",
    "src/main/jte/stock/htmx/stockItemDetailContent.jte",
  };

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

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

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  /** Both bundles must carry the placeholder - without it the split gives back the whole text. */
  @Test
  void 두_묶음_모두_자리표시자를_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text = read("src/main/resources/" + bundle);
      String value = valueOf(text, KEY);
      assertThat(value).as(bundle + " unit pattern").contains("{0}");
    }
  }

  /** English puts a space after the number, Korean does not. */
  @Test
  void 영어만_숫자_뒤에_공백을_둔다() throws IOException {
    String en = valueOf(read("src/main/resources/uiMessage.properties"), KEY);
    String ko = valueOf(read("src/main/resources/uiMessage_ko.properties"), KEY);
    assertThat(en).as("English").startsWith("{0} ");
    assertThat(ko).as("Korean").startsWith("{0}");
    assertThat(ko.substring(3))
        .as("Korean must not start the unit with a space")
        .doesNotStartWith(" ");
  }

  /** Every quantity that carries the unit must be wrapped by both halves of the pattern. */
  @Test
  void 수량마다_앞뒤_조각이_짝을_이룬다() throws IOException {
    for (String template : new String[] {TEMPLATES[0], TEMPLATES[1]}) {
      String jte = flat(read(template));
      assertThat(jte)
          .as(template + " must not keep the old plain label")
          .doesNotContain("${sharesLabel}");
      int opens = count(jte, "${sharesBefore}<span class=" + Q + "amount-value" + Q + ">");
      int closes = count(jte, "</span>${sharesAfter}");
      assertThat(opens).as(template + " front half on the quantity").isGreaterThan(0);
      assertThat(closes).as(template + " back half count, front was " + opens).isEqualTo(opens);
      // 단위를 손으로 띄운 자리가 남아 있으면 영어에서 두 칸이 된다.
      assertThat(jte).as(template + " hand-typed space").doesNotContain("</span> ${sharesAfter}");
    }
  }

  /** The quantity card builds one string, so both halves must be in the same expression. */
  @Test
  void 수량_카드도_앞뒤_조각을_쓴다() throws IOException {
    String jte = flat(read(TEMPLATES[2]));
    assertThat(jte)
        .contains(
            UTIL
                + ".beforePlaceholder(MessageUtil.getMessage("
                + Q
                + KEY
                + Q
                + ")) + String.format(");
    assertThat(jte)
        .contains("+ " + UTIL + ".afterPlaceholder(MessageUtil.getMessage(" + Q + KEY + Q + "))");
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
