package net.luversof.web.dynamiccrud2.admin.api;

import java.util.NoSuchElementException;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.dynamiccrud2.adapter.QueryErrorCode;
import net.luversof.web.dynamiccrud2.admin.api.AdminApiViews.ApiError;
import net.luversof.web.dynamiccrud2.config.Msg;

/** 관리 JSON 호출의 오류를 {"error": 사유} 로(화면이면 폼 위에 보일 문구). 로그인 · 권한은 AccessDeniedAdvice 가 401 · 403 으로 */
// 공통 처리기(ProblemDetail 등)보다 먼저 - 이 패키지 컨트롤러에만 걸리므로 다른 화면에는 영향이 없다
@org.springframework.core.annotation.Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackageClasses = AdminApiAdvice.class)
public class AdminApiAdvice {

  /** 입력 검사 · 되돌리기 불가 등 */
  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError invalid(IllegalArgumentException e) {
    return new ApiError(e.getMessage());
  }

  /** 주소 이름 겹침 · 하위가 있어 못 지움 · 쿼리 검사 등 */
  @ExceptionHandler(BlueskyException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError rejected(BlueskyException e) {
    var message = QueryErrorCode.messageOf(e);
    return new ApiError(message == null ? e.getErrorCode() : message);
  }

  /** 본문이 JSON 이 아니거나 모양이 다름(모르는 enum 값 등) */
  @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError unreadable(org.springframework.http.converter.HttpMessageNotReadableException e) {
    var cause = e.getMostSpecificCause().getMessage();
    return new ApiError(Msg.t("api.unreadable", cause == null ? "" : cause.lines().findFirst().orElse(cause)));
  }

  /** 없는 id(서비스의 findById().orElseThrow()) */
  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiError notFound(NoSuchElementException e) {
    return new ApiError(Msg.t("api.notFound"));
  }

  /** DB 가 받지 않음(되돌리기 · 실행 계획 등) - 첫 줄만 */
  @ExceptionHandler(DataAccessException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiError failed(DataAccessException e) {
    var message = e.getMostSpecificCause().getMessage();
    return new ApiError(
        Msg.t("menu.action.failed", message == null ? "" : message.lines().findFirst().orElse(message)));
  }
}
