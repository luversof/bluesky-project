package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 매매 상세 줄은 계좌를 밝혀야 한다.
 *
 * <p>실측 2026-09-10: 자산성장 화면의 매매 이력 표에는 계좌 열이 없어, 같은 날 같은 값의 다른 계좌 거래가 화면에서 완전히 같은 줄이 됐다. 258 행 중 2
 * 쌍(2026-09-02 · 2026-07-02 PLUS 고배당주위클리고정커버드콜, 각각 수량 6 / 7)이 연금저축1 과 연금저축2 로 갈리는 별개 거래인데 구분할 길이
 * 없었다. 매매 화면의 같은 표({@code tradeDetailList.jte})는 계좌 열을 갖고 있어 둘이 어긋나 있었다.
 */
class TradeHistoryAccountColumnTest {

  private static final Path HISTORY = Path.of("src/main/jte/stock/htmx/tradeHistory.jte");

  private static final Path SIBLING =
      Path.of("src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 계좌_열이_있다() throws IOException {
    String template = read(HISTORY);

    assertThat(template).contains("@param Map<UUID, String> accountNames");
    assertThat(template).contains("${accountLabel}");
    assertThat(template)
        .as("계좌 이름을 찍지 않으면 열만 생기고 내용이 빈다")
        .contains("accountNames.getOrDefault(trade.accountId()");
  }

  @Test
  void 형제_표와_같은_반응형_규약을_쓴다() throws IOException {
    String template = read(HISTORY);
    String sibling = read(SIBLING);

    String rule = "hidden sm:table-cell";
    assertThat(sibling).as("형제 표의 규약이 바뀌었으면 이 검사를 다시 맞춰야 한다").contains(rule);

    int head = template.indexOf("${accountLabel}");
    assertThat(head).isGreaterThan(0);
    assertThat(template.substring(Math.max(0, head - 90), head)).contains(rule);
  }

  @Test
  void 빈_상태_colspan_이_열_수와_맞는다() throws IOException {
    String template = read(HISTORY);

    int headStart = template.indexOf("<thead>");
    int headEnd = template.indexOf("</thead>", headStart);
    String head = template.substring(headStart, headEnd);

    int columns = 0;
    int from = 0;
    while (true) {
      int at = head.indexOf("<th ", from);
      if (at < 0) {
        break;
      }
      columns++;
      from = at + 1;
    }

    assertThat(columns).isEqualTo(10);
    assertThat(template)
        .as("colspan 이 열 수보다 작으면 빈 상태 문구가 표 폭을 못 채운다")
        .contains("colspan=\"" + columns + "\"");
  }
}
