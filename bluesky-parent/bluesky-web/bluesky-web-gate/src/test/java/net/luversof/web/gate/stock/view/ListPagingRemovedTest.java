package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 배당·매매 상세 목록은 페이징 없이 전체를 펼친다. 그 결정 뒤에 남아 있던 페이징 배관은 도달 불가였고, 지웠다.
 *
 * <p>실측 2026-09-09: 두 목록 핸들러는 {@code size} 를 받자마자 전체 건수로 덮어써 항상 1 페이지였다. 폼과 화면 어디서도 page/size 를 보내지
 * 않았고, 손으로 {@code size=100000}, {@code page=-5} 를 보내도 응답 바이트가 같았다. 템플릿의 페이징 블록은 {@code totalPage >
 * 1} 조건이라 한 번도 그려지지 않았다. 살아 있는 코드로 오해되는 비용만 남는다.
 */
class ListPagingRemovedTest {

  private static final Path MAIN = Path.of("src/main");

  private static String read(String relative) throws IOException {
    return Files.readString(MAIN.resolve(relative), StandardCharsets.UTF_8);
  }

  @Test
  void 목록_핸들러는_page_size_를_받지_않는다() throws IOException {
    for (String controller :
        List.of(
            "java/net/luversof/web/gate/stock/controller/StockDividendHtmxController.java",
            "java/net/luversof/web/gate/stock/controller/StockTradeHtmxController.java")) {
      String source = read(controller);
      assertThat(source)
          .as(controller)
          .doesNotContain("int page,")
          .doesNotContain("int size,")
          .doesNotContain("new PageImpl<>")
          .doesNotContain("import net.luversof.web.common.menu.domain.Pagination");
    }
  }

  @Test
  void 목록_템플릿은_페이징_블록을_그리지_않는다() throws IOException {
    for (String template :
        List.of(
            "jte/stock/htmx/tradeList.jte",
            "jte/stock/htmx/fragments/tabsDividendHistory.jte",
            "jte/stock/htmx/fragments/dividend/dividendTable.jte",
            "jte/stock/htmx/fragments/trade/tradeDetailList.jte")) {
      String source = read(template);
      assertThat(source)
          .as(template)
          .doesNotContain("Pagination")
          .doesNotContain("htmxPagination")
          .doesNotContain("@param int totalPages")
          .doesNotContain("@param int currentPage");
    }
  }

  /** 페이징 버튼 클릭 뒤 스크롤을 옮기던 스크립트도 함께 갔다. 버튼이 없으니 붙을 곳이 없다. */
  @Test
  void 페이징_스크롤_훅이_없다() throws IOException {
    assertThat(read("frontend/src/stock/dividendHistory.ts")).doesNotContain("__pendingScrollTo");
  }
}
