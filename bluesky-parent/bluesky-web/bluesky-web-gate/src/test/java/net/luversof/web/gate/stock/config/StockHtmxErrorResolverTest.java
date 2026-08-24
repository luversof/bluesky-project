package net.luversof.web.gate.stock.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 주식 화면의 실패가 <b>보이는 오류</b>로 나가는지 고정한다.
 *
 * <p>이 처리기가 없거나 분기가 틀어지면 실패가 조용해진다. 각 분기는 실제로 겪은 사고를 하나씩 막고 있는데 지금까지 테스트가 없었다.
 *
 * <ul>
 *   <li>htmx 조각 요청: 공통 처리기가 <b>본문 없는 200</b> 을 만들고 htmx 가 그 빈 내용을 갈아끼워 영역이 그냥 사라졌다(실측: 백엔드 중지 시
 *       summary/asset-growth/dividend 조각이 200·0바이트).
 *   <li>없는 주소: 오류 화면을 200 으로 돌려주면 {@code /stock} 아래 오타 주소가 전부 정상 응답으로 보였다.
 *   <li>JSON API: HTML 을 돌려주면 규격이 깨진다.
 *   <li>허용되지 않은 메서드: 조회 실패가 아니라 잘못된 요청이므로 405 여야 한다(실측: PUT /stock/htmx/summary 가 200 이었다).
 * </ul>
 */
class StockHtmxErrorResolverTest {

  private final StockHtmxErrorResolver resolver = new StockHtmxErrorResolver();

  private MockHttpServletRequest request(String uri, boolean htmx, String accept) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
    request.setRequestURI(uri);
    if (htmx) {
      request.addHeader("HX-Request", "true");
    }
    if (accept != null) {
      request.addHeader("Accept", accept);
    }
    return request;
  }

  private ModelAndView resolve(
      MockHttpServletRequest request, MockHttpServletResponse response, Exception ex) {
    return resolver.resolveException(request, response, null, ex);
  }

  @Test
  void 주식이_아닌_주소는_건드리지_않는다() {
    var response = new MockHttpServletResponse();
    var view =
        resolve(
            request("/board/list", false, "text/html"),
            response,
            new IllegalStateException("boom"));

    assertThat(view).as("다른 모듈의 예외 처리를 가로채면 안 된다").isNull();
  }

  /**
   * JSON API 는 URI 로 걸러야 한다.
   *
   * <p>Accept 가 {@code application/json} 이면 아래 Accept 분기로도 통과하므로, 그 입력으로는 이 분기를 검증할 수 없다 (처음 쓴 이
   * 테스트가 그래서 분기를 지워도 통과했다). 브라우저가 흔히 보내는 {@code &#42;&#47;&#42;} 로 확인한다 &mdash; URI 분기가 없으면 이 요청에
   * HTML 오류 화면이 나가 JSON 규격이 깨진다.
   */
  @Test
  void JSON_API_는_Accept_와_무관하게_기존_오류_규격을_유지한다() {
    for (String accept : new String[] {"application/json", "*/*", "text/html"}) {
      var view =
          resolve(
              request("/stock/api/timeSeries", false, accept),
              new MockHttpServletResponse(),
              new IllegalStateException("boom"));
      assertThat(view).as("Accept=" + accept + " 인 JSON API 에 HTML 을 돌려주면 규격이 깨진다").isNull();
    }
  }

  /**
   * JSON 을 보내 온 요청은 주소가 {@code /stock/api/} 아래가 아니어도 JSON 오류를 받아야 한다.
   *
   * <p>실측 2026-08-24: 로그인 없이 {@code PUT /stock/dividend/monthly-reference/profile/order} 를 부르면 오류
   * 화면이 <b>200</b> 으로 나갔다(27,926 바이트). 브라우저의 {@code putJson} 은 {@code res.ok} 가 true 라 오류 갈래로 가지 않고
   * {@code JSON.parse} 에서 {@code ParseError} 를 낸다. 그 예외에는 서버가 보낸 사유가 없으므로 화면에는 기본 문구만 뜨고, 세션이 끊겼다는
   * 사실을 알려 줄 수 없다. 감시 도구에도 200 으로 잡힌다.
   *
   * <p>{@code Accept} 로는 가를 수 없다 &mdash; {@code fetch} 는 기본 {@code &#42;&#47;&#42;} 를 보내고 그 값은 아래
   * Accept 분기를 그대로 통과한다(그래서 이 검사는 Accept 를 {@code &#42;&#47;&#42;} 로 둔다). 요청 <b>본문</b>의
   * Content-Type 으로 가른다.
   */
  @Test
  void JSON_본문을_보낸_요청은_주소와_무관하게_JSON_오류를_받는다() {
    for (String contentType : new String[] {"application/json", "application/json;charset=UTF-8"}) {
      MockHttpServletRequest request =
          request("/stock/dividend/monthly-reference/profile/order", false, "*/*");
      request.setMethod("PUT");
      request.setContentType(contentType);

      var view = resolve(request, new MockHttpServletResponse(), new IllegalStateException("boom"));

      assertThat(view)
          .as("Content-Type=" + contentType + " 인 요청에 HTML 오류 화면을 200 으로 돌려주면 안 된다")
          .isNull();
    }
  }

  /** 폼 전송은 화면을 기대한다 &mdash; 위 분기가 이것까지 가져가면 안 된다. */
  @Test
  void 폼_전송은_그대로_오류_화면을_받는다() {
    MockHttpServletRequest request =
        request("/stock/dividend/monthly-reference/payout/delete", false, "text/html");
    request.setMethod("POST");
    request.setContentType("application/x-www-form-urlencoded");

    var view = resolve(request, new MockHttpServletResponse(), new IllegalStateException("boom"));

    assertThat(view).as("폼 전송에는 사람이 읽을 오류 화면이 나가야 한다").isNotNull();
  }

  /** 조각 요청이 실패하면 '빈 200' 이 아니라 오류 조각을 돌려줘야 한다. */
  @Test
  void htmx_조각_실패는_오류_조각을_돌려준다() {
    var response = new MockHttpServletResponse();
    var view =
        resolve(
            request("/stock/htmx/summary", true, "text/html"),
            response,
            new IllegalStateException("boom"));

    assertThat(view).isNotNull();
    assertThat(view.getViewName()).isEqualTo("stock/htmx/fragments/loadError");
    assertThat(response.getStatus()).as("htmx 가 내용을 갈아끼우려면 200 이어야 한다").isEqualTo(200);
  }

  @Test
  void 페이지_요청_실패는_오류_화면을_돌려준다() {
    var response = new MockHttpServletResponse();
    var view =
        resolve(
            request("/stock/trade", false, "text/html"),
            response,
            new IllegalStateException("boom"));

    assertThat(view).isNotNull();
    assertThat(view.getViewName()).isEqualTo("stock/pageError");
    assertThat(view.getModel()).containsKeys("titleKey", "descKey", "isAuthenticated");
    assertThat(response.getStatus()).isEqualTo(200);
  }

  /** 없는 주소는 200 이 아니라 404 여야 한다. 아니면 오타 주소가 전부 정상으로 보인다. */
  @Test
  void 없는_주소는_404_와_안내_화면이다() throws Exception {
    var response = new MockHttpServletResponse();
    var view =
        resolve(
            request("/stock/없는주소", false, "text/html"),
            response,
            new NoResourceFoundException(HttpMethod.GET, "/stock/없는주소", "static"));

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(view).isNotNull();
    assertThat(view.getViewName()).isEqualTo("stock/pageError");
  }

  /** 없는 주소라도 htmx·JSON 요청은 각자의 규격(problem detail)을 그대로 둔다. */
  @Test
  void 없는_주소라도_htmx_와_JSON_은_손대지_않는다() {
    for (var probe :
        new MockHttpServletRequest[] {
          request("/stock/htmx/nope", true, "text/html"),
          request("/stock/nope", false, "application/json")
        }) {
      var view =
          resolve(
              probe,
              new MockHttpServletResponse(),
              new NoResourceFoundException(HttpMethod.GET, probe.getRequestURI(), "static"));
      assertThat(view).as(probe.getRequestURI() + " 는 기존 처리에 맡겨야 한다").isNull();
    }
  }

  /** 허용되지 않은 메서드는 조회 실패가 아니다. */
  @Test
  void 허용되지_않은_메서드는_405_다() {
    var response = new MockHttpServletResponse();
    var view =
        resolve(
            request("/stock/htmx/summary", true, "text/html"),
            response,
            new HttpRequestMethodNotSupportedException("PUT"));

    assertThat(response.getStatus()).isEqualTo(405);
    assertThat(view).isNotNull();
  }
}
