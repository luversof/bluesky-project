package net.luversof.web.dynamiccrud2.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 메뉴 검색 설정(Menu.searchSettings jsonb). 조회 결과 컬럼 이름(label)으로 가리킨다.
 *
 * @param fields 검색칸(순서대로)
 * @param keyword 통합 검색이 함께 찾는 글자 컬럼(비면 통합 검색칸 없음)
 */
public record SearchSettings(List<Item> fields, List<String> keyword) {

  public static final SearchSettings EMPTY = new SearchSettings(List.of(), List.of());

  /**
   * 검색칸 하나.
   *
   * @param pick 고르는 방식. null 이면 예전 저장값(필드 값 목록이 있으면 그 셀렉트, 없으면 입력)
   */
  public record Item(String label, SearchOperator operator, Pick pick) {

    public Item(String label, SearchOperator operator) {
      this(label, operator, null);
    }
  }

  /** 검색칸을 무엇으로 채우나 */
  public enum Pick {
    /** 글자 · 숫자 · 날짜를 직접 넣는다(필드에 값 목록이 있어도) */
    INPUT,
    /** 필드 값 목록(테이블 필드 관리)의 셀렉트 */
    LIST,
    /** DB 에 있는 그 컬럼의 값 전체(서로 다른 값)의 셀렉트. 값 목록이 있으면 표시 글자로 보인다 */
    DISTINCT;

    /** 실제로 쓸 방식: 안 정했으면(예전 저장값) 값 목록 유무로, 값 목록이 없는데 LIST 면 INPUT */
    public static Pick resolve(Pick pick, boolean hasValueList) {
      if (pick == null || (pick == LIST && !hasValueList)) {
        return hasValueList ? LIST : INPUT;
      }
      return pick;
    }
  }

  public boolean isEmpty() {
    return fields.isEmpty() && keyword.isEmpty();
  }

  /** 저장된 jsonb 를 읽는다. 모르는 방식 · 빈 항목은 건너뛴다 */
  @SuppressWarnings("unchecked")
  public static SearchSettings of(Map<String, Object> map) {
    if (map == null) {
      return EMPTY;
    }
    var fields = new ArrayList<Item>();
    if (map.get("fields") instanceof List<?> list) {
      for (var element : list) {
        if (element instanceof Map<?, ?> item
            && item.get("label") instanceof String label
            && item.get("operator") instanceof String operator) {
          try {
            fields.add(new Item(label, SearchOperator.valueOf(operator), pickOf(item.get("pick"))));
          } catch (IllegalArgumentException e) {
            // 지금 판이 모르는 방식 - 그 칸만 뺀다
          }
        }
      }
    }
    var keyword = new ArrayList<String>();
    if (map.get("keyword") instanceof List<?> list) {
      list.forEach(label -> {
        if (label instanceof String text && !text.isBlank()) {
          keyword.add(text);
        }
      });
    }
    return new SearchSettings(fields, keyword);
  }

  /** 모르는 고르기 방식은 null(예전 동작) */
  private static Pick pickOf(Object value) {
    if (value instanceof String text) {
      for (var pick : Pick.values()) {
        if (pick.name().equals(text)) {
          return pick;
        }
      }
    }
    return null;
  }

  public Map<String, Object> toMap() {
    var map = new LinkedHashMap<String, Object>();
    map.put(
        "fields",
        fields.stream()
            .map(item -> {
              var field = new LinkedHashMap<String, Object>();
              field.put("label", item.label());
              field.put("operator", item.operator().name());
              if (item.pick() != null) {
                field.put("pick", item.pick().name());
              }
              return (Map<String, Object>) field;
            })
            .toList());
    map.put("keyword", keyword);
    return map;
  }
}
