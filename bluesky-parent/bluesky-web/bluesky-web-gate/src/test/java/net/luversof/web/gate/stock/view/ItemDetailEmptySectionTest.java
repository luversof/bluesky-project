package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자료가 없는 구역은 사라지지 말고 그렇다고 말해야 한다.
 *
 * <p>실측 2026-09-12(종목 상세, 최근 3개월): 이 종목은 시세가 2026-04-01 에 끝나 그 기간의 점이 0 이다. 같은 화면의 매매·배당은 "이 종목의 …
 * 내역이 없습니다" 를 띄우는데 <b>주가 추이만 구역째 사라졌다</b> &mdash; 화면만 봐서는 자료가 없는 것인지 이 화면에 원래 없는 구역인지 알 수 없다.
 */
class ItemDetailEmptySectionTest {

  private static final String FRAGMENT = "src/main/jte/stock/htmx/stockItemDetailContent.jte";

  @Test
  void 주가_추이는_비어도_안내를_남긴다() throws IOException {
    String jte = flatten(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));
    assertThat(jte)
        .as("빈 경우의 가지가 있어야 한다")
        .contains(flatten("stock.item.detail.empty.price.history"));
    // 제목은 두 가지 모두에 있어야 구역이 사라지지 않는다.
    assertThat(countOccurrences(jte, flatten("stock.item.detail.price.chart.title")))
        .as("차트가 있을 때와 없을 때 모두 제목을 단다")
        .isGreaterThanOrEqualTo(2);
  }

  /** 같은 화면의 형제 구역들이 이미 지키던 규칙 - 함께 묶어 되돌아가지 않게 한다. */
  @Test
  void 형제_구역도_빈_안내를_유지한다() throws IOException {
    String jte = flatten(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));
    assertThat(jte).contains(flatten("stock.item.detail.empty.trades"));
    assertThat(jte).contains(flatten("stock.item.detail.empty.dividends"));
  }

  @Test
  void 두_번들에_문구가_다_있다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      String text =
          Files.readString(
              Path.of("src/main/resources").resolve(bundle), StandardCharsets.ISO_8859_1);
      assertThat(text).as(bundle).contains("stock.item.detail.empty.price.history");
    }
  }

  private static int countOccurrences(String source, String needle) {
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
