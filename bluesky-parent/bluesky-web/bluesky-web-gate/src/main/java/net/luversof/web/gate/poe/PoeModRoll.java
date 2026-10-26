package net.luversof.web.gate.poe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SplittableRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 범위 문장의 수치를 굴린다(10-16 C370 — 제작 에뮬레이터 툴팁을 인게임처럼 굴린 값으로). 범위 위치는 기준 문장(영어 "(70-84)" 꼴)에서 찾고, 보일
 * 문장에서 같은 숫자 쌍("(70-84)" · "(70~84)" · PoE2 한국어 "70~84")을 차례로 굴린 값으로 바꾼다 — 영어 · 한국어가 같은 값을 보인다. 범위가
 * 아닌 숫자는 그대로. 정수 범위는 양 끝 포함 균등, 소수 범위는 소수 자릿수 단위로 균등. 같은 씨앗이면 같은 값.
 */
public final class PoeModRoll {

  private PoeModRoll() {}

  private static final Pattern RANGE =
      Pattern.compile("\\((-?\\d+(?:\\.\\d+)?)[-–](-?\\d+(?:\\.\\d+)?)\\)");

  /** 기준 문장이 보일 문장과 같을 때(범위가 "(a-b)" 꼴로 들어 있는 문장). */
  public static String roll(String rangeText, long seed) {
    return roll(rangeText, rangeText, seed);
  }

  /** text = 보일 문장, ref = 범위 위치를 읽을 기준 문장(같은 옵션의 영어 범위 문장). */
  public static String roll(String text, String ref, long seed) {
    if (text == null) {
      return "";
    }
    if (ref == null) {
      return text;
    }
    SplittableRandom rng = new SplittableRandom(seed);
    List<String[]> pairs = new ArrayList<>();
    List<String> values = new ArrayList<>();
    Matcher r = RANGE.matcher(ref);
    while (r.find()) {
      String a = r.group(1);
      String b = r.group(2);
      int scale = Math.max(decimals(a), decimals(b));
      double unit = Math.pow(10, -scale);
      long lo = Math.round(Math.min(Double.parseDouble(a), Double.parseDouble(b)) / unit);
      long hi = Math.round(Math.max(Double.parseDouble(a), Double.parseDouble(b)) / unit);
      long v = lo + (long) Math.floor(rng.nextDouble() * (hi - lo + 1));
      pairs.add(new String[] {a, b});
      // 소수점은 서버 로케일과 무관하게 "."(C378 검토 — de 로케일이면 "0,3")
      values.add(
          scale == 0 ? Long.toString(v) : String.format(Locale.ROOT, "%." + scale + "f", v * unit));
    }
    StringBuilder sb = new StringBuilder();
    int from = 0;
    for (int k = 0; k < pairs.size(); k++) {
      Matcher m = pair(pairs.get(k)[0], pairs.get(k)[1]).matcher(text);
      if (!m.find(from)) {
        continue;
      }
      sb.append(text, from, m.start()).append(values.get(k));
      from = m.end();
    }
    sb.append(text.substring(from));
    return sb.toString();
  }

  /**
   * 숫자 쌍 a·b 를 찾는 식 — 괄호째 "(a~b)" 이거나 괄호 없는 "a~b"(C378 검토: 괄호를 따로 붙여 찾아 "(최대 10~20)" 의 바깥 괄호를 먹었고,
   * 문장 끝 마침표 "10~20." 은 못 찾았다). 그룹 1 = 괄호 꼴 구분자, 그룹 2 = 괄호 없는 꼴 구분자.
   */
  static Pattern pair(String a, String b) {
    String qa = Pattern.quote(a);
    String qb = Pattern.quote(b);
    return Pattern.compile(
        "\\(" + qa + "([-–~])" + qb + "\\)|(?<![\\d.])" + qa + "([-–~])" + qb + "(?!\\d|\\.\\d)");
  }

  /** 행운 굴림(C399 축성된 화석 "관련 수치 개선") — 범위마다 두 번 굴려 큰 값. 씨앗 하나로 결정적(두 번째는 씨앗을 섞어서). 나머지는 roll 과 같다. */
  public static String rollLucky(String text, String ref, long seed) {
    if (text == null) {
      return "";
    }
    if (ref == null) {
      return text;
    }
    SplittableRandom rng = new SplittableRandom(seed);
    SplittableRandom rng2 = new SplittableRandom(seed ^ 0x9E3779B97F4A7C15L);
    List<String[]> pairs = new ArrayList<>();
    List<String> values = new ArrayList<>();
    Matcher r = RANGE.matcher(ref);
    while (r.find()) {
      String a = r.group(1);
      String b = r.group(2);
      int scale = Math.max(decimals(a), decimals(b));
      double unit = Math.pow(10, -scale);
      long lo = Math.round(Math.min(Double.parseDouble(a), Double.parseDouble(b)) / unit);
      long hi = Math.round(Math.max(Double.parseDouble(a), Double.parseDouble(b)) / unit);
      long v1 = lo + (long) Math.floor(rng.nextDouble() * (hi - lo + 1));
      long v2 = lo + (long) Math.floor(rng2.nextDouble() * (hi - lo + 1));
      long v = Math.max(v1, v2);
      pairs.add(new String[] {a, b});
      values.add(
          scale == 0 ? Long.toString(v) : String.format(Locale.ROOT, "%." + scale + "f", v * unit));
    }
    StringBuilder sb = new StringBuilder();
    int from = 0;
    for (int k = 0; k < pairs.size(); k++) {
      Matcher m = pair(pairs.get(k)[0], pairs.get(k)[1]).matcher(text);
      if (!m.find(from)) {
        continue;
      }
      sb.append(text, from, m.start()).append(values.get(k));
      from = m.end();
    }
    sb.append(text.substring(from));
    return sb.toString();
  }

  private static final Pattern NUMBER = Pattern.compile("(?<![\\d.])\\d+(?:\\.\\d+)?");

  // 범위 문장의 숫자 자리 — "(a-b)" · "(a~b)" · 괄호 없는 "a~b"(PoE2 한국어) 는 범위 하나, 그 밖 숫자는 고정값 하나
  private static final Pattern RANGE_OR_NUMBER =
      Pattern.compile(
          "\\((-?\\d+(?:\\.\\d+)?)[-–~](-?\\d+(?:\\.\\d+)?)\\)"
              + "|(?<![\\d.])(\\d+(?:\\.\\d+)?)~(\\d+(?:\\.\\d+)?)(?![\\d.])"
              + "|(?<![\\d.])\\d+(?:\\.\\d+)?");

  /**
   * 인게임 상세 보기(Alt — 10-22 Q3c, craftofexile 'Hold Alt to toggle advanced modifier descriptions')처럼
   * 굴린 값 뒤에 범위 — "+123 to maximum Life" + "+(115-129) to maximum Life" → "+123(115-129) to maximum
   * Life". 숫자 자리를 차례로 맞추고, 범위 자리면 굴린 숫자 뒤에 "(a-b)", 고정값 자리면 그대로. 자리 수가 다르면(문장 모양이 달라짐) 굴린 문장 그대로.
   */
  public static String withRanges(String rolled, String ranges) {
    if (rolled == null) {
      return "";
    }
    if (ranges == null) {
      return rolled;
    }
    List<String> slots = new ArrayList<>();
    Matcher r = RANGE_OR_NUMBER.matcher(ranges);
    while (r.find()) {
      if (r.group(1) != null) {
        slots.add("(" + r.group(1) + "-" + r.group(2) + ")");
      } else if (r.group(3) != null) {
        slots.add("(" + r.group(3) + "-" + r.group(4) + ")");
      } else {
        slots.add("");
      }
    }
    List<int[]> nums = new ArrayList<>();
    Matcher m = NUMBER.matcher(rolled);
    while (m.find()) {
      nums.add(new int[] {m.start(), m.end()});
    }
    if (nums.size() != slots.size() || slots.stream().allMatch(String::isEmpty)) {
      return rolled;
    }
    StringBuilder sb = new StringBuilder();
    int from = 0;
    for (int k = 0; k < nums.size(); k++) {
      sb.append(rolled, from, nums.get(k)[1]).append(slots.get(k));
      from = nums.get(k)[1];
    }
    sb.append(rolled.substring(from));
    return sb.toString();
  }

  /**
   * 기폭제 퀄리티(C385) — 문장의 숫자를 × (100 + quality) / 100, 0 쪽으로 버림(PoB itemLib.formatValue
   * floorSymmetric). 소수는 그 자릿수에서 버린다. quality 가 0 이하면 그대로.
   */
  public static String scale(String text, int quality) {
    if (text == null) {
      return "";
    }
    if (quality <= 0) {
      return text;
    }
    Matcher m = NUMBER.matcher(text);
    StringBuilder sb = new StringBuilder();
    int from = 0;
    while (m.find()) {
      String n = m.group();
      int d = decimals(n);
      long units = Math.round(Double.parseDouble(n) * Math.pow(10, d));
      long v = units * (100 + quality) / 100;
      sb.append(text, from, m.start())
          .append(
              d == 0
                  ? Long.toString(v)
                  : String.format(Locale.ROOT, "%." + d + "f", v / Math.pow(10, d)));
      from = m.end();
    }
    sb.append(text.substring(from));
    return sb.toString();
  }

  /**
   * 축성(C390, PoE2) — 문장의 숫자를 × pct / 100, 올림(poe2wiki "rounding up"). 소수는 그 자릿수에서 올린다. pct 가 100 이면
   * 그대로.
   */
  public static String scaleUp(String text, int pct) {
    if (text == null) {
      return "";
    }
    if (pct == 100 || pct <= 0) {
      return text;
    }
    Matcher m = NUMBER.matcher(text);
    StringBuilder sb = new StringBuilder();
    int from = 0;
    while (m.find()) {
      String n = m.group();
      int d = decimals(n);
      long units = Math.round(Double.parseDouble(n) * Math.pow(10, d));
      // 음수(앞에 '-')는 올림 = 0 쪽(C397 검토 — 크기를 올려 -5.46 이 -6 이 됐다)
      boolean neg = m.start() > 0 && text.charAt(m.start() - 1) == '-';
      long v = neg ? units * pct / 100 : (units * pct + 99) / 100;
      sb.append(text, from, m.start())
          .append(
              d == 0
                  ? Long.toString(v)
                  : String.format(Locale.ROOT, "%." + d + "f", v / Math.pow(10, d)));
      from = m.end();
    }
    sb.append(text.substring(from));
    return sb.toString();
  }

  private static int decimals(String n) {
    int dot = n.indexOf('.');
    return dot < 0 ? 0 : n.length() - dot - 1;
  }
}
