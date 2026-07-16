package net.luversof.web.dynamiccrud.exception;

import java.lang.reflect.UndeclaredThrowableException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;

import io.github.luversof.boot.autoconfigure.web.util.ExceptionUtil;
import io.github.luversof.boot.exception.BlueskyErrorMessage;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.web.util.ProblemDetailUtil;

/**
 * bluesky boot의 기본 처리는 4xx를 에러 페이지(/support/404 등)로 redirect 하기 때문에 BlueskyException에 담긴 에러 메시지가
 * 화면에 표시되지 않는다. htmx fragment 요청에 한해 메시지를 담은 fragment를 직접 반환한다.
 */
@Order(
    Ordered.LOWEST_PRECEDENCE
        - 10) // bluesky boot의 CoreMvcExceptionHandler(LOWEST_PRECEDENCE)보다 먼저 처리되어야 함
@ControllerAdvice
public class DmtExceptionHandler {

  private static final String HTMX_REQUEST_HEADER = "HX-Request";

  /** ProblemDetail에 BrickErrorMessage가 담기는 속성명 (error/fragment.jte의 param명과 동일) */
  private static final String EXCEPTION_PARAMETER = "result";

  private static final String ERROR_FRAGMENT_VIEW = "error/fragment";

  /**
   * BrickException을 htmx fragment 요청에서 에러 메시지 fragment로 반환한다. fragment가 아닌 요청은 brick 기본 처리(에러 페이지
   * redirect)를 그대로 따른다.
   */
  @ExceptionHandler
  public <T extends BlueskyException> Object handleException(
      T exception, HandlerMethod handlerMethod, NativeWebRequest nativeWebRequest) {
    var problemDetail = ProblemDetailUtil.getProblemDetail(exception);

    if (!isHtmxRequest(nativeWebRequest)
        || !ExceptionUtil.isHtmlResponse(handlerMethod, nativeWebRequest)) {
      return ExceptionUtil.handleException(problemDetail, handlerMethod, nativeWebRequest);
    }

    // api 호출로 발생한 BrickException은 result에 BrickErrorMessage 목록이 담기므로 fragment 대상이 아니다.
    var properties = problemDetail.getProperties();
    if (properties == null
        || !(properties.get(EXCEPTION_PARAMETER)
            instanceof BlueskyErrorMessage blueskyErrorMessage)) {
      return ExceptionUtil.handleException(problemDetail, handlerMethod, nativeWebRequest);
    }

    // htmx는 2xx 응답만 swap 하므로 에러 메시지를 화면에 표시하려면 200으로 반환해야 한다.
    var modelAndView = new ModelAndView(ERROR_FRAGMENT_VIEW, HttpStatus.OK);
    modelAndView.addObject(EXCEPTION_PARAMETER, blueskyErrorMessage);
    return modelAndView;
  }

  private boolean isHtmxRequest(NativeWebRequest nativeWebRequest) {
    return "true".equals(nativeWebRequest.getHeader(HTMX_REQUEST_HEADER));
  }

  /**
   * @SneakyThrows를 통해 throw된 Exception의 내용을 안내하기 위해 처리
   *
   * @param <T>
   * @param exception
   * @param handlerMethod
   * @param nativeWebRequest
   * @return
   */
  @ExceptionHandler
  public <T extends UndeclaredThrowableException> Object handleException(
      T exception, HandlerMethod handlerMethod, NativeWebRequest nativeWebRequest) {
    return ExceptionUtil.handleException(
        ProblemDetailUtil.getProblemDetail(exception.getCause()), handlerMethod, nativeWebRequest);
  }
}
