package net.luversof.web.gate.poe;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 아이템 툴팁 속성 칸(방어도 · 회피 · 에너지 보호막 · 수호 · 막기 · 물리 피해 · 초당 공격 · 치명타)에 그 아이템 자신의 로컬 옵션을 반영(10-08 C182,
 * PoE1 · PoE2 공용). 예전엔 베이스 값(+ PoE1 품질 20%)만 보여 해질녘 "방어도 (350-450)% 증가" 인데 "방어도 223", 메드베드의 도전 "수호
 * (30-50)% 증가" 인데 "수호 122" 처럼 인게임 툴팁과 달랐다.
 *
 * <p>규칙은 PoB Item.lua(인게임 계산 재현)와 같다 — 방어: round((베이스 + 고정) × (1 + 증가%/100) × (1 + 품질/100)),
 * "Armour and Evasion" 같은 겹친 옵션은 양쪽에, "Armour, Evasion and Energy Shield"(방어 전체)는 셋 모두에. 막기:
 * floor((베이스 + 고정) × (1 + 증가%/100)). 물리 피해: round((베이스 + 더함) × (1 + 증가%/100) × (1 + 품질/100)). 초당
 * 공격: round2(베이스 × (1 + 증가%/100)). 치명타: round2((베이스 + 고정) × (1 + 증가%/100)). 방어구가 아닌 아이템(장신구)의 방어 옵션
 * · 무기가 아닌 아이템의 공격 옵션은 전역이라 호출하는 쪽이 넘기지 않는다(kind). 롤 범위("(350-450)%")는 최소 · 최대를 따로 계산한다.
 */
public final class PoeLocalProps {

  private PoeLocalProps() {}

  /** 값 범위 — 정수 칸은 lo == hi 면 한 값. changed = 옵션으로 값이 바뀌었다(인게임은 파란 값). */
  public record Range(double lo, double hi, boolean changed) {

    public boolean present() {
      return hi > 0;
    }

    /** 정수 칸 글자 — "223" 또는 "1003-1226". */
    public String text() {
      return text("-");
    }

    /** 정수 칸 글자 — 범위 구분자를 고른다(PoE1 상세는 "–"). */
    public String text(String sep) {
      long a = Math.round(lo);
      long b = Math.round(hi);
      return a == b ? Long.toString(a) : a + sep + b;
    }

    /** 소수 칸 글자(초당 공격 · 치명타) — "1.50" 또는 "1.50-1.65". */
    public String text2() {
      String a = String.format(java.util.Locale.ROOT, "%.2f", lo);
      String b = String.format(java.util.Locale.ROOT, "%.2f", hi);
      return a.equals(b) ? a : a + "-" + b;
    }

    /** 피해 칸 한쪽 글자 — 범위면 괄호 "(52-60)" (최소–최대 사이의 "–" 와 섞이지 않게). */
    public String damageText() {
      long a = Math.round(lo);
      long b = Math.round(hi);
      return a == b ? Long.toString(a) : "(" + a + "-" + b + ")";
    }
  }

  /** 반영 결과. 무기 칸은 무기가 아니면 베이스 그대로. */
  public record Result(
      Range armour,
      Range evasion,
      Range energyShield,
      Range ward,
      Range block,
      Range physMin,
      Range physMax,
      Range attacksPerSecond,
      Range critChance,
      // 무기 로컬 원소 · 카오스 추가 피해(10-08 C183, "Adds X to Y Fire Damage") — 인게임 "원소 피해: …"(속성별 색) · "카오스
      // 피해: …"
      Dmg fire,
      Dmg cold,
      Dmg lightning,
      Dmg chaos) {

    /** 원소 피해 칸이 있는가(셋 중 하나라도). */
    public boolean hasElemental() {
      return fire.present() || cold.present() || lightning.present();
    }
  }

  /** 추가 피해 한 종류 — 최소 · 최대(각각 범위일 수 있다). 없으면 0. */
  public record Dmg(Range min, Range max) {

    public boolean present() {
      return max.present();
    }

    /**
     * "12-20" 또는 "(10-12)-(18-22)" — 인게임 범위 구분자는 붙임표(ClientStrings ItemDisplayWeaponDamageRange
     * "{0}-{1}", 10-09 C206).
     */
    public String text() {
      return min.damageText() + "-" + max.damageText();
    }
  }

  /** 베이스 값(범위 — PoE1 방어 베이스는 최소 · 최대가 있다). 없는 칸은 0. */
  public record Base(
      double armourLo,
      double armourHi,
      double evasionLo,
      double evasionHi,
      double energyShieldLo,
      double energyShieldHi,
      double wardLo,
      double wardHi,
      double block,
      double physMin,
      double physMax,
      double attacksPerSecond,
      double critChance) {}

  private static final Pattern FLAT_SPIRIT =
      Pattern.compile(
          "^\\+" + "\\(?(-?\\d+)(?:-(\\d+))?\\)?" + " to spirit$", Pattern.CASE_INSENSITIVE);

  private static final Pattern INC_SPIRIT =
      Pattern.compile(
          "^\\(?(-?\\d+)(?:-(\\d+))?\\)?% (increased|reduced) spirit$", Pattern.CASE_INSENSITIVE);

  /**
   * PoE2 셉터 정신력(10-08 C188) — PoB2 Item.lua: round((베이스 + 고정) × (1 + 증가%/100)). 베이스 정신력이 있는
   * 아이템(셉터)만 로컬이라 호출하는 쪽이 베이스가 있을 때만 부른다(목걸이 · 갑옷의 "+N to Spirit" 은 전역).
   */
  public static Range spirit(int base, List<String> lines) {
    Acc flat = new Acc(), inc = new Acc();
    for (String raw : lines == null ? List.<String>of() : lines) {
      if (raw == null) {
        continue;
      }
      Matcher m;
      if ((m = FLAT_SPIRIT.matcher(raw.trim())).matches()) {
        double[] v = range(m, 1);
        flat.add(v[0], v[1]);
      } else if ((m = INC_SPIRIT.matcher(raw.trim())).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        inc.add(v[0], v[1]);
      }
    }
    return scaled(base, base, flat, inc, 1, Rounding.ROUND);
  }

  private static final Pattern INC_RELOAD =
      Pattern.compile(
          "^\\(?(-?\\d+)(?:-(\\d+))?\\)?% (increased|reduced) reload speed$",
          Pattern.CASE_INSENSITIVE);

  /**
   * PoE2 쇠뇌 재장전 시간(초, 10-08 C190) — PoB2 Item.lua: round2(베이스 ÷ (1 + (재장전 속도 증가% + 공격 속도
   * 증가%)/100)). 무기에서만 로컬.
   */
  public static Range reload(double baseSeconds, List<String> lines) {
    Acc inc = new Acc();
    for (String raw : lines == null ? List.<String>of() : lines) {
      if (raw == null) {
        continue;
      }
      Matcher m;
      if ((m = INC_RELOAD.matcher(raw.trim())).matches()
          || (m = INC_APS.matcher(raw.trim())).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        inc.add(v[0], v[1]);
      }
    }
    // 증가가 클수록 짧다 — 짧은 쪽 = 최대 증가
    double a = Math.round(baseSeconds / (1 + inc.hi / 100) * 100) / 100.0;
    double b = Math.round(baseSeconds / (1 + inc.lo / 100) * 100) / 100.0;
    return new Range(Math.min(a, b), Math.max(a, b), inc.any);
  }

  /** 암시 · 고정 옵션 줄을 합친다(템플릿용, null 은 빈 목록). */
  public static List<String> join(List<String> implicits, List<String> explicits) {
    List<String> all = new java.util.ArrayList<>();
    if (implicits != null) {
      all.addAll(implicits);
    }
    if (explicits != null) {
      all.addAll(explicits);
    }
    return all;
  }

  private static final String N = "\\(?(-?\\d+(?:\\.\\d+)?)(?:-(\\d+(?:\\.\\d+)?))?\\)?";

  private static final Pattern FLAT_DEF =
      Pattern.compile(
          "^\\+"
              + N
              + " to (armour|evasion rating|maximum energy shield|ward|maximum runic ward|armour and evasion rating|armour and energy shield|evasion rating and energy shield)$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern INC_DEF =
      Pattern.compile(
          "^"
              + N
              + "% (increased|reduced) (armour|evasion rating|energy shield|ward|runic ward|armour and evasion|armour and energy shield|evasion and energy shield|armour, evasion and energy shield|armour, evasion or energy shield|global defences|defences)$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern FLAT_BLOCK =
      Pattern.compile(
          "^\\+" + N + "% (?:chance to block|to block chance)$", Pattern.CASE_INSENSITIVE);

  private static final Pattern INC_BLOCK =
      Pattern.compile(
          "^" + N + "% (increased|reduced) (?:chance to block|block chance)$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern ADD_PHYS =
      Pattern.compile("^adds " + N + " to " + N + " physical damage$", Pattern.CASE_INSENSITIVE);

  private static final Pattern ADD_ELE =
      Pattern.compile(
          "^adds " + N + " to " + N + " (fire|cold|lightning|chaos) damage$",
          Pattern.CASE_INSENSITIVE);

  private static final Pattern INC_PHYS =
      Pattern.compile("^" + N + "% (increased|reduced) physical damage$", Pattern.CASE_INSENSITIVE);

  private static final Pattern INC_APS =
      Pattern.compile("^" + N + "% (increased|reduced) attack speed$", Pattern.CASE_INSENSITIVE);

  private static final Pattern FLAT_CRIT =
      Pattern.compile(
          "^\\+" + N + "% to critical (?:strike|hit) chance$", Pattern.CASE_INSENSITIVE);

  private static final Pattern INC_CRIT =
      Pattern.compile(
          "^" + N + "% (increased|reduced) critical (?:strike|hit) chance$",
          Pattern.CASE_INSENSITIVE);

  /** 칸별 누적(최소 · 최대 따로). */
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
   * @param armourPiece 방어구 · 방패(방어 옵션이 로컬)
   * @param weapon 무기(공격 옵션이 로컬)
   * @param quality 품질 % — 방어 · 물리 피해에 곱한다(PoE1 고유 상세 20, PoE2 0, 빌드 아이템은 실제 품질)
   */
  public static Result apply(
      Base base, List<String> lines, boolean armourPiece, boolean weapon, int quality) {
    Acc aF = new Acc(), eF = new Acc(), sF = new Acc(), wF = new Acc();
    Acc aI = new Acc(), eI = new Acc(), sI = new Acc(), wI = new Acc();
    Acc bF = new Acc(), bI = new Acc();
    Acc pMinF = new Acc(),
        pMaxF = new Acc(),
        pI = new Acc(),
        apsI = new Acc(),
        cF = new Acc(),
        cI = new Acc();
    // 원소 · 카오스 추가 피해 — [종류][0 최소 · 1 최대]
    java.util.Map<String, Acc[]> ele = new java.util.LinkedHashMap<>();
    for (String t : List.of("fire", "cold", "lightning", "chaos")) {
      ele.put(t, new Acc[] {new Acc(), new Acc()});
    }
    for (String raw : lines == null ? List.<String>of() : lines) {
      if (raw == null) {
        continue;
      }
      String line = raw.trim();
      Matcher m;
      if (armourPiece && (m = FLAT_DEF.matcher(line)).matches()) {
        double[] v = range(m, 1);
        switch (m.group(3).toLowerCase(java.util.Locale.ROOT)) {
          case "armour" -> aF.add(v[0], v[1]);
          case "evasion rating" -> eF.add(v[0], v[1]);
          case "maximum energy shield" -> sF.add(v[0], v[1]);
          case "ward", "maximum runic ward" -> wF.add(v[0], v[1]);
          case "armour and evasion rating" -> {
            aF.add(v[0], v[1]);
            eF.add(v[0], v[1]);
          }
          case "armour and energy shield" -> {
            aF.add(v[0], v[1]);
            sF.add(v[0], v[1]);
          }
          default -> {
            eF.add(v[0], v[1]);
            sF.add(v[0], v[1]);
          }
        }
      } else if (armourPiece && (m = INC_DEF.matcher(line)).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        switch (m.group(4).toLowerCase(java.util.Locale.ROOT)) {
          case "armour" -> aI.add(v[0], v[1]);
          case "evasion rating" -> eI.add(v[0], v[1]);
          case "energy shield" -> sI.add(v[0], v[1]);
          case "ward", "runic ward" -> wI.add(v[0], v[1]);
          case "armour and evasion" -> {
            aI.add(v[0], v[1]);
            eI.add(v[0], v[1]);
          }
          case "armour and energy shield" -> {
            aI.add(v[0], v[1]);
            sI.add(v[0], v[1]);
          }
          case "evasion and energy shield" -> {
            eI.add(v[0], v[1]);
            sI.add(v[0], v[1]);
          }
          default -> {
            // 방어 전체 — PoB "Defences" 는 방어도 · 회피 · 에너지 보호막 · 수호 모두
            aI.add(v[0], v[1]);
            eI.add(v[0], v[1]);
            sI.add(v[0], v[1]);
            wI.add(v[0], v[1]);
          }
        }
      } else if (armourPiece && (m = FLAT_BLOCK.matcher(line)).matches()) {
        double[] v = range(m, 1);
        bF.add(v[0], v[1]);
      } else if (armourPiece && (m = INC_BLOCK.matcher(line)).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        bI.add(v[0], v[1]);
      } else if (weapon && (m = ADD_PHYS.matcher(line)).matches()) {
        double[] lo = range(m, 1);
        double[] hi = range(m, 3);
        pMinF.add(lo[0], lo[1]);
        pMaxF.add(hi[0], hi[1]);
      } else if (weapon && (m = ADD_ELE.matcher(line)).matches()) {
        double[] lo = range(m, 1);
        double[] hi = range(m, 3);
        Acc[] e = ele.get(m.group(5).toLowerCase(java.util.Locale.ROOT));
        e[0].add(lo[0], lo[1]);
        e[1].add(hi[0], hi[1]);
      } else if (weapon && (m = INC_PHYS.matcher(line)).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        pI.add(v[0], v[1]);
      } else if (weapon && (m = INC_APS.matcher(line)).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        apsI.add(v[0], v[1]);
      } else if (weapon && (m = FLAT_CRIT.matcher(line)).matches()) {
        double[] v = range(m, 1);
        cF.add(v[0], v[1]);
      } else if (weapon && (m = INC_CRIT.matcher(line)).matches()) {
        double[] v = signed(range(m, 1), m.group(3));
        cI.add(v[0], v[1]);
      }
    }
    double q = 1 + quality / 100.0;
    return new Result(
        defence(base.armourLo(), base.armourHi(), aF, aI, q),
        defence(base.evasionLo(), base.evasionHi(), eF, eI, q),
        defence(base.energyShieldLo(), base.energyShieldHi(), sF, sI, q),
        defence(base.wardLo(), base.wardHi(), wF, wI, q),
        scaled(base.block(), base.block(), bF, bI, 1, Rounding.FLOOR),
        scaled(base.physMin(), base.physMin(), pMinF, pI, q, Rounding.ROUND),
        scaled(base.physMax(), base.physMax(), pMaxF, pI, q, Rounding.ROUND),
        scaled(
            base.attacksPerSecond(), base.attacksPerSecond(), new Acc(), apsI, 1, Rounding.ROUND2),
        scaled(base.critChance(), base.critChance(), cF, cI, 1, Rounding.ROUND2),
        added(ele.get("fire")),
        added(ele.get("cold")),
        added(ele.get("lightning")),
        added(ele.get("chaos")));
  }

  /** 추가 피해 — 베이스 0 에 더한 값(로컬 증가는 드물어 반영하지 않는다, PoB 도 무기 원소 증가는 별도 로컬 옵션만). */
  private static Dmg added(Acc[] e) {
    return new Dmg(
        new Range(Math.round(e[0].lo), Math.round(e[0].hi), e[0].any),
        new Range(Math.round(e[1].lo), Math.round(e[1].hi), e[1].any));
  }

  private enum Rounding {
    ROUND,
    FLOOR,
    ROUND2
  }

  private static Range defence(double lo, double hi, Acc flat, Acc inc, double q) {
    return scaled(lo, hi, flat, inc, q, Rounding.ROUND);
  }

  private static Range scaled(double lo, double hi, Acc flat, Acc inc, double q, Rounding r) {
    double a = calc(lo + flat.lo, inc.lo, q, r);
    double b = calc(hi + flat.hi, inc.hi, q, r);
    return new Range(Math.min(a, b), Math.max(a, b), flat.any || inc.any);
  }

  private static double calc(double flat, double inc, double q, Rounding r) {
    double v = Math.max(0, flat * (1 + inc / 100.0) * q);
    return switch (r) {
      case ROUND -> Math.round(v);
      case FLOOR -> Math.floor(v);
      case ROUND2 -> Math.round(v * 100) / 100.0;
    };
  }

  private static double[] range(Matcher m, int g) {
    double a = Double.parseDouble(m.group(g));
    double b = m.group(g + 1) == null ? a : Double.parseDouble(m.group(g + 1));
    return new double[] {a, b};
  }

  private static double[] signed(double[] v, String dir) {
    return "reduced".equalsIgnoreCase(dir) ? new double[] {-v[1], -v[0]} : v;
  }
}
