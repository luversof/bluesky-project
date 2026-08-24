package net.luversof.api.stock.web.advice;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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
 * 요청이 잘못된 경우를 400 으로 돌려준다.
 *
 * <p>공통 예외 처리는 {@code BlueskyException} / {@code BindException} 이 아닌 모든 예외를 500 으로 내보낸다. 그래서 필수
 * 파라미터가 빠지거나 UUID·날짜 형식이 틀린 요청까지 서버 오류가 됐다(실측: {@code /api/dataFirstDate}, {@code /api/dataStatus},
 * {@code /api/activityFilterIds}, {@code /api/dividend/meta}, {@code
 * /api/tradeProfit/holdingsSnapshot}, {@code holdingsSnapshotBatch} 6 개가 파라미터 누락에 500). 같은 상황에서
 * {@code /api/dividend} 는 400 을 내주고 있어 엔드포인트마다 답이 달랐다.
 *
 * <p>이 서비스는 인증 없이 노출돼 있어 5xx 가 나면 그대로 서버 오류 알림으로 잡힌다. 응답 본문 모양은 공통 유틸을 그대로 써서 바꾸지 않고 상태 코드만 바로잡는다.
 * 로그 레벨은 {@code bluesky-boot.core.log-except-exception-list} 로 낮춘다.
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StockRequestErrorHandler {

  /**
   * 커맨드 객체 바인딩 실패는 {@code BindException} 으로 온다.
   *
   * <p>공통 처리는 이 예외를 받아 요청이 JSON 을 원할 때만 problemDetail 을 돌려주고, 그렇지 않으면 에러 뷰를 찾다가 없으면 {@code null} 을
   * 반환한다({@code ExceptionUtil.handleException}). 그 {@code null} 이 그대로 나가서 <b>거부한 요청에 200 과 빈
   * 본문</b>이 응답됐다.
   *
   * <p>실측(로컬 40134, {@code accountIdList=not-a-uuid}): {@code Accept: application/json} 이면 400 과
   * {@code application/problem+json} 이 나오지만, {@code Accept} 가 없거나 {@code * / *} 또는 {@code
   * text/html} 이면 {@code calculateProfit}, {@code timeSeries}, {@code timeSeriesWithSummary},
   * {@code /api/trade}, {@code /api/dividend} 5 개가 모두 200 · 0 바이트였다. {@code * / *} 는 curl 과 대부분의
   * HTTP 라이브러리, 모니터링 프로브의 기본값이라 잘못된 요청이 조용히 "데이터 없음" 으로 보인다.
   *
   * <p>{@code MethodArgumentNotValidException} 이 {@code BindException} 을 상속하므로 이 한 줄이 둘 다 덮는다. 이
   * advice 가 {@code HIGHEST_PRECEDENCE} 라 협상 결과와 무관하게 항상 400 을 낸다.
   */
  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    BindException.class
  })
  public ResponseEntity<ProblemDetail> handleBadRequest(Exception exception) {
    return ResponseEntity.badRequest()
        .body(ProblemDetailUtil.getProblemDetail(exception, HttpStatus.BAD_REQUEST));
  }

  /**
   * 서비스가 상태 코드를 실어 던진 예외는 그 코드를 그대로 쓴다.
   *
   * <p>{@code ResponseStatusException} 은 자기 상태를 들고 있는데도 공통 처리에서 500 으로 나갔다(실측: 없는 심볼로 {@code
   * /api/monthlyDividendPayout} 을 부르면 코드가 400 을 의도했는데 응답은 500). 던진 쪽 의도를 존중한다.
   */
  @ExceptionHandler(ErrorResponseException.class)
  public ResponseEntity<ProblemDetail> handleErrorResponse(ErrorResponseException exception) {
    var status = exception.getStatusCode();
    return ResponseEntity.status(status)
        .body(ProblemDetailUtil.getProblemDetail(exception, status));
  }

  /**
   * 없는 경로(404)와 허용되지 않은 메서드(405)도 자기 상태를 들고 있다.
   *
   * <p>Spring 7 에서 이 둘은 {@code ErrorResponseException} 이 아니라 {@code ServletException} 을 상속하고 {@code
   * ErrorResponse} 인터페이스만 구현한다. 그래서 위 핸들러에 걸리지 않고 공통 처리로 떨어져 500 이 나갔다(실측: {@code /api/nope},
   * {@code /favicon.ico}, {@code PUT /api/trade} 모두 500). 인증 없이 노출된 서비스라 스캐너가 훑고 가면 그대로 서버 오류 알림이
   * 된다.
   */
  @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
  public ResponseEntity<ProblemDetail> handleServletErrorResponse(ServletException exception) {
    var status = ((ErrorResponse) exception).getStatusCode();
    return ResponseEntity.status(status)
        .body(ProblemDetailUtil.getProblemDetail(exception, status));
  }
}
