package net.luversof.web.gate.stock.support;

/**
 * 주소에서 받은 <b>정렬 지정</b>을 쓸 수 없을 때. 어느 값이 문제였는지 이름을 들고 다닌다.
 *
 * <p>실측 2026-09-11(배당 상세 202 행 · 매매 258 행): 모르는 정렬 키는 조용히 무시됐는데, 그 결과가 기본 순서도 아니었다. 표시 순서를 뒤집는 규칙이
 * {@code sort == null} 에만 걸려 있어 <b>세 번째 순서</b>가 나왔다 &mdash; 지정 없음은 오래된 것부터(2020-04-08), {@code
 * sort=BOGUS} 는 최근 것부터(2026-09-02). 사용자는 자기가 적은 정렬이 먹혔다고 읽는다.
 *
 * <p>방향도 같다: {@code payDate,sideways} 는 조용히 오름차순이었다.
 */
public class StockSortParamException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  private final String name;

  public StockSortParamException(String name, String value) {
    super("sort parameter cannot be used: " + name + "=" + value);
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
