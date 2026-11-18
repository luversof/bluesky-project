package net.luversof.web.dynamiccrud2.adapter;

import java.util.List;

import net.luversof.web.dynamiccrud2.domain.SearchOperator;

/**
 * 조회에 덧붙이는 검색 조건 하나. 값은 이름 붙은 파라미터로 따로 바인딩한다.
 *
 * <p>labels 가 여럿이면 OR(통합 검색). CONTAINS · STARTS_WITH 는 대소문자를 가리지 않는 LIKE 이고 값은 호출하는 쪽이 % 와
 * 이스케이프(!)를 붙여 넘긴다. RANGE 는 param(부터) · paramTo(까지) 중 null 인 쪽을 뺀다.
 *
 * @param labels 조회 결과 컬럼 이름
 * @param operator 비교 방식
 * @param param 값 파라미터 이름(RANGE 의 부터, 없으면 null)
 * @param paramTo RANGE 의 까지 파라미터 이름(없으면 null)
 */
public record Filter(List<String> labels, SearchOperator operator, String param, String paramTo) {

  /** LIKE 값의 이스케이프 문자 */
  public static final char ESCAPE = '!';

  /** LIKE 특수 문자(% _ 와 이스케이프 문자)를 글자 그대로 찾게 바꾼다 */
  public static String escapeLike(String value) {
    var out = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      if (c == '%' || c == '_' || c == ESCAPE) {
        out.append(ESCAPE);
      }
      out.append(c);
    }
    return out.toString();
  }
}
