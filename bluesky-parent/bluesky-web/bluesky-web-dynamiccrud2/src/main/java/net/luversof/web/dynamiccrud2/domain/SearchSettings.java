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

  /** 검색칸 하나 */
  public record Item(String label, SearchOperator operator) {}

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
            fields.add(new Item(label, SearchOperator.valueOf(operator)));
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

  public Map<String, Object> toMap() {
    var map = new LinkedHashMap<String, Object>();
    map.put(
        "fields",
        fields.stream()
            .map(item ->
                Map.<String, Object>of("label", item.label(), "operator", item.operator().name()))
            .toList());
    map.put("keyword", keyword);
    return map;
  }
}
