package net.luversof.web.gate.stock.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 로그인 뒤 돌아올 주소: 조회는 그 주소, 조회가 아닌 요청은 폼이 있던 화면.
 *
 * <p>실측 2026-09-09: 세션이 끊긴 채 월배당 기준 폼을 저장하면 로그인으로 보내면서 돌아올 주소로 <b>POST 전용 주소</b>를 붙였다. 로그인 뒤 그 주소를
 * GET 으로 열게 되니 "직접 열 수 없는 주소입니다"(405) 화면에 떨어졌다. 관리·시뮬레이터의 POST 핸들러 10곳이 모두 그랬다.
 */
class LoginRedirectAfterPostTest {

  private static MockHttpServletRequest request(String method, String uri, String query) {
    MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
    request.setScheme("https");
    request.setServerName("dev.bluesky.local");
    request.setServerPort(40122);
    request.setRequestURI(uri);
    request.setQueryString(query);
    return request;
  }

  @Test
  void 조회는_그_주소_그대로_돌아온다() {
    assertThat(
            StockViewSupport.returnUrlAfterLogin(request("GET", "/stock/item", "stockItemId=abc")))
        .isEqualTo("https://dev.bluesky.local:40122/stock/item?stockItemId=abc");
    assertThat(StockViewSupport.loginRedirectView(request("GET", "/stock/trade", null)))
        .isEqualTo(
            "redirect:/login?redirectUrl=https%3A%2F%2Fdev.bluesky.local%3A40122%2Fstock%2Ftrade");
  }

  @Test
  void 폼_전송은_폼이_있던_화면으로_돌아온다() {
    MockHttpServletRequest post = request("POST", "/stock/dividend/monthly-reference/payout", null);
    post.addHeader("Referer", "https://dev.bluesky.local:40122/stock/admin?tab=monthly-reference");
    assertThat(StockViewSupport.returnUrlAfterLogin(post))
        .as("POST 주소로 돌아오면 405 다")
        .isEqualTo("https://dev.bluesky.local:40122/stock/admin?tab=monthly-reference");
  }

  @Test
  void 폼이_있던_화면을_모르거나_다른_출처면_주식_첫_화면이다() {
    assertThat(
            StockViewSupport.returnUrlAfterLogin(
                request("POST", "/stock/simulator/monthly-dividend", null)))
        .isEqualTo("https://dev.bluesky.local:40122/stock");
    MockHttpServletRequest foreign =
        request("PUT", "/stock/dividend/monthly-reference/profile/order", null);
    foreign.addHeader("Referer", "https://evil.example/stock/admin");
    assertThat(StockViewSupport.returnUrlAfterLogin(foreign))
        .as("다른 출처의 Referer 로는 돌려보내지 않는다(오픈 리다이렉트)")
        .isEqualTo("https://dev.bluesky.local:40122/stock");
  }

  @Test
  void 표준_포트는_주소에_붙이지_않는다() {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/stock");
    request.setScheme("https");
    request.setServerName("stock.example");
    request.setServerPort(443);
    request.setRequestURI("/stock");
    assertThat(StockViewSupport.returnUrlAfterLogin(request))
        .isEqualTo("https://stock.example/stock");
  }
}
