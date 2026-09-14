package net.luversof.web.gate.stock.support;

/**
 * 주소에서 받은 <b>한 쪽에 담을 줄 수</b>를 쓸 수 없을 때. 어느 값이 문제였는지 이름을 들고 다닌다.
 *
 * <p>실측 2026-09-12(매매 이력 258 행): 형제 값들은 이미 이름을 들고 끊는데({@code sort=BOGUS} · {@code page=abc} ·
 * {@code size=abc} 모두 400) {@code size} 의 <b>범위</b>만 규칙 밖이었다 &mdash; {@code size=0} 과 {@code
 * size=-5} 는 조용히 20 으로 바뀌었고, {@code size=100000} 은 상한 없이 258 행을 한 번에 그렸다(239,866 바이트).
 *
 * <p>화면은 이 값을 보내지 않는다. 그러니 이 값은 손으로 만든 주소에서만 오는데, 조용히 다른 수로 바꾸면 사용자는 자기가 적은 줄 수가 먹혔다고 읽는다 &mdash;
 * {@code sort} 와 {@code timeZone} 에서 같은 이유로 이미 끊기로 했다.
 */
public class StockPageSizeParamException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String name;

  public StockPageSizeParamException(String name, int value) {
    super("page size parameter cannot be used: " + name + "=" + value);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
