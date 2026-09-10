package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;

/**
 * 종목·계좌 상세의 htmx 응답은 조각 템플릿만 렌더한다(레이아웃 없이).
 *
 * <p>실측 2026-09-10(qa/fetch-fragment.cjs): htmx 요청에도 페이지 전체(레이아웃 + 조각)를 렌더하고 클라이언트가 {@code
 * hx-select} 로 조각만 골라 썼다 - 종목 상세 응답 107,201자 중 레이아웃 28,447자(27%)가 매 조회마다 버려졸다. 조각 본문을 {@code
 * stock/htmx/*Content.jte} 로 분리해 페이지는 그것을 호출하고 컨트롤러의 htmx 경로는 조각만 돌려준다.
 */
class DetailFragmentTemplateTest {

  private static String render(String template, Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(template, model, output);
    return output.toString();
  }

  @Test
  void 종목_상세_조각_템플릿은_레이아웃_없이_조각_div_로_시작한다() {
    String html = render("stock/htmx/stockItemDetailContent.jte", Map.of()).trim();
    assertThat(html).startsWith("<div id=\"stockItemDetailFragment\"");
    assertThat(html)
        .doesNotContain("<html")
        .doesNotContain("<head")
        .doesNotContain("id=\"app-config\"");
    assertThat(html).endsWith("</div>");
  }

  @Test
  void 계좌_상세_조각_템플릿은_레이아웃_없이_조각_div_로_시작한다() {
    String html = render("stock/htmx/accountDetailContent.jte", Map.of()).trim();
    assertThat(html).startsWith("<div id=\"accountDetailFragment\"");
    assertThat(html).doesNotContain("<html").doesNotContain("<head");
    assertThat(html).endsWith("</div>");
  }

  @Test
  void 페이지_템플릿은_같은_조각을_호출하고_셸은_그대로다() {
    String ready = render("stock/stockItemDetail.jte", Map.of("contentReady", true));
    assertThat(ready)
        .contains("<html")
        .contains("<div id=\"stockItemDetailFragment\" class=\"space-y-6\"");
    String shell =
        render("stock/stockItemDetail.jte", Map.of("contentReady", false, "stockItemIdParam", "x"));
    assertThat(shell)
        .contains("hx-select=\"#stockItemDetailFragment\"")
        .contains("hx-trigger=\"load\"");
    assertThat(shell).doesNotContain("class=\"space-y-6\" data-page-title");
  }

  @Test
  void 컨트롤러의_htmx_경로는_조각_템플릿을_돌려준다() throws IOException {
    String src =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"),
            StandardCharsets.UTF_8);
    for (String[] pair :
        new String[][] {
          {"stock/stockItemDetail", "stock/htmx/stockItemDetailContent"},
          {"stock/accountDetail", "stock/htmx/accountDetailContent"}
        }) {
      int shell = count(src, "return \"" + pair[0] + "\";");
      int fragment = count(src, "return \"" + pair[1] + "\";");
      assertThat(shell).as(pair[0] + " 셸(비 htmx) 반환은 하나").isEqualTo(1);
      assertThat(fragment).as(pair[1] + " htmx 반환(없는 id 포함)").isEqualTo(2);
    }
  }

  private static int count(String s, String needle) {
    Matcher m = Pattern.compile(Pattern.quote(needle)).matcher(s);
    int n = 0;
    while (m.find()) n++;
    return n;
  }
}
