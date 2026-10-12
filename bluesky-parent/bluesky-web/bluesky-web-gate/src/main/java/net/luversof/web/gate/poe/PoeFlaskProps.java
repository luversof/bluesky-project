package net.luversof.web.gate.poe;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 플라스크 툴팁 속성(회복량 · 지속시간 · 충전)에 그 플라스크 자신의 로컬 옵션을 반영(10-08 C186). 예전엔 고유 플라스크도 베이스 값이라 "(30-40)%
 * increased Duration" · "+(10-20) to Maximum Charges" 가 있는데 베이스 지속 · 충전을 보였다.
 *
 * <p>규칙은 PoB Item.lua 와 같다 — 회복 플라스크: 회복량 = 베이스 × (1 + 품질/100) × (1 + 회복량 증가%/100), 지속 = round1(베이스
 * × (1 + 지속 증가%/100) ÷ (1 + 회복 속도 증가%/100) × 지속 감폭(less)). 유틸 플라스크: 지속 = round1(베이스 × (1 + 지속
 * 증가%/100) × (1 + 품질/100) × 감폭). 충전: 최대 = 베이스 + 고정, 소모 = floor(베이스 × (1 + 소모 증가%/100)).
 */
public final class PoeFlaskProps {

  private PoeFlaskProps() {}

  /** 값 범위 — changed = 옵션으로 바뀜(인게임 파란 값). */
  public record Range(double lo, double hi, boolean changed) {

    public String text() {
      long a = Math.round(lo);
      long b = Math.round(hi);
      return a == b ? Long.toString(a) : a + "-" + b;
    }

    /** 초 단위(소수 둘째 자리 — PoE1 툴팁 표기). */
    public String seconds() {
      return seconds(2);
    }

    /** 초 단위 — 소수 자릿수를 고른다(PoE2 툴팁은 첫째 자리). */
    public String seconds(int decimals) {
      String f = "%." + decimals + "f";
      String a = String.format(java.util.Locale.ROOT, f, lo);
      String b = String.format(java.util.Locale.ROOT, f, hi);
      return a.equals(b) ? a : a + "-" + b;
    }
  }

  public record Result(
      Range life, Range mana, Range duration, Range maxCharges, Range chargesUsed) {}

  private static final String N = "\\(?(-?\\d+(?:\\.\\d+)?)(?:-(-?\\d+(?:\\.\\d+)?))?\\)?";

  private static final Pattern INC =
      Pattern.compile(
          "^"
              + N
              + "% (increased|reduced) (amount recovered|life recovered|mana recovered|recovery rate|duration|charges per use|charges used|charges)$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern LESS =
      Pattern.compile("^" + N + "% (less|more) duration$", Pattern.CASE_INSENSITIVE);

  private static final Pattern MAX_CHARGES =
      Pattern.compile("^\\+" + N + " to maximum charges$", Pattern.CASE_INSENSITIVE);

  private static final class Acc {
    double lo;
    double hi;
    boolean any;

    void add(double a, double b) {
      lo += Math.min(a, b);
      hi += Math.max(a, b);
      any = true;
    }
  }

  /**
   * @param utility 유틸 플라스크(회복 없음) — 품질이 지속시간에 곱해진다
   * @param quality 품질 %(PoE1 고유 상세 20)
   */
  public static Result apply(
      double life,
      double mana,
      double duration,
      int maxCharges,
      int chargesUsed,
      boolean utility,
      List<String> lines,
      int quality) {
    Acc recAll = new Acc(),
        recLife = new Acc(),
        recMana = new Acc(),
        rate = new Acc(),
        dur = new Acc();
    Acc used = new Acc(), charges = new Acc(), chargesInc = new Acc();
    double moreLo = 1, moreHi = 1;
    boolean moreAny = false;
    for (String raw : lines == null ? List.<String>of() : lines) {
      if (raw == null) {
        continue;
      }
      String line = raw.trim();
      Matcher m;
      if ((m = INC.matcher(line)).matches()) {
        double a = Double.parseDouble(m.group(1));
        double b = m.group(2) == null ? a : Double.parseDouble(m.group(2));
        if ("reduced".equalsIgnoreCase(m.group(3))) {
          double t = -b;
          b = -a;
          a = t;
        }
        switch (m.group(4).toLowerCase(java.util.Locale.ROOT)) {
          case "amount recovered" -> recAll.add(a, b);
          case "life recovered" -> recLife.add(a, b);
          case "mana recovered" -> recMana.add(a, b);
          case "recovery rate" -> rate.add(a, b);
          case "duration" -> dur.add(a, b);
          // PoE2 "N% increased Charges" — 최대 충전 증가(PoB2 charmData.chargesMax = (베이스 + 고정) × (1 +
          // 증가%), 10-08 C190)
          case "charges" -> chargesInc.add(a, b);
          default -> used.add(a, b);
        }
      } else if ((m = LESS.matcher(line)).matches()) {
        double a = Double.parseDouble(m.group(1));
        double b = m.group(2) == null ? a : Double.parseDouble(m.group(2));
        boolean less = "less".equalsIgnoreCase(m.group(3));
        double x = 1 + (less ? -a : a) / 100.0;
        double y = 1 + (less ? -b : b) / 100.0;
        moreLo *= Math.min(x, y);
        moreHi *= Math.max(x, y);
        moreAny = true;
      } else if ((m = MAX_CHARGES.matcher(line)).matches()) {
        double a = Double.parseDouble(m.group(1));
        double b = m.group(2) == null ? a : Double.parseDouble(m.group(2));
        charges.add(a, b);
      }
    }
    double q = 1 + quality / 100.0;
    Range lifeR =
        recovery(
            life, q, recAll.lo + recLife.lo, recAll.hi + recLife.hi, recAll.any || recLife.any);
    Range manaR =
        recovery(
            mana, q, recAll.lo + recMana.lo, recAll.hi + recMana.hi, recAll.any || recMana.any);
    Range durR;
    if (utility) {
      double a = round1(duration * (1 + dur.lo / 100) * q * moreLo);
      double b = round1(duration * (1 + dur.hi / 100) * q * moreHi);
      durR = new Range(Math.min(a, b), Math.max(a, b), dur.any || moreAny);
    } else {
      // 회복 속도는 분모 — 짧은 쪽은 지속 증가 최소 · 속도 최대, 긴 쪽은 그 반대
      double a = round1(duration * (1 + dur.lo / 100) / (1 + rate.hi / 100) * moreLo);
      double b = round1(duration * (1 + dur.hi / 100) / (1 + rate.lo / 100) * moreHi);
      durR = new Range(Math.min(a, b), Math.max(a, b), dur.any || rate.any || moreAny);
    }
    double m1 = (maxCharges + charges.lo) * (1 + chargesInc.lo / 100);
    double m2 = (maxCharges + charges.hi) * (1 + chargesInc.hi / 100);
    Range maxR = new Range(Math.min(m1, m2), Math.max(m1, m2), charges.any || chargesInc.any);
    double u1 = Math.floor(chargesUsed * (1 + used.lo / 100));
    double u2 = Math.floor(chargesUsed * (1 + used.hi / 100));
    Range usedR = new Range(Math.min(u1, u2), Math.max(u1, u2), used.any);
    return new Result(lifeR, manaR, durR, maxR, usedR);
  }

  private static Range recovery(double base, double q, double incLo, double incHi, boolean any) {
    double a = base * q * (1 + incLo / 100);
    double b = base * q * (1 + incHi / 100);
    return new Range(Math.min(a, b), Math.max(a, b), any);
  }

  private static double round1(double v) {
    return Math.round(v * 10) / 10.0;
  }
}
