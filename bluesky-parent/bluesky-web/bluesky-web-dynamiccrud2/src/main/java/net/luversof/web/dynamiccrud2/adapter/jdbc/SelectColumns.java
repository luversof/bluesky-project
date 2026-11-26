package net.luversof.web.dynamiccrud2.adapter.jdbc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * 조회 SQL 의 SELECT 칸 목록을 글자로 다룬다(JSqlParser 로 다시 쓰면 1차 · 사용자 서식이 사라진다). 칸마다 시작 위치(쉼표 다음)와 최상위 FROM
 * 위치. 1차 가져오기(칸 빼기 · 별칭 바꾸기 · 순서)와 메뉴 설정의 칸 순서 바꾸기가 쓴다.
 *
 * @param starts 칸마다 시작 위치
 * @param from 최상위 FROM 위치
 */
public record SelectColumns(List<Integer> starts, int from) {

  private static final Pattern HEAD = Pattern.compile("(?is)^\\s*SELECT\\s+");
  private static final Pattern DISTINCT_TOP = Pattern.compile("(?is)^(DISTINCT|TOP)\\b");
  private static final Pattern ITEM_NAME =
      Pattern.compile("(?s)(?:\\[([^\\]]+)\\]|`([^`]+)`|'([^']+)'|\"([^\"]+)\"|([\\w$#@]+))\\s*$");

  /** 읽을 수 없는 모양(DISTINCT · TOP · FROM 없음 · SELECT 아님)이면 null */
  public static SelectColumns of(String sql) {
    if (sql == null) {
      return null;
    }
    var head = HEAD.matcher(sql);
    if (!head.find() || DISTINCT_TOP.matcher(sql.substring(head.end())).find()) {
      return null;
    }
    var starts = new ArrayList<Integer>();
    starts.add(head.end());
    var depth = 0;
    char quote = 0;
    for (int i = head.end(); i < sql.length(); i++) {
      var c = sql.charAt(i);
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if (c == '\'' || c == '"' || c == '`') {
        quote = c;
      } else if (c == '[') {
        quote = ']';
      } else if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
      } else if (depth == 0 && c == ',') {
        starts.add(i + 1);
      } else if (depth == 0
          && sql.regionMatches(true, i, "FROM", 0, 4)
          && !Character.isLetterOrDigit(sql.charAt(i - 1))
          && sql.charAt(i - 1) != '_'
          && (i + 4 >= sql.length()
              || !Character.isLetterOrDigit(sql.charAt(i + 4)) && sql.charAt(i + 4) != '_')) {
        return new SelectColumns(starts, i);
      }
    }
    return null;
  }

  /** 칸 글자(앞뒤 공백 포함, 쉼표 제외) */
  public List<String> items(String sql) {
    var items = new ArrayList<String>();
    for (int k = 0; k < starts.size(); k++) {
      items.add(sql.substring(starts.get(k), k + 1 < starts.size() ? starts.get(k + 1) - 1 : from));
    }
    return items;
  }

  /** SELECT 칸 하나의 결과 이름: 끝 이름(별칭 · t.컬럼 의 컬럼). 따옴표 · 대괄호는 벗긴다. 식만 있으면(* 포함) null */
  public static String resultName(String item) {
    var m = ITEM_NAME.matcher(item.strip());
    if (!m.find()) {
      return null;
    }
    for (int g = 1; g <= 5; g++) {
      if (m.group(g) != null) {
        return m.group(g);
      }
    }
    return null;
  }

  /**
   * 칸을 order(결과 이름 → 순서, 대소문자 무시) 순서로 다시 놓는다. order 에 없는 칸은 뒤로, 같은 순서는 원래 순서대로. 칸 자리의 앞뒤 공백(줄바꿈)은
   * 그대로 두고 내용만 바꾼다. 순서가 그대로거나 읽을 수 없는 모양이면 null
   */
  public static String reorder(String sql, Map<String, Integer> order) {
    var list = of(sql);
    if (list == null || order.isEmpty()) {
      return null;
    }
    var lower = new HashMap<String, Integer>();
    order.forEach((k, v) -> lower.put(k.toLowerCase(Locale.ROOT), v));
    var items = list.items(sql);
    var ranked = new ArrayList<Integer>(IntStream.range(0, items.size()).boxed().toList());
    ranked.sort(
        Comparator.comparing(k -> {
          var name = resultName(items.get(k));
          return name == null
              ? Integer.MAX_VALUE
              : lower.getOrDefault(name.toLowerCase(Locale.ROOT), Integer.MAX_VALUE);
        }));
    if (ranked.equals(IntStream.range(0, items.size()).boxed().toList())) {
      return null;
    }
    var out = new StringBuilder(sql.substring(0, list.starts().get(0)));
    for (int k = 0; k < items.size(); k++) {
      var slot = items.get(k);
      var lead = slot.substring(0, slot.length() - slot.stripLeading().length());
      var trail = slot.substring(slot.stripTrailing().length());
      out.append(k == 0 ? "" : ",")
          .append(lead.isEmpty() && k > 0 ? " " : lead)
          .append(items.get(ranked.get(k)).strip())
          .append(trail);
    }
    return out.append(sql.substring(list.from())).toString();
  }

  /** 화면에서 칸 순서를 바꿀 수 있는 쿼리인가: SELECT 칸마다 결과 이름이 있다(* · 별칭 없는 식이 없다) */
  public static boolean reorderable(String sql) {
    var list = of(sql);
    return list != null && list.items(sql).stream().allMatch(item -> resultName(item) != null);
  }
}
