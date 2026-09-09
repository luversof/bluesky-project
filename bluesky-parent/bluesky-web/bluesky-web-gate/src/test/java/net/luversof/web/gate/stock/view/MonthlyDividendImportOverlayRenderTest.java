package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 관리 &gt; 월배당 기준의 가져오기 폼에 붙는 로딩 오버레이.
 *
 * <p>왜 필요한가(실측 2026-09-08): 출처 일괄 가져오기는 대상 프로필마다 외부 ETF 사이트를 차례로 내려받는다. 한 건(riseetf.co.kr)이 8.65 초
 * 걸렸고 월 중 대상 프로필이 4 개다. 그동안 화면은 아무 표시도 하지 않아 멈춘 것처럼 보였고, 다시 누르면 같은 가져오기가 한 번 더 돌았다.
 *
 * <p>이 화면의 가져오기는 htmx 가 아니라 평범한 POST 폼이라 {@code hx-indicator} 가 붙지 않는다. 그래서 폼마다 표시를 달고 스크립트가 그것을 보고
 * 오버레이를 띄운다 &mdash; 표시가 빠지면 아무 일도 일어나지 않으므로 여기서 지킨다.
 */
class MonthlyDividendImportOverlayRenderTest {

  private static final String TEMPLATE = "stock/fragments/monthlyDividendReference.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private String render() {
    return render(TEMPLATE, new HashMap<>());
  }

  private String render(String template, Map<String, Object> model) {
    // 출처 단건 가져오기는 저장된 프로필과 출처 URL 이 있을 때만 나온다.
    model.put("monthlyDividendProfileExists", true);
    // 지급 이력 삭제 폼은 저장된 이력이 있을 때만 나온다 - 덮지 않아야 할 폼의 본보기다.
    model.put("monthlyDividendPayoutExists", true);
    model.put("selectedMonthlyDividendSymbol", "476800");
    model.put(
        "selectedMonthlyDividendSourceUrl", "https://www.samsungfund.com/etf/product/view.do");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(template, model, output);
    return output.toString();
  }

  /** action 으로 폼 여는 태그 하나를 집어낸다. */
  private String formTag(String html, String action) {
    String needle = "<form method=\"post\" action=\"" + action + "\"";
    int start = html.indexOf(needle);
    assertThat(start).as("폼을 찾지 못했다: %s", action).isNotNegative();
    int end = html.indexOf('>', start);
    assertThat(end).isNotNegative();
    return html.substring(start, end + 1);
  }

  @Test
  void 오버레이_한_벌이_화면에_있다() {
    String html = render();

    assertThat(html)
        .contains("data-submit-overlay-panel=\"true\"")
        .contains("data-submit-overlay-heading=\"true\"")
        .contains("data-submit-overlay-note=\"true\"")
        .as("처음에는 숨어 있어야 한다")
        .contains("data-submit-overlay-panel=\"true\" class=\"hidden ");
    assertThat(html).as("도는 동안 도는 표시가 있어야 한다").contains("loading loading-spinner");
  }

  /** 어느 가져오기가 도는지 알아야 하므로 폼마다 제 이름을 단다. */
  @Test
  void 일괄_가져오기_두_폼이_각각_제_이름을_단다() {
    String html = render();

    String midMonth = "월 중 배당 일괄 가져오기";
    String monthEnd = "월말 배당 일괄 가져오기";
    assertThat(html).contains("data-submit-overlay-title=\"" + midMonth + "\"");
    assertThat(html).contains("data-submit-overlay-title=\"" + monthEnd + "\"");
  }

  /** 출처에서 받아 오는 폼들은 왜 오래 걸리는지 함께 알린다. */
  @Test
  void 출처를_내려받는_폼은_오래_걸린다는_안내를_단다() {
    String html = render();
    String sourceNote = "출처 사이트에서 내려받는 중이라 시간이 걸릴 수 있습니다.";

    String singleImport = formTag(html, "/stock/dividend/monthly-reference/payout/import/source");
    assertThat(singleImport)
        .contains("data-submit-overlay=\"true\"")
        .contains("data-submit-overlay-desc=\"" + sourceNote + "\"");
    // 일괄 두 건까지 세 곳이다.
    assertThat(html.split(java.util.regex.Pattern.quote(sourceNote), -1)).hasSize(3 + 1);
  }

  /** 붙여넣기는 외부를 부르지 않으므로 출처 안내를 달면 거짓말이 된다. */
  @Test
  void 붙여넣기_폼은_출처_안내를_달지_않는다() {
    String pasteForm = formTag(render(), "/stock/dividend/monthly-reference/payout/import");

    assertThat(pasteForm)
        .contains("data-submit-overlay=\"true\"")
        .contains("data-submit-overlay-desc=\"잠시만 기다려주세요.\"")
        .doesNotContain("출처 사이트에서");
  }

  /** 저장/삭제처럼 금방 끝나는 폼까지 덮으면 화면이 깜빡이기만 한다. */
  @Test
  void 가져오기가_아닌_폼은_덮지_않는다() {
    String html = render();

    assertThat(formTag(html, "/stock/dividend/monthly-reference/profile"))
        .doesNotContain("data-submit-overlay");
    assertThat(formTag(html, "/stock/dividend/monthly-reference/payout/delete"))
        .doesNotContain("data-submit-overlay");
  }

  /**
   * 시뮬레이터의 '시트에서 보유/평단가 가져오기'도 같은 부류다 - 구글 시트를 읽어 저장하는 평범한 POST 폼. 패널은 공용 컴포넌트({@code
   * _components/ui/submitOverlay})라 두 화면이 같은 마크업을 쓴다.
   */
  @Test
  void 시뮬레이터_시트_가져오기도_같은_오버레이를_쓴다() {
    String html = render("stock/fragments/monthlyDividendSimulator.jte", new HashMap<>());

    assertThat(html).contains("data-submit-overlay-panel=\"true\"");
    String sheetImport = formTag(html, "/stock/simulator/monthly-dividend/import-sheet");
    assertThat(sheetImport)
        .contains("data-submit-overlay=\"true\"")
        .contains("data-submit-overlay-title=\"배당주 검색 시트에서 보유/평단가 가져오기\"")
        .contains("data-submit-overlay-desc=\"구글 시트에서 읽어 오는 중이라 시간이 걸릴 수 있습니다.\"");
    // 시뮬레이터 폼은 시뮬레이션 계산이라 금방 끝난다 - 덮지 않는다.
    assertThat(formTag(html, "/stock/simulator/monthly-dividend"))
        .doesNotContain("data-submit-overlay");
  }

  /** 시뮬레이터 화면도 스크립트를 싣는다(월배당 탭). */
  @Test
  void 시뮬레이터_화면이_스크립트를_싣는다() throws IOException {
    String source =
        Files.readString(Path.of("src/main/jte/stock/simulator.jte"), StandardCharsets.UTF_8);

    assertThat(source)
        .contains("<script type=\"module\" src=\"/js/stock/submitOverlay.js\"></script>");
  }

  /** 스크립트를 싣지 않으면 표시만 있고 아무 일도 일어나지 않는다. */
  @Test
  void 관리_화면이_스크립트를_싣는다() throws IOException {
    String source =
        Files.readString(Path.of("src/main/jte/stock/admin.jte"), StandardCharsets.UTF_8);

    assertThat(source)
        .contains("<script type=\"module\" src=\"/js/stock/submitOverlay.js\"></script>");
  }
}
