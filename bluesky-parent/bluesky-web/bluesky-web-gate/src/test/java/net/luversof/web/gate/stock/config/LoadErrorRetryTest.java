package net.luversof.web.gate.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 조각 조회가 실패하면 오류 알림에 "다시 시도" 가 붙고, 그 버튼은 실패한 주소(쿼리 포함)를 그대로 다시 부른다.
 *
 * <p>실측 2026-09-09(api-stock 을 내리고 화면 10개): 조각마다 안내는 나왔지만 백엔드가 살아난 뒤에도 페이지 전체를 새로 고쳐야 했다. 실패한 요청의
 * 쿼리에는 hx-include 로 실려 온 기간·필터가 들어 있으므로 그대로 다시 써야 같은 화면이 돌아온다. 조회가 아닌 요청(POST 등)은 재시도 버튼을 달지 않는다.
 */
class LoadErrorRetryTest {

  private final StockHtmxErrorResolver resolver = new StockHtmxErrorResolver();

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

  private static MockHttpServletRequest htmx(String method, String uri, String query) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setRequestURI(uri);
    request.setQueryString(query);
    request.addHeader("HX-Request", "true");
    request.addHeader("Accept", "text/html");
    return request;
  }

  private static String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/loadError.jte", model, output);
    return output.toString();
  }

  @Test
  void 실패한_GET_조회는_쿼리까지_그대로_재시도_주소가_된다() {
    ModelAndView view =
        resolver.resolveException(
            htmx("GET", "/stock/htmx/dividend/list", "timeZone=Asia%2FSeoul&rangeMode=all"),
            new MockHttpServletResponse(),
            null,
            new RuntimeException("backend down"));

    assertThat(view.getViewName()).isEqualTo("stock/htmx/fragments/loadError");
    assertThat(view.getModel())
        .containsEntry("retryUrl", "/stock/htmx/dividend/list?timeZone=Asia%2FSeoul&rangeMode=all");
  }

  @Test
  void 쿼리가_없으면_주소만() {
    ModelAndView view =
        resolver.resolveException(
            htmx("GET", "/stock/htmx/summary", null),
            new MockHttpServletResponse(),
            null,
            new RuntimeException("backend down"));

    assertThat(view.getModel()).containsEntry("retryUrl", "/stock/htmx/summary");
  }

  @Test
  void 조회가_아닌_요청에는_재시도를_달지_않는다() {
    ModelAndView view =
        resolver.resolveException(
            htmx("POST", "/stock/htmx/summary", null),
            new MockHttpServletResponse(),
            null,
            new RuntimeException("backend down"));

    assertThat(view.getModel()).doesNotContainKey("retryUrl");
  }

  private static MockHttpServletRequest browser(String uri, String query) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRequestURI(uri);
    request.setQueryString(query);
    request.addHeader("Accept", "text/html");
    return request;
  }

  /** 페이지가 통째로 실패해도 같은 주소로 돌아갈 길을 준다 - 상세 화면(종목·계좌)은 데이터 없이는 껍데기도 없다. */
  @Test
  void 페이지_실패_화면도_같은_주소를_재시도로_넘긴다() {
    ModelAndView view =
        resolver.resolveException(
            browser("/stock/item", "stockItemId=019d271d-ca23-7b7e-968d-9d98b8ef4e0b"),
            new MockHttpServletResponse(),
            null,
            new RuntimeException("backend down"));

    assertThat(view.getViewName()).isEqualTo("stock/pageError");
    assertThat(view.getModel())
        .containsEntry("retryUrl", "/stock/item?stockItemId=019d271d-ca23-7b7e-968d-9d98b8ef4e0b");
  }

  /** 없는 주소는 다시 불러도 없다 - 404 화면에는 재시도를 달지 않는다. */
  @Test
  void 없는_주소의_404_화면에는_재시도가_없다() {
    ModelAndView view =
        resolver.resolveException(
            browser("/stock/no-such-page", null),
            new MockHttpServletResponse(),
            null,
            new org.springframework.web.servlet.resource.NoResourceFoundException(
                org.springframework.http.HttpMethod.GET, "/stock/no-such-page", null));

    assertThat(view.getViewName()).isEqualTo("stock/pageError");
    assertThat(view.getModel()).doesNotContainKey("retryUrl");
  }

  @Test
  void 오류_조각은_재시도_주소가_있을_때만_버튼을_그린다() {
    Map<String, Object> with = new HashMap<>();
    with.put("retryUrl", "/stock/htmx/summary?timeZone=Asia%2FSeoul");
    String html = render(with);
    assertThat(html)
        .contains("data-load-error")
        .contains("hx-get=\"/stock/htmx/summary?timeZone=Asia%2FSeoul\"")
        .contains("hx-target=\"closest [data-load-error]\"")
        .contains("hx-swap=\"outerHTML\"")
        .contains("다시 시도");

    // 안내 문구("잠시 후 다시 시도해 주세요")에도 같은 낱말이 있으므로 버튼 요소로 가른다.
    assertThat(render(new HashMap<>())).doesNotContain("hx-get=").doesNotContain("<button");
  }
}
