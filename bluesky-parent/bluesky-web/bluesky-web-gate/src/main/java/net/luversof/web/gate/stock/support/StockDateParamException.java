package net.luversof.web.gate.stock.support;

/**
 * 주소에서 받은 <b>날짜 문자열</b>을 읽지 못했을 때. 어느 값이 문제였는지 이름을 들고 다닌다.
 *
 * <p>실측 2026-09-11: 매매 이력 조각은 {@code from}/{@code to} 를 String 으로 받아 컨트롤러 안에서 직접 읽는다. 그래서 {@code
 * from=notadate} 는 형 변환 예외가 아니라 {@code DateTimeParseException} 이 되어 "불러오지 못했습니다 · <b>잠시 후 다시 시도해
 * 주세요</b>" 로 나왔다 &mdash; 같은 조각의 {@code size}/{@code page} 는 "주소의 size 값을 읽지 못했습니다" 라고 정확히 알려 주는데
 * 날짜만 서버 장애처럼 보였고, 다시 시도해도 결과는 같다.
 */
public class StockDateParamException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String name;

  public StockDateParamException(String name, Throwable cause) {
    super("date parameter cannot be read: " + name, cause);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
