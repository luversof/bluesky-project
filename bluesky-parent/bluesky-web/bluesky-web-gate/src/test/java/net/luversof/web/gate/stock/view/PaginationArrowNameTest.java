package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * The four arrows around the page numbers must say what they do.
 *
 * <p>Measured 2026-09-12 on the trade history pager: 4 of its 14 buttons had no letter or digit in
 * their accessible name at all - the whole name was the glyph itself. A screen reader reads those
 * as punctuation or skips them, so the only way to move by page block was invisible to it. The
 * landmark was named with the literal English word "pagination" in both languages as well.
 *
 * <p>They jump a block of ten, not one page ({@code Pagination} builds the block), so the names say
 * block. The number itself is not in the name because the block size lives in Java, not here.
 */
class PaginationArrowNameTest {

  private static final String TEMPLATE = "src/main/jte/_components/htmxPagination.jte";
  private static final String Q = String.valueOf((char) 34);
  private static final String[] KEYS = {
    "common.pagination.nav",
    "common.pagination.first",
    "common.pagination.prev.block",
    "common.pagination.next.block",
    "common.pagination.last",
  };

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

  /** Every arrow gets a name, and the glyph itself is hidden so it is not read twice. */
  @Test
  void 화살표_네_개가_모두_이름을_가진다() throws IOException {
    String jte = read(TEMPLATE);
    for (String glyph : new String[] {"&laquo;", "&lsaquo;", "&rsaquo;", "&raquo;"}) {
      assertThat(jte)
          .as(glyph + " must be hidden from the name")
          .contains("<span aria-hidden=" + Q + "true" + Q + ">" + glyph + "</span>");
      assertThat(jte)
          .as(glyph + " must not sit in the button as bare text")
          .doesNotContain(Q + ">" + glyph + "</button>");
    }
    assertThat(count(jte, "aria-label=" + Q + "${MessageUtil.getMessage("))
        .as("four arrows plus the landmark")
        .isEqualTo(5);
  }

  /** The landmark name must come from the bundle, not be a hard-coded English word. */
  @Test
  void 이정표_이름도_로케일을_탄다() throws IOException {
    String jte = read(TEMPLATE);
    assertThat(jte).doesNotContain("aria-label=" + Q + "pagination" + Q);
    assertThat(jte)
        .contains(
            "<nav class="
                + Q
                + "join"
                + Q
                + " aria-label="
                + Q
                + "${MessageUtil.getMessage("
                + Q
                + "common.pagination.nav"
                + Q
                + ")}"
                + Q
                + ">");
  }

  /** Both bundles carry every key, and the two languages differ. */
  @Test
  void 두_묶음_모두_다섯_키를_가진다() throws IOException {
    String en = read("src/main/resources/uiMessage.properties");
    String ko = read("src/main/resources/uiMessage_ko.properties");
    for (String key : KEYS) {
      assertThat(valueOf(en, key)).as("en " + key).isNotEmpty();
      assertThat(valueOf(ko, key)).as("ko " + key).isNotEmpty();
      assertThat(valueOf(ko, key)).as(key + " must be translated").isNotEqualTo(valueOf(en, key));
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
        return trimmed.substring(eq + 1).trim();
      }
    }
    return "";
  }
}
