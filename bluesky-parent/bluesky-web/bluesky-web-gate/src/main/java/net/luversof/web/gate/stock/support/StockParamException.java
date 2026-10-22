package net.luversof.web.gate.stock.support;

/**
 * 주식 화면이 직접 읽는 요청 인자(날짜 · 타임존 · 정렬 · 줄 수 · 쪽 번호)를 쓸 수 없을 때의 공통 부모. 어느 인자였는지 이름을 들고 다니고, 화면에 보일 안내
 * 문구 키를 스스로 말한다.
 *
 * <p>{@code StockHtmxErrorResolver} 는 이 부모 하나로 "요청 쪽 잘못(페이지 400 · 조각은 오류 조각)" 으로 보고 이름과 문구 키를 넘긴다
 * &mdash; 2026-10-22 까지 다섯 예외가 같은 이름 필드를 한 벌씩 들고, 해석기에는 instanceof 다섯 줄이 두 벌(문구 · 400 판정) 있었다.
 */
public abstract class StockParamException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String name;

  protected StockParamException(String message, String name, Throwable cause) {
    super(message, cause);
    this.name = name;
  }

  /** 문제였던 인자 이름(안내 문구의 {0}). */
  public String getName() {
    return name;
  }

  /** 화면 안내 문구 키. */
  public abstract String descKey();
}
