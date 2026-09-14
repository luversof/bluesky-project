package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 합계 줄의 colspan 안에 <b>좁은 폭에서 숨는 열</b>이 들어가면 안 된다.
 *
 * <p>colspan 은 고정인데 그 범위의 열이 숨으면 합계 줄만 한 칸 넓어져 값이 다른 열 밑에 붙는다. 실측 2026-09-11 375px: 매매 상세는 헤더 7 칸인데
 * 합계 줄 8 칸, 배당 상세는 헤더 6 칸인데 합계 줄 7 칸이었다. 두 표 모두 계좌 열({@code hidden sm:table-cell})이 합계 라벨의 colspan
 * 안에 있었다.
 *
 * <p>넓은 폭에서는 숫자가 맞아떨어져 보이지 않는다 &mdash; 1800px 에서는 헤더 10/11 칸과 합계 줄이 정확히 같았다.
 */
class TableTotalRowColspanTest {

  private static final Path TRADE =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte");

  private static final Path DIVIDEND =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte");

  private String footer(Path path) throws IOException {
    String template = Files.readString(path, StandardCharsets.UTF_8);
    int from = template.indexOf("<tfoot>");
    assertThat(from).as(path + " 에 tfoot 이 없다").isGreaterThan(0);
    return template.substring(from, template.indexOf("</tfoot>", from));
  }

  @Test
  void 매매_합계는_숨는_열을_품지_않는다() throws IOException {
    String foot = footer(TRADE);

    assertThat(foot)
        .as("colspan 6 은 계좌 열(sm 미만 숨김)까지 덮는다")
        .doesNotContain("colspan=\"6\"")
        .contains("colspan=\"3\"");
    assertThat(foot)
        .as("숨는 계좌 열은 헤더와 같은 클래스를 단 빈 칸으로 둬야 한다")
        .contains("<td class=\"hidden sm:table-cell\"></td>");
  }

  @Test
  void 배당_합계는_숨는_열을_품지_않는다() throws IOException {
    String foot = footer(DIVIDEND);

    assertThat(foot).as("colspan 3 은 계좌 열까지 덮는다").doesNotContain("colspan=\"3\"");
    assertThat(foot).contains("colspan=\"2\"");
    assertThat(foot).contains("<td class=\"hidden sm:table-cell\"></td>");
  }

  @Test
  void 합계_칸수가_헤더_열수와_같다() throws IOException {
    assertThat(cells(footer(TRADE))).as("매매 상세는 10 열이다").isEqualTo(10);
    assertThat(cells(footer(DIVIDEND))).as("배당 상세는 11 열이다").isEqualTo(11);
  }

  /**
   * tfoot 한 줄의 칸 수를 colspan 을 반영해 센다.
   *
   * <p>합계 줄의 첫 칸은 그 줄의 머리글이라 {@code th scope="row"} 다(2026-09-11). {@code td} 만 세면 한 칸이 빠져 정렬이 맞는데도
   * 틀렸다고 나온다 - 두 태그를 함께 센다.
   */
  private int cells(String foot) {
    int total = 0;
    int from = 0;
    while (true) {
      int td = foot.indexOf("<td", from);
      int th = foot.indexOf("<th", from);
      int at = td < 0 ? th : (th < 0 ? td : Math.min(td, th));
      if (at < 0) {
        break;
      }
      int end = foot.indexOf(">", at);
      String tag = foot.substring(at, end);
      int cs = tag.indexOf("colspan=");
      if (cs < 0) {
        total += 1;
      } else {
        int q1 = tag.indexOf('"', cs);
        int q2 = tag.indexOf('"', q1 + 1);
        total += Integer.parseInt(tag.substring(q1 + 1, q2));
      }
      from = end + 1;
    }
    return total;
  }
}
