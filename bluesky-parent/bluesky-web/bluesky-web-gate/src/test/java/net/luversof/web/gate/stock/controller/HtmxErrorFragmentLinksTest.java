package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 컨트롤러가 직접 돌려주는 오류 조각({@code stock/htmx/error})에도 다음 행동이 붙는다: 세션이 풀렸으면 로그인 링크, 백엔드 조회가 실패했으면 재시도
 * 버튼.
 *
 * <p>실측 2026-09-09(익명으로 조각 27개 호출): 전부 "로그인이 필요합니다" 한 줄(337B)만 나왔고 로그인으로 가는 길이 없었다. 공통 예외 처리기의
 * loadError 조각에는 재시도가 붙는데, 컨트롤러가 {@code remoteFailureView} 로 직접 그리는 실패 안내에는 없어 같은 상황에 화면이 달랐다.
 */
class HtmxErrorFragmentLinksTest {

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

  @AfterEach
  void clearRequest() {
    RequestContextHolder.resetRequestAttributes();
  }

  private static StockBaseHtmxController controller() {
    StaticMessageSource messages = new StaticMessageSource();
    messages.setUseCodeAsDefaultMessage(true);
    return new StockAssetGrowthHtmxController(
        null, null, null, null, null, null, null, null, messages);
  }

  private static MockHttpServletRequest bind(String method, String uri, String query) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setServerName("dev.bluesky.local");
    request.setServerPort(40122);
    request.setScheme("https");
    request.setRequestURI(uri);
    request.setQueryString(query);
    request.addHeader("HX-Request", "true");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    return request;
  }

  private static String render(Map<String, Object> model) {
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/error.jte", model, output);
    return output.toString();
  }

  @Test
  void 로그인_링크는_사용자가_보던_페이지로_돌아온다() {
    bind("GET", "/stock/htmx/summary", "timeZone=Asia%2FSeoul")
        .addHeader("HX-Current-URL", "https://dev.bluesky.local:40122/stock/dividend?tab=history");
    Model model = new ConcurrentModel();

    String view = controller().loginRequiredView(model);

    assertThat(view).isEqualTo("stock/htmx/error");
    assertThat(model.getAttribute("loginUrl"))
        .as("조각 주소가 아니라 보고 있던 페이지(HX-Current-URL)로 돌아와야 한다")
        .isEqualTo(
            "/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock%2Fdividend%3Ftab%3Dhistory");
  }

  @Test
  void 현재_페이지_헤더가_없으면_요청_주소로_돌아온다() {
    bind("GET", "/stock/htmx/summary", "timeZone=Asia%2FSeoul");
    Model model = new ConcurrentModel();

    controller().loginRequiredView(model);

    assertThat(model.getAttribute("loginUrl"))
        .isEqualTo(
            "/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock%2Fhtmx%2Fsummary%3FtimeZone%3DAsia%252FSeoul");
  }

  /** 요청 문맥이 없는 단위 테스트에서도 안내 문구는 그대로 나와야 한다(기존 호출부 15곳의 시그니처 불변). */
  @Test
  void 요청_문맥이_없으면_문구만_넣는다() {
    Model model = new ConcurrentModel();

    controller().loginRequiredView(model);
    controller().remoteFailureView(model);

    assertThat(model.asMap()).containsOnlyKeys("error");
  }

  @Test
  void 조회_실패에는_쿼리까지_그대로_재시도_주소가_붙는다() {
    bind("GET", "/stock/htmx/asset-growth/period-summary", "rangeMode=1y&timeZone=Asia%2FSeoul");
    Model model = new ConcurrentModel();

    controller().remoteFailureView(model);

    assertThat(model.getAttribute("retryUrl"))
        .isEqualTo("/stock/htmx/asset-growth/period-summary?rangeMode=1y&timeZone=Asia%2FSeoul");
  }

  @Test
  void 조회가_아닌_요청에는_재시도를_달지_않는다() {
    bind("POST", "/stock/htmx/asset-growth/period-summary", null);
    Model model = new ConcurrentModel();

    controller().remoteFailureView(model);

    assertThat(model.asMap()).doesNotContainKey("retryUrl");
  }

  @Test
  void 오류_조각은_주소가_있을_때만_링크와_버튼을_그린다() {
    Map<String, Object> login = new HashMap<>();
    login.put("error", "로그인이 필요합니다");
    login.put("loginUrl", "/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock");
    String loginHtml = render(login);
    assertThat(loginHtml)
        .contains("role=\"alert\"")
        .contains("href=\"/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock\"")
        .contains(">로그인</a>")
        .doesNotContain("<button");

    Map<String, Object> retry = new HashMap<>();
    retry.put("error", "불러오지 못했습니다");
    retry.put("retryUrl", "/stock/htmx/summary?timeZone=Asia%2FSeoul");
    String retryHtml = render(retry);
    assertThat(retryHtml)
        .contains("data-load-error")
        .contains("hx-get=\"/stock/htmx/summary?timeZone=Asia%2FSeoul\"")
        .contains("hx-target=\"closest [data-load-error]\"")
        .contains("hx-swap=\"outerHTML\"")
        .contains("다시 시도")
        .doesNotContain("<a ");

    Map<String, Object> plain = new HashMap<>();
    plain.put("error", "오류");
    assertThat(render(plain)).doesNotContain("<a ").doesNotContain("<button").contains("오류");
  }
}
