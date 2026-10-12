package net.luversof.web.gate.poe;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 아이템 툴팁 요구 능력치에 그 아이템 자신의 옵션을 반영(10-08 C177, PoE1 · PoE2 공용) — "+150 Strength Requirement"(해질녘) ·
 * "800% increased Attribute Requirements"(메드베드의 도전) · "Has no Attribute Requirements"(디알라의 악의). 예전엔
 * 베이스 요구치를 그대로 그려 인게임 툴팁과 달랐다.
 *
 * <p>규칙은 PoB Item.lua 와 같다: (베이스 + 고정 합) × (1 + 증가 % 합 / 100), 내림. "능력치 요구사항 없음"이면 0. "Items and
 * Gems have …" · "Equipment and Skill Gems have …" 처럼 다른 아이템에도 걸리는 전역 옵션은 이 아이템 요구치를 바꾸지 않는다(PoB 도
 * GlobalAttributeRequirements 로 따로 센다). 롤 범위("+(500-700)")는 최소 · 최대를 따로 계산해 "(최소-최대)" 로 보인다.
 */
public final class PoeRequirements {

  private PoeRequirements() {}

  private static final String NUM = "\\(?(\\d+)(?:-(\\d+))?\\)?";

  private static final String ATTRS =
      "(strength and intelligence|strength and dexterity|dexterity and intelligence|strength|dexterity|intelligence|attribute)";

  private static final Pattern FLAT =
      Pattern.compile("^([+-])" + NUM + " " + ATTRS + " requirements?$", Pattern.CASE_INSENSITIVE);

  private static final Pattern INC =
      Pattern.compile(
          "^" + NUM + "% (increased|reduced) " + ATTRS + " requirements?$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern NONE =
      Pattern.compile("^has no attribute requirements$", Pattern.CASE_INSENSITIVE);

  /** 반영한 요구치 한 칸 — min == max 면 고정값. 둘 다 0 이면 요구 없음(그리지 않는다). */
  public record Value(int min, int max) {

    public boolean present() {
      return max > 0;
    }

    /** 화면 글자 — 고정이면 "249", 범위면 "(680-880)". */
    public String text() {
      return min == max ? Integer.toString(min) : "(" + min + "-" + max + ")";
    }
  }

  /** 힘 · 민첩 · 지능 순서. */
  public record Result(Value str, Value dex, Value intel) {}

  /** 암시 · 고정 옵션 줄(영문 원문)을 합쳐 반영 — 템플릿용. */
  public static Result apply(
      Integer str, Integer dex, Integer intel, List<String> implicits, List<String> explicits) {
    List<String> all = new java.util.ArrayList<>();
    if (implicits != null) {
      all.addAll(implicits);
    }
    if (explicits != null) {
      all.addAll(explicits);
    }
    return apply(str, dex, intel, all);
  }

  /** 베이스 요구치와 아이템 옵션 줄(영문 원문)로 반영한 요구치. lines 가 null 이면 베이스 그대로. */
  public static Result apply(Integer str, Integer dex, Integer intel, List<String> lines) {
    int[] base = {nz(str), nz(dex), nz(intel)};
    int[] flatMin = new int[3];
    int[] flatMax = new int[3];
    int[] incMin = new int[3];
    int[] incMax = new int[3];
    boolean none = false;
    for (String raw : lines == null ? List.<String>of() : lines) {
      if (raw == null) {
        continue;
      }
      String line = raw.trim();
      if (NONE.matcher(line).matches()) {
        none = true;
        continue;
      }
      Matcher f = FLAT.matcher(line);
      if (f.matches()) {
        int sign = "-".equals(f.group(1)) ? -1 : 1;
        int lo = Integer.parseInt(f.group(2));
        int hi = f.group(3) == null ? lo : Integer.parseInt(f.group(3));
        for (int a : targets(f.group(4))) {
          flatMin[a] += sign * (sign > 0 ? lo : hi);
          flatMax[a] += sign * (sign > 0 ? hi : lo);
        }
        continue;
      }
      Matcher m = INC.matcher(line);
      if (m.matches()) {
        int sign = "reduced".equalsIgnoreCase(m.group(3)) ? -1 : 1;
        int lo = Integer.parseInt(m.group(1));
        int hi = m.group(2) == null ? lo : Integer.parseInt(m.group(2));
        for (int a : targets(m.group(4))) {
          incMin[a] += sign * (sign > 0 ? lo : hi);
          incMax[a] += sign * (sign > 0 ? hi : lo);
        }
      }
    }
    Value[] out = new Value[3];
    for (int a = 0; a < 3; a++) {
      if (none) {
        out[a] = new Value(0, 0);
        continue;
      }
      int lo = calc(base[a] + flatMin[a], incMin[a]);
      int hi = calc(base[a] + flatMax[a], incMax[a]);
      out[a] = new Value(Math.min(lo, hi), Math.max(lo, hi));
    }
    return new Result(out[0], out[1], out[2]);
  }

  private static int calc(int flat, int inc) {
    return Math.max(0, (int) Math.floor(flat * (1 + inc / 100.0)));
  }

  private static int nz(Integer v) {
    return v == null ? 0 : v;
  }

  private static int[] targets(String attrs) {
    return switch (attrs.toLowerCase(Locale.ROOT)) {
      case "strength" -> new int[] {0};
      case "dexterity" -> new int[] {1};
      case "intelligence" -> new int[] {2};
      case "strength and intelligence" -> new int[] {0, 2};
      case "strength and dexterity" -> new int[] {0, 1};
      case "dexterity and intelligence" -> new int[] {1, 2};
      default -> new int[] {0, 1, 2};
    };
  }
}
