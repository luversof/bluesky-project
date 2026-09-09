package net.luversof.web.gate.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import io.github.luversof.boot.exception.BlueskyException;

/**
 * 잘못된 입력(4xx 계열)은 "불러오지 못했습니다 · 잠시 후 다시 시도" 가 아니라 "잘못된 요청" 안내다. 재시도 버튼도 없다.
 *
 * <p>실측 2026-09-09(조작 입력 26건): {@code startDate=garbage}, {@code stockItemIdList=not-a-uuid}, 검증
 * 실패, 백엔드의 400 거절 4건이 전부 백엔드 장애와 같은 안내로 나왔다. 같은 입력을 다시 보내도 결과는 같으므로 사용자가 할 일(입력 수정)을 알려야 한다.
 */
class ClientErrorNoticeTest {

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

  private static MockHttpServletRequest request(boolean htmx, String uri, String query) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRequestURI(uri);
    request.setQueryString(query);
    request.addHeader("Accept", "text/html");
    if (htmx) {
      request.addHeader("HX-Request", "true");
    }
    return request;
  }

  private static Exception typeMismatch() throws NoSuchMethodException {
    MethodParameter parameter =
        new MethodParameter(
            ClientErrorNoticeTest.class.getDeclaredMethod("sample", String.class), 0);
    return new MethodArgumentTypeMismatchException(
        "garbage", java.time.Instant.class, "startDate", parameter, new IllegalArgumentException());
  }

  @SuppressWarnings("unused")
  private static void sample(String startDate) {}

  @Test
  void 형_변환_실패_조각은_잘못된_요청_안내이고_재시도가_없다() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView view =
        resolver.resolveException(
            request(true, "/stock/htmx/trade/list", "startDate=garbage"),
            response,
            null,
            typeMismatch());

    assertThat(view.getViewName()).isEqualTo("stock/htmx/fragments/loadError");
    assertThat(view.getModel())
        .containsEntry("titleKey", "stock.error.badrequest.title")
        .containsEntry("descKey", "stock.error.badrequest.desc")
        .as("입력을 고쳐야 하는 상황에 재시도 버튼은 거짓 희망이다")
        .doesNotContainKey("retryUrl");
    assertThat(response.getStatus()).as("htmx 가 갈아끼우려면 200").isEqualTo(200);
  }

  @Test
  void 백엔드가_400_으로_거절한_호출도_잘못된_요청이다() {
    ModelAndView view =
        resolver.resolveException(
            request(true, "/stock/htmx/holdings-snapshot", "date=garbage"),
            new MockHttpServletResponse(),
            null,
            new BlueskyException("API_EXCEPTION", 400));

    assertThat(view.getModel()).containsEntry("titleKey", "stock.error.badrequest.title");
  }

  @Test
  void 백엔드_5xx_는_여전히_불러오지_못했습니다_와_재시도다() {
    ModelAndView view =
        resolver.resolveException(
            request(true, "/stock/htmx/summary", "timeZone=Asia%2FSeoul"),
            new MockHttpServletResponse(),
            null,
            new BlueskyException("API_EXCEPTION", 503));

    assertThat(view.getModel())
        .doesNotContainKey("titleKey")
        .containsEntry("retryUrl", "/stock/htmx/summary?timeZone=Asia%2FSeoul");
  }

  @Test
  void 필수_파라미터_누락도_잘못된_요청이다() {
    ModelAndView view =
        resolver.resolveException(
            request(true, "/stock/htmx/summary", null),
            new MockHttpServletResponse(),
            null,
            new MissingServletRequestParameterException("timeZone", "String"));

    assertThat(view.getModel()).containsEntry("titleKey", "stock.error.badrequest.title");
  }

  @Test
  void 페이지_요청의_잘못된_입력은_400_과_안내_화면이다() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView view =
        resolver.resolveException(
            request(false, "/stock/item", "stockItemId=garbage"), response, null, typeMismatch());

    assertThat(view.getViewName()).isEqualTo("stock/pageError");
    assertThat(view.getModel())
        .containsEntry("titleKey", "stock.error.badrequest.title")
        .doesNotContainKey("retryUrl");
    assertThat(response.getStatus()).isEqualTo(400);
  }

  @Test
  void 오류_조각은_넘겨받은_키로_문구를_그린다() {
    Map<String, Object> model = new HashMap<>();
    model.put("titleKey", "stock.error.badrequest.title");
    model.put("descKey", "stock.error.badrequest.desc");
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html)
        .render("stock/htmx/fragments/loadError.jte", model, output);
    String html = output.toString();

    assertThat(html).contains("잘못된 요청입니다").contains("입력한 값").doesNotContain("<button");
    assertThat(html).as("메시지 키가 번들에 없으면 키가 그대로 나간다").doesNotContain("stock.error.badrequest");
  }

  /**
   * 405 는 입력값과 무관하다 - 화면이 아닌 주소를 연 것이다. 실측 2026-09-09: POST 전용 저장 주소를 GET 으로 열면 '입력한 값을 확인하세요' 가
   * 나왔다.
   */
  @Test
  void 허용되지_않은_메서드는_전용_문구다() {
    MockHttpServletResponse response = new MockHttpServletResponse();
    ModelAndView view =
        resolver.resolveException(
            request(true, "/stock/htmx/summary", null),
            response,
            null,
            new org.springframework.web.HttpRequestMethodNotSupportedException("PUT"));
    assertThat(response.getStatus()).isEqualTo(405);
    assertThat(view.getModel())
        .containsEntry("titleKey", "stock.error.method.title")
        .containsEntry("descKey", "stock.error.method.desc")
        .doesNotContainKey("retryUrl");

    MockHttpServletResponse pageResponse = new MockHttpServletResponse();
    ModelAndView pageView =
        resolver.resolveException(
            request(false, "/stock/dividend/monthly-reference/payout", null),
            pageResponse,
            null,
            new org.springframework.web.HttpRequestMethodNotSupportedException("GET"));
    assertThat(pageResponse.getStatus()).isEqualTo(405);
    assertThat(pageView.getViewName()).isEqualTo("stock/pageError");
    assertThat(pageView.getModel()).containsEntry("titleKey", "stock.error.method.title");
  }

  @Test
  void 전용_문구_키가_두_번들에_있다() throws java.io.IOException {
    for (String bundle : java.util.List.of("uiMessage.properties", "uiMessage_ko.properties")) {
      String source =
          java.nio.file.Files.readString(
              java.nio.file.Path.of("src/main/resources", bundle),
              java.nio.charset.StandardCharsets.UTF_8);
      assertThat(source)
          .as(bundle)
          .contains("stock.error.method.title")
          .contains("stock.error.method.desc");
    }
  }
}
