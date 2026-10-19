package net.luversof.web.gate.poe;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 여러 등급을 합친 범위 문장(10-16 C373 — 계산기 목표 라벨이 "T1~T3" 인데 T1 최대값만 보였다). 기준 문장(영어)의 숫자를 자리마다 짝지어 — 범위
 * "(a-b)" 는 (a, b), 고정값 n 은 (n, n) — 가장 높은 등급의 범위 자리를 가장 낮은 등급 최솟값 ~ 가장 높은 등급 최댓값으로 바꾼다(보일 문장의 한국어
 * "a~b" · "(a~b)" 포함). 두 등급의 숫자 자리 수가 다르면 그대로 둔다.
 */
public final class PoeModSpan {

  private PoeModSpan() {}

  private static final Pattern TOKEN =
      Pattern.compile(
          "\\((-?\\d+(?:\\.\\d+)?)[-–](-?\\d+(?:\\.\\d+)?)\\)|(?<![\\d.])(-?\\d+(?:\\.\\d+)?)");

  /** text = 보일 문장(가장 높은 등급), best · worst = 가장 높은 · 낮은 등급의 기준(영어 범위) 문장. */
  public static String span(String text, String best, String worst) {
    if (text == null || best == null || worst == null) {
      return text == null ? "" : text;
    }
    List<String[]> hi = tokens(best);
    List<String[]> lo = tokens(worst);
    if (hi.size() != lo.size()) {
      return text;
    }
    StringBuilder sb = new StringBuilder();
    int from = 0;
    for (int k = 0; k < hi.size(); k++) {
      if (!"r".equals(hi.get(k)[2])) {
        continue; // 고정값 자리는 그대로
      }
      Matcher m = PoeModRoll.pair(hi.get(k)[0], hi.get(k)[1]).matcher(text);
      if (!m.find(from)) {
        continue;
      }
      boolean paren = m.group(1) != null;
      String sep = paren ? m.group(1) : m.group(2);
      String range = lo.get(k)[0] + sep + hi.get(k)[1];
      sb.append(text, from, m.start()).append(paren ? "(" + range + ")" : range);
      from = m.end();
    }
    sb.append(text.substring(from));
    return sb.toString();
  }

  /** 숫자 자리들 — {최소, 최대, "r"(범위) | "n"(고정값)}. */
  private static List<String[]> tokens(String s) {
    List<String[]> out = new ArrayList<>();
    Matcher m = TOKEN.matcher(s);
    while (m.find()) {
      out.add(
          m.group(1) != null
              ? new String[] {m.group(1), m.group(2), "r"}
              : new String[] {m.group(3), m.group(3), "n"});
    }
    return out;
  }
}
