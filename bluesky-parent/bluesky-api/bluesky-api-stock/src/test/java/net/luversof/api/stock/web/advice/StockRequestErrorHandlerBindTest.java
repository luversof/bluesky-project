package net.luversof.api.stock.web.advice;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 바인딩이 실패한 요청을 400 으로 돌려주는 계약을 고정한다.
 *
 * <p>왜 필요한가(실측, 로컬 40134 · {@code accountIdList=not-a-uuid}): 이 핸들러가 {@code BindException} 을 받기 전에는
 * 응답이 {@code Accept} 헤더에 따라 갈렸다.
 *
 * <pre>
 *   Accept: application/json  -&gt; 400  application/problem+json   (정상)
 *   Accept 없음 / 별표 / text/html -&gt; 200  본문 0 바이트            (거부인데 성공으로 보임)
 * </pre>
 *
 * <p>그렇게 200 이 나간 엔드포인트는 {@code /api/tradeProfit/calculateProfit}, {@code timeSeries}, {@code
 * timeSeriesWithSummary}, {@code /api/trade}, {@code /api/dividend} 5 개다. 공통 처리({@code
 * ExceptionUtil.handleException})가 JSON 협상이 되지 않으면 에러 뷰를 찾다가 {@code null} 을 반환하기 때문이고, 와일드카드 {@code
 * Accept} 는 curl 과 대부분의 HTTP 라이브러리·모니터링 프로브의 기본값이다.
 *
 * <p>핸들러 자체를 직접 호출해 검증하지 못하는 이유: {@code ProblemDetailUtil} 이 {@code ApplicationContextUtil} 로 전역
 * 컨텍스트에서 {@code MessageSource} 를 꺼내므로 컨텍스트 없는 단위 테스트에서 NPE 가 난다. 그래서 여기서는 등록 여부만 고정하고, 상태 코드와 본문은 떠
 * 있는 서비스에 실제 요청을 보내 확인했다.
 */
class StockRequestErrorHandlerBindTest {

  @Test
  void 바인딩_실패_예외가_400_핸들러에_등록돼_있다() throws NoSuchMethodException {
    // MethodArgumentNotValidException(커맨드 객체) 도 BindException 하위형이라 한 줄로 함께 덮인다.
    assertTrue(BindException.class.isAssignableFrom(MethodArgumentNotValidException.class));

    var annotation =
        StockRequestErrorHandler.class
            .getDeclaredMethod("handleBadRequest", Exception.class)
            .getAnnotation(ExceptionHandler.class);

    assertTrue(
        Arrays.asList(annotation.value()).contains(BindException.class),
        "BindException 이 빠지면 Accept 협상에 따라 200 빈 응답으로 되돌아간다");
  }
}
