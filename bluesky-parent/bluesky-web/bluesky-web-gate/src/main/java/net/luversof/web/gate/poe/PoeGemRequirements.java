package net.luversof.web.gate.poe;

/**
 * 젬 요구 능력치(10-09 C225) — PoB CalcTools.lua {@code getGemStatRequirement}: round((20 + (요구 레벨 - 3) ×
 * 3) × (비율 / 100)^0.9 × 계수), 계수 액티브 0.7 · 보조 0.5, 14 미만이면 0(요구 없음). 회오리바람 70레벨(힘 40 · 민첩 60%) = 68
 * 힘 · 98 민첩(poedb 와 같음).
 */
public final class PoeGemRequirements {

  private PoeGemRequirements() {}

  /**
   * PoE2 — PoB2 CalcTools.lua getGemStatRequirement: round((5 + (요구 레벨 - 3) × 1.7) × (비율 /
   * 100)^0.9) + 4, 보조 젬 · 8 미만은 0(10-09 C227). 얼음 화살 90레벨(민첩 100%) = 157(poe2db 와 같음).
   */
  public static int poe2Stat(int requiredLevel, boolean support, Integer pct) {
    if (pct == null || pct <= 0 || support) {
      return 0;
    }
    long req = Math.round((5 + (requiredLevel - 3) * 1.7) * Math.pow(pct / 100.0, 0.9)) + 4;
    return req < 8 ? 0 : (int) req;
  }

  public static int stat(int requiredLevel, boolean support, Integer pct) {
    if (pct == null || pct <= 0) {
      return 0;
    }
    double type = support ? 0.5 : 0.7;
    long req = Math.round((20 + (requiredLevel - 3) * 3) * Math.pow(pct / 100.0, 0.9) * type);
    return req < 14 ? 0 : (int) req;
  }
}
