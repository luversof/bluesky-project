package net.luversof.web.gate.stock.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;

/**
 * 주식 화면 요청이 실패하면 화면에 보이는 오류로 응답한다(조각 요청은 조각, 페이지 요청은 전체 화면).
 *
 * <p>이게 없으면 공통 예외 처리기가 본문 없는 200 을 만들고 htmx 가 그 빈 내용을 갈아끼워, 사용자에게는 아무 안내 없이 해당 영역만 사라진다(실측: 백엔드 중지
 * 시 summary/asset-growth/dividend 조각이 200·0바이트).
 *
 * <p>{@code @ControllerAdvice} 로 만들면 htmx 가 아닌 요청을 다시 던져야 하는데, 그러면 기존 처리 체인이 끊겨 JSON 엔드포인트가
 * Whitelabel 페이지를 돌려준다(실측). 여기서는 처리하지 않을 때 null 을 반환해 다음 처리기로 넘긴다.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class StockHtmxErrorResolver implements HandlerExceptionResolver {

  private static final Logger log = LoggerFactory.getLogger(StockHtmxErrorResolver.class);

  @Override
  public ModelAndView resolveException(
      HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
    String uri = request.getRequestURI();
    if (uri == null || !uri.startsWith("/stock")) {
      return null;
    }
    // JSON API 는 지금의 오류 응답(problem detail)을 그대로 둔다. HTML 을 돌려주면 규격이 깨진다.
    if (uri.startsWith("/stock/api/")) {
      return null;
    }
    // 주소가 /stock/api/ 아래가 아니어도 <b>JSON 을 보내 온 요청</b>이면 JSON 을 기대한다.
    //
    // 실측 2026-08-24: 로그인 없이 PUT /stock/dividend/monthly-reference/profile/order 를 부르면
    // 여기서 오류 화면을 <b>200</b> 으로 돌려준다(27,926 바이트). 브라우저의 putJson 은 res.ok 가
    // true 라 오류 갈래로 가지 않고 JSON.parse 에서 ParseError 를 내며, 그 예외에는 서버가 보낸 사유가
    // 없어 화면에는 기본 문구만 뜬다. 세션이 끊긴 것이라고 알려 줄 수 없고 감시 도구에도 200 으로 잡힌다.
    //
    // 폼 전송(application/x-www-form-urlencoded)은 화면을 기대하므로 그대로 둔다.
    String contentType = request.getContentType();
    if (contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).contains("json")) {
      return null;
    }
    // 없는 주소는 404 여야 한다. 오류 화면을 200 으로 돌려주면 /stock 아래 오타 주소가 전부
    // "정상 응답"으로 보인다(실측: 예전에 /stock/없는주소 가 200, 다른 모듈은 404).
    // 조회 실패와 주소 없음은 상태 코드로 구분하고, 아래에서 본문 형식만 요청에 맞춘다.
    boolean htmx = request.getHeader("HX-Request") != null;
    if (ex instanceof NoResourceFoundException) {
      // 상태 코드는 404 그대로 두되, 브라우저 요청이면 JSON 대신 화면을 돌려준다.
      // 기본 처리는 Accept 가 text/html 이어도 application/problem+json 을 내보내서
      // 주소를 잘못 친 사용자에게 날 JSON 이 그대로 보인다(실측: /stock/nope 가 problem+json 101 바이트).
      // htmx 조각 요청과 JSON 요청은 규격이 있으므로 손대지 않는다.
      String acceptHeader = request.getHeader("Accept");
      if (htmx || acceptHeader == null || !acceptHeader.contains("text/html")) {
        return null;
      }
      response.setStatus(HttpServletResponse.SC_NOT_FOUND);
      return notFoundView();
    }
    if (!htmx) {
      String accept = request.getHeader("Accept");
      if (accept != null && !accept.contains("text/html") && !accept.contains("*/*")) {
        return null;
      }
    }
    log.warn("stock view failed: {} htmx={} {}", uri, htmx, ex.toString(), ex);
    // 조회가 실패한 경우는 200 으로 돌려줘야 htmx 가 오류 조각을 갈아끼운다(위 주석 참고).
    // 다만 '허용되지 않은 메서드'는 조회 실패가 아니라 잘못된 요청이므로 405 로 답한다
    // (실측: PUT/POST /stock/htmx/summary 가 200 이었다). 화면에서 이 경로로 오는 요청은 없다.
    response.setStatus(
        ex instanceof HttpRequestMethodNotSupportedException
            ? HttpServletResponse.SC_METHOD_NOT_ALLOWED
            : HttpServletResponse.SC_OK);
    if (htmx) {
      return new ModelAndView("stock/htmx/fragments/loadError");
    }
    return pageErrorView("stock.error.fragment.title", "stock.error.fragment.desc");
  }

  private ModelAndView notFoundView() {
    return pageErrorView("stock.error.notfound.title", "stock.error.notfound.desc");
  }

  /**
   * 전체 화면은 레이아웃을 그리므로 로그인 표시에 필요한 값을 직접 채운다.
   *
   * <p>예외 경로에서는 {@code @ModelAttribute} 전역 어드바이스가 돌지 않아, 두지 않으면 로그인 상태인데도 상단에 "로그인" 버튼이 뜬다.
   */
  private ModelAndView pageErrorView(String titleKey, String descKey) {
    ModelAndView modelAndView = new ModelAndView("stock/pageError");
    modelAndView.addObject("isAuthenticated", UserUtil.getUserId() != null);
    modelAndView.addObject("username", UserUtil.getUsername());
    modelAndView.addObject("titleKey", titleKey);
    modelAndView.addObject("descKey", descKey);
    return modelAndView;
  }
}
