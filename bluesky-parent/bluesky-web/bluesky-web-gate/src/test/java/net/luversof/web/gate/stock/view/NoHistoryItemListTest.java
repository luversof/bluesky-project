package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 관리 화면의 "시세 이력이 없는 종목" 은 개수만 적혀 있었다.
 *
 * <p>같은 화면의 다른 이상 항목은 이미 종목·날짜까지 적는다 - 거래량 0 인데 종가가 바뀐 행, 하루 변동이 제한폭을 넘은 행. 이것만 숫자여서 무엇을 고쳐야 할지 알 수
 * 없었다(실측 2026-09-13: "(시세 이력이 없는 종목 4개)" - 그 4 개가 무엇인지 알아내려고 종목 86 개의 시세를 하나씩 물어봐야 했다).
 *
 * <p>고정하는 것은 셋이다. (1) 목록이 있으면 종목코드와 이름을 적는다. (2) 개수 문구는 그대로 둔다(목록은 상한이 있으므로 개수가 진실이다). (3) 목록이 비면
 * 아무것도 그리지 않는다.
 */
class NoHistoryItemListTest {

  private static final String FRAGMENT = "src/main/jte/stock/htmx/fragments/adminActions.jte";
  private static final String DTO =
      "src/main/java/net/luversof/web/gate/stock/dto/response/DataStatusResponse.java";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  @Test
  void 종목코드와_이름을_적는다() throws IOException {
    String jte = read(FRAGMENT);

    // 같은 파일의 다른 이상 목록도 row.stockItemName() 을 쓴다(5 곳) - 한 줄로 묶어 본다.
    assertThat(jte).as("한 줄에 종목코드와 이름을 함께 적는다").contains("${row.symbol()} ${row.stockItemName()}");
    assertThat(jte)
        .as("이 목록을 돌린다 - 다른 목록을 돌리면 남의 줄이 나온다")
        .contains("NoHistoryItemRow row : dataStatus.priceHistoryNoHistoryItemRows()");
  }

  /** 목록이 비었는데 빈 상자를 그리면 화면에 까닭 없는 여백이 생긴다. */
  @Test
  void 목록이_비면_그리지_않는다() throws IOException {
    String jte = read(FRAGMENT);

    assertThat(jte)
        .contains(
            "@if(dataStatus.priceHistoryNoHistoryItemRows() != null &&"
                + " !dataStatus.priceHistoryNoHistoryItemRows().isEmpty())");
  }

  /** 개수 문구는 그대로다 - 목록에는 상한이 있어 개수가 진실을 말한다. */
  @Test
  void 개수_문구는_그대로_둔다() throws IOException {
    assertThat(read(FRAGMENT)).contains("dataStatus.priceHistoryNoHistoryItemCount()");
  }

  @Test
  void 응답에_목록_자리가_있다() throws IOException {
    String dto = read(DTO);

    assertThat(dto).contains("List<NoHistoryItemRow> priceHistoryNoHistoryItemRows");
    assertThat(dto).contains("public record NoHistoryItemRow(String symbol, String stockItemName)");
  }

  /** 세는 조건과 나열하는 조건이 다르면 "4개" 라고 적고 세 줄만 보여 주게 된다. */
  @Test
  void 세는_조건과_나열하는_조건이_같다() throws IOException {
    String repo =
        read(
            "../../bluesky-api/bluesky-api-stock/src/main/java/net/luversof/api/stock/repository/StockPriceHistoryRepository.java");
    int listAt = repo.indexOf("List<ItemWithoutPriceHistory> findItemsWithoutPriceHistory");
    assertThat(listAt).as("목록 질의").isGreaterThanOrEqualTo(0);

    String listQuery = repo.substring(Math.max(0, listAt - 700), listAt);
    assertThat(listQuery)
        .as("개수 질의와 같은 NOT EXISTS 조건")
        .contains(
            "WHERE NOT EXISTS (SELECT 1 FROM \"StockPriceHistory\" z WHERE z.\"stockItem_id\" = s.\"id\")");
  }
}
