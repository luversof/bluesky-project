package net.luversof.web.gate.stock.util;

import net.luversof.web.gate.stock.support.StockPageSizeParamException;

/**
 * 주소의 {@code size}(한 쪽에 담을 줄 수)를 읽는 규칙.
 *
 * <p>값을 아예 주지 않으면 종전 기본값, 값을 줬는데 쓸 수 없는 값이면 이름 있는 400 이다 &mdash; {@code sort} · {@code timeZone} 과
 * 같은 규칙이다. 조용히 다른 수로 바꾸지 않는다.
 *
 * <p>상한을 두는 까닭: 실측 2026-09-12 에 {@code size=100000} 은 258 행을 한 번에 239,866 바이트로 그렸다. 화면은 이 값을 보내지
 * 않으므로 (실측: 매매 · 자산성장 어느 요청에도 {@code size=} 가 실리지 않는다) 상한을 넘는 값은 손으로 적은 것이고, 그 요청은 목록이 아니라 내려받기에
 * 가깝다.
 */
public final class StockPageSizeUtil {

  /** 한 쪽에 담을 수 있는 줄 수의 위끝. 화면이 쓰는 값은 20 이다. */
  public static final int MAX_PAGE_SIZE = 200;

  private StockPageSizeUtil() {}

  /**
   * 쓸 수 있는 줄 수면 그대로, 아니면 이름을 들고 끊는다.
   *
   * @param name 어느 파라미터였는지(문구에 실린다)
   * @param size 요청이 준 값
   */
  public static int resolve(String name, int size) {
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new StockPageSizeParamException(name, size);
    }
    return size;
  }
}
