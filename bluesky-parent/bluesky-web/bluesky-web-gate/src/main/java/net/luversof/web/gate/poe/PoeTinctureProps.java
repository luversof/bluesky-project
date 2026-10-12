package net.luversof.web.gate.poe;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 팅크처 속성 칸(10-09 C211) — 인게임 "{0}초마다 마나 연소 유발"(ItemDisplayTinctureToxicityNSeconds) · "비활성화 시 재사용
 * 대기시간 {0}초"(ItemDisplayTinctureCooldownNSeconds).
 *
 * <p>옵션 반영은 PoB Item.lua 식: 마나 연소 = 기본 ÷ (1 + 연소 속도 증가%), 재사용 대기시간 = 기본 ÷ (1 + 재사용 대기시간 회복 속도 증가%).
 * PoB 는 연소 간격에 0.01 을 더하지만(엔진 계산용) 인게임 표기는 데이터 값 그대로라(poedb 재껍질 팅크 "every 0.60 Seconds") 더하지 않는다.
 * 표기는 연소 간격 소수 둘째 자리, 재사용 대기시간은 정수면 정수("7 Second Cooldown").
 */
public final class PoeTinctureProps {

  private PoeTinctureProps() {}

  public record Result(PoeFlaskProps.Range manaBurn, PoeFlaskProps.Range cooldown) {

    public String manaBurnText() {
      return manaBurn.seconds();
    }

    public String cooldownText() {
      String a = trim(cooldown.lo());
      String b = trim(cooldown.hi());
      return a.equals(b) ? a : a + "-" + b;
    }

    private static String trim(double v) {
      double r = Math.round(v * 100) / 100.0;
      return r == Math.rint(r) ? Long.toString((long) r) : String.format(Locale.ROOT, "%.2f", r);
    }
  }

  private static final String N = "\\(?(-?\\d+(?:\\.\\d+)?)(?:-(-?\\d+(?:\\.\\d+)?))?\\)?";

  private static final Pattern BURN =
      Pattern.compile("^" + N + "% (increased|reduced) Mana Burn rate$", Pattern.CASE_INSENSITIVE);

  private static final Pattern COOLDOWN =
      Pattern.compile(
          "^" + N + "% (increased|reduced) Cooldown Recovery Rate$", Pattern.CASE_INSENSITIVE);

  public static Result apply(double manaBurn, double cooldown, List<String> lines) {
    double[] burn = sum(BURN, lines);
    double[] cd = sum(COOLDOWN, lines);
    return new Result(divide(manaBurn, burn), divide(cooldown, cd));
  }

  /** 증가% 합(최소 · 최대 · 있었는지). */
  private static double[] sum(Pattern p, List<String> lines) {
    double lo = 0;
    double hi = 0;
    boolean any = false;
    if (lines != null) {
      for (String line : lines) {
        Matcher m = p.matcher(line.trim());
        if (!m.matches()) {
          continue;
        }
        double a = Double.parseDouble(m.group(1));
        double b = m.group(2) == null ? a : Double.parseDouble(m.group(2));
        double sign = m.group(3).equalsIgnoreCase("reduced") ? -1 : 1;
        lo += sign * a;
        hi += sign * b;
        any = true;
      }
    }
    return new double[] {lo, hi, any ? 1 : 0};
  }

  private static PoeFlaskProps.Range divide(double base, double[] inc) {
    double x = base / (1 + inc[0] / 100);
    double y = base / (1 + inc[1] / 100);
    return new PoeFlaskProps.Range(Math.min(x, y), Math.max(x, y), inc[2] > 0);
  }
}
