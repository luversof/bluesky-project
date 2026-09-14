package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The two edge arrows of the period bar must say what they do.
 *
 * <p>Measured 2026-09-12 across the stock screens: 24 controls (6 screens x 2 locales x 2 buttons)
 * had no letter or digit anywhere in their accessible name - the whole name was the glyph. Their
 * two neighbours already carried one, because those print the word next to the arrow.
 *
 * <p>They are not a one-step move: the jump action slides the window to the first or the last date
 * that has data and keeps the window length. So the names say earliest/latest, not previous/next.
 */
class DateRangeEdgeArrowNameTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/htmx/fragments/components/dateRangeNavBar.jte";
  private static final String Q = String.valueOf((char) 34);
  private static final String EARLIEST_KEY = "stock.range.jump.earliest";
  private static final String LATEST_KEY = "stock.range.jump.latest";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
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

  /** Each jump button carries its own name, and the glyph is hidden so it is not read as one. */
  @Test
  void 양_끝_화살표가_이름을_가진다() throws IOException {
    String jte = read(TEMPLATE);
    String first = String.valueOf((char) 171);
    String last = String.valueOf((char) 187);
    assertThat(jte)
        .as("earliest button")
        .contains(
            "aria-label="
                + Q
                + "${jumpEarliestLabel}"
                + Q
                + " data-picker="
                + Q
                + "${pickerName}"
                + Q
                + " data-picker-action="
                + Q
                + "jump"
                + Q
                + " data-picker-arg="
                + Q
                + "start"
                + Q
                + ">"
                + "<span aria-hidden="
                + Q
                + "true"
                + Q
                + ">"
                + first
                + "</span></button>");
    assertThat(jte)
        .as("latest button")
        .contains(
            "aria-label="
                + Q
                + "${jumpLatestLabel}"
                + Q
                + " data-picker="
                + Q
                + "${pickerName}"
                + Q
                + " data-picker-action="
                + Q
                + "jump"
                + Q
                + " data-picker-arg="
                + Q
                + "end"
                + Q
                + ">"
                + "<span aria-hidden="
                + Q
                + "true"
                + Q
                + ">"
                + last
                + "</span></button>");
    assertThat(jte).as("no bare glyph left in a button").doesNotContain(">" + first + "</button>");
    assertThat(jte).as("no bare glyph left in a button").doesNotContain(">" + last + "</button>");
  }

  /** The one-step buttons keep their own wording - the jump must not borrow it. */
  @Test
  void 한_칸_이동_이름을_빌려_쓰지_않는다() throws IOException {
    String jte = read(TEMPLATE);
    assertThat(count(jte, "aria-label=" + Q + "${previousLabel}" + Q))
        .as("previous belongs to the one-step button only")
        .isEqualTo(1);
    assertThat(count(jte, "aria-label=" + Q + "${nextLabel}" + Q))
        .as("next belongs to the one-step button only")
        .isEqualTo(1);
  }

  @Test
  void 두_묶음_모두_두_키를_가진다() throws IOException {
    String en = read("src/main/resources/uiMessage.properties");
    String ko = read("src/main/resources/uiMessage_ko.properties");
    for (String key : new String[] {EARLIEST_KEY, LATEST_KEY}) {
      assertThat(valueOf(en, key)).as("en " + key).isNotEmpty();
      assertThat(valueOf(ko, key)).as("ko " + key).isNotEmpty();
      assertThat(valueOf(ko, key)).as(key + " must be translated").isNotEqualTo(valueOf(en, key));
    }
    assertThat(valueOf(en, EARLIEST_KEY))
        .as("the two ends must not share one name")
        .isNotEqualTo(valueOf(en, LATEST_KEY));
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
