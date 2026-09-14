package net.luversof.web.gate.stock.util;

import net.luversof.web.gate.stock.support.StockSortParamException;

/**
 * {@code sort=필드,방향} 문자열을 읽는다. 규칙은 timeZone · granularity 와 같다 &mdash; 값이 없으면 화면의 기본 순서, 값을 줬는데 쓸 수
 * 없으면 이름을 들고 끊는다.
 *
 * <p>필드 이름이 맞는지는 각 화면의 비교기 switch 가 판정한다(허용 목록을 여기 한 벌 더 두면 조용히 어긋난다). 이 유틸은 갈라 읽기와 방향만 맡는다.
 */
public final class StockSortUtil {

  private StockSortUtil() {}

  /** 쉼표 앞이 필드 이름이다. 정규식을 쓰지 않는다(빌드 도구가 이스케이프를 먹는 일이 반복됐다). */
  public static String field(String sort) {
    if (sort == null) {
      return null;
    }
    int comma = sort.indexOf(',');
    return (comma < 0 ? sort : sort.substring(0, comma)).trim();
  }

  /**
   * 방향이 내림차순인지. 방향을 적지 않으면 오름차순(종전 규칙)이고, asc/desc 가 아닌 값은 끊는다.
   *
   * <p>2026-09-11 까지 asc 가 아닌 모든 값이 조용히 오름차순이었다 &mdash; {@code payDate,sideways} 가 그대로 그려졌다.
   */
  public static boolean descending(String name, String sort) {
    if (sort == null) {
      return false;
    }
    int comma = sort.indexOf(',');
    if (comma < 0) {
      return false;
    }
    String direction = sort.substring(comma + 1).trim();
    if (direction.isEmpty() || "asc".equalsIgnoreCase(direction)) {
      return false;
    }
    if ("desc".equalsIgnoreCase(direction)) {
      return true;
    }
    throw new StockSortParamException(name, sort);
  }
}
