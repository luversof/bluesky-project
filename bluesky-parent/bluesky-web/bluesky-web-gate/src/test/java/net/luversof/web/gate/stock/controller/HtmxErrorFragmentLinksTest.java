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
 * 컨트롤러가 직접 돌려주는 오류 조각({@code stock/htmx/error})에도 다음 행동이 붙는다: 세션이 풀렸으면 로그인 링크.
 *
 * <p>실측 2026-09-09(익명으로 조각 27개 호출): 전부 "로그인이 필요합니다" 한 줄(337B)만 나왔고 로그인으로 가는 길이 없었다.
 *
 * <p>2026-10-22 까지 이 시험은 백엔드 조회 실패 안내({@code remoteFailureView})의 재시도 버튼도 봤다. 그 안내를 부르던 마지막 라우트가
 * 2026-09-09 에 지워진 뒤로는 시험만 불러 메서드와 버튼을 함께 지웠다 - 조회 실패의 재시도는 공통 예외 처리기의 loadError 조각이 맡고
 * LoadErrorRetryTest 가 지킨다.
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

  /** 요청 문맥이 없는 단위 테스트에서도 안내 문구는 그대로 나와야 한다(기존 호출부 11곳의 시그니처 불변). */
  @Test
  void 요청_문맥이_없으면_문구만_넣는다() {
    Model model = new ConcurrentModel();

    controller().loginRequiredView(model);

    assertThat(model.asMap()).containsOnlyKeys("error");
  }

  @Test
  void 오류_조각은_로그인_주소가_있을_때만_링크를_그린다() {
    Map<String, Object> login = new HashMap<>();
    login.put("error", "로그인이 필요합니다");
    login.put("loginUrl", "/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock");
    String loginHtml = render(login);
    assertThat(loginHtml)
        .contains("role=\"alert\"")
        .contains("href=\"/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock\"")
        .contains(">로그인</a>")
        .doesNotContain("<button");

    Map<String, Object> plain = new HashMap<>();
    plain.put("error", "오류");
    assertThat(render(plain)).doesNotContain("<a ").doesNotContain("<button").contains("오류");
  }

  /**
   * 조각 실패 제목 문구 키가 실제 번들에 있는지 - 없으면 화면에 키 문자열이 그대로 나간다. 공통 예외 처리기의 loadError · pageError 가
   * 쓴다(2026-10-22 PeriodSummaryFailureTest 에서 옮김).
   */
  @Test
  void 조각_실패_문구_키가_번들에_있다() throws java.io.IOException {
    for (String bundle : java.util.List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      java.nio.file.Path path = java.nio.file.Path.of("src/main/resources", bundle);
      String source = java.nio.file.Files.readString(path, java.nio.charset.StandardCharsets.UTF_8);
      // 키 줄 자체를 본다 - 글자만 찾으면 stock.error.fragment.title.xxx 나 주석에도 통과한다.
      assertThat(source)
          .as(bundle + " 에 stock.error.fragment.title 이 없다")
          .containsPattern("(?m)^stock\\.error\\.fragment\\.title\\s*=");
    }
  }
}
