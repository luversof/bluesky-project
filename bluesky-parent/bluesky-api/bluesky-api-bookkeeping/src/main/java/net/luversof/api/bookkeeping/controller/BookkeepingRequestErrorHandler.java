package net.luversof.api.bookkeeping.controller;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import io.github.luversof.boot.web.util.ProblemDetailUtil;
import jakarta.servlet.ServletException;

/**
 * 잘못된 요청을 400 으로 돌려준다(api-stock StockRequestErrorHandler 와 같은 처리).
 *
 * <p>공통 예외 처리는 BlueskyException · BindException 이 아닌 예외를 500 으로 내고, BindException 은 Accept 가 JSON 이
 * 아니면 200 · 빈 본문을 낸다. 그래서 파라미터 누락 · UUID / 날짜 형식 오류 · 읽을 수 없는 JSON 본문(모르는 enum 값 등)을 여기서 400 으로
 * 바로잡는다. 응답 본문 모양은 공통 유틸 그대로다.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BookkeepingRequestErrorHandler {

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    BindException.class,
    HttpMessageNotReadableException.class
  })
  public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest()
        .body(ProblemDetailUtil.getProblemDetail(exception, HttpStatus.BAD_REQUEST));
  }

  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException exception) {
    var status = exception.getStatusCode();
    return ResponseEntity.status(status)
        .body(ProblemDetailUtil.getProblemDetail(exception, status));
  }

  @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
  public ResponseEntity<ProblemDetail> handleServletErrorResponse(ServletException exception) {
    var status = ((ErrorResponse) exception).getStatusCode();
    return ResponseEntity.status(status)
        .body(ProblemDetailUtil.getProblemDetail(exception, status));
  }
}
