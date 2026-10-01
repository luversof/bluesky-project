package net.luversof.api.poe.poe2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PoE2 영문 옵션 줄 → 한국어 (빌드 요약의 희귀·마법 장비 옵션). PoE1 PoeModTranslateService 와 같은 방식: 데이터에 이미 있는 영/한 쌍(옵션
 * 풀 티어·고유 옵션·증강물 효과·베이스 암시)을 수치 자리표시(§)로 정규화한 사전을 만들고, 들어온 영문 줄도 같게 정규화해 찾은 뒤 실제 롤 숫자를 한국어 틀에 되꽂는다.
 * 못 찾으면 null(화면은 영문 유지).
 *
 * <p>PoE2 데이터의 범위 표기: 영문 "(45-50)", 한국어 풀 "45~50"(괄호 없음) · 고유 "(15-20)" · 부가 피해 "(1~2)~(3~4)". 범위는
 * 실제 아이템에선 한 수치라 먼저 § 하나로 접고, 남은 숫자도 § 로 바꾼다. 영/한 § 개수가 다르면(문장 구조가 다름) 사전에 넣지 않는다.
 */
public final class Poe2ModTranslator {

  private static final String SLOT = "§";
  private static final String NUM = "-?\\d+(?:\\.\\d+)?";
  private static final Pattern NUMBER = Pattern.compile(NUM);
  private static final Pattern PAREN_RANGE =
      Pattern.compile("\\(" + NUM + "\\s*[-~]\\s*" + NUM + "\\)");
  private static final Pattern BARE_RANGE = Pattern.compile(NUM + "~" + NUM);

  private final Map<String, String> dictionary;

  private Poe2ModTranslator(Map<String, String> dictionary) {
    this.dictionary = dictionary;
  }

  /** 영/한 줄 목록 쌍들로 사전을 만든다(먼저 넣은 쌍이 이긴다 — 옵션 풀을 먼저 넘길 것). */
  public static Poe2ModTranslator of(List<List<String>[]> pairs) {
    Map<String, String> map = new HashMap<>();
    for (List<String>[] pair : pairs) {
      List<String> en = pair[0];
      List<String> ko = pair[1];
      if (en == null || ko == null) {
        continue;
      }
      for (int i = 0; i < en.size() && i < ko.size(); i++) {
        String e = en.get(i);
        String k = ko.get(i);
        if (e == null || k == null || e.isBlank() || k.isBlank() || e.equals(k)) {
          continue;
        }
        String et = template(e);
        String kt = template(k);
        if (count(et) != count(kt)) {
          // 한국어에만 있는 글자 그대로의 숫자("1초마다 생명력 29.1~33 재생", "투사체 1개 추가")는 롤이 아니다 → 범위만 접은 틀로 다시
          kt = rangesOnly(k);
        }
        if (count(et) == count(kt)) {
          map.putIfAbsent(et, kt);
        }
      }
    }
    return new Poe2ModTranslator(Map.copyOf(map));
  }

  public int size() {
    return dictionary.size();
  }

  /** 영문 한 줄 → 한국어(못 찾으면 null). 숫자는 영문 줄에 나온 순서대로 한국어 틀의 § 에 넣는다. */
  public String translate(String en) {
    if (en == null || en.isBlank()) {
      return null;
    }
    String ko = dictionary.get(template(en));
    if (ko == null) {
      return null;
    }
    List<String> numbers = new ArrayList<>();
    Matcher m = NUMBER.matcher(stripRanges(en));
    while (m.find()) {
      numbers.add(m.group());
    }
    StringBuilder out = new StringBuilder();
    int n = 0;
    for (int i = 0; i < ko.length(); i++) {
      char c = ko.charAt(i);
      if (c == SLOT.charAt(0) && n < numbers.size()) {
        out.append(numbers.get(n++));
      } else {
        out.append(c);
      }
    }
    return out.toString();
  }

  /** 범위는 실제 아이템처럼 앞쪽 한 수치로 — 번역 입력이 "(45-50)" 원문이어도 숫자 개수가 틀과 맞게 */
  private static String stripRanges(String s) {
    String t =
        PAREN_RANGE.matcher(s).replaceAll(r -> Matcher.quoteReplacement(firstNumber(r.group())));
    return BARE_RANGE.matcher(t).replaceAll(r -> Matcher.quoteReplacement(firstNumber(r.group())));
  }

  private static String firstNumber(String range) {
    Matcher m = NUMBER.matcher(range);
    return m.find() ? m.group() : range;
  }

  static String template(String s) {
    String t = PAREN_RANGE.matcher(s.trim()).replaceAll(SLOT);
    t = BARE_RANGE.matcher(t).replaceAll(SLOT);
    return NUMBER.matcher(t).replaceAll(SLOT);
  }

  private static String rangesOnly(String s) {
    String t = PAREN_RANGE.matcher(s.trim()).replaceAll(SLOT);
    return BARE_RANGE.matcher(t).replaceAll(SLOT);
  }

  private static int count(String t) {
    int c = 0;
    for (int i = 0; i < t.length(); i++) {
      if (t.charAt(i) == SLOT.charAt(0)) {
        c++;
      }
    }
    return c;
  }
}
