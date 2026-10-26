package net.luversof.api.poe.service;

/** 스킬젬의 레벨별 수치 한 줄 (skill-gems.json levels[]). */
public record PoeGemLevel(
    int level,
    int requiredLevel,
    Integer cost,
    String costType,
    Double reservation,
    Integer costMultiplier,
    Integer cooldownMs,
    Double critChance,
    Double damageEffectiveness,
    Double baseMultiplier,
    java.util.List<String> statLines,
    java.util.List<String> statLinesKo,
    // statLines 와 같은 길이의 줄별 리마인더 Id(젬 reminderText 사전 키, 10-04 C115). 없으면 null
    java.util.List<java.util.List<String>> statReminders,
    // 공격 속도 배율 %(인게임 "공격 속도: 기본 수치의 90%", 10-08 C201). 없으면 null
    Integer attackSpeedPct,
    // 바알 스킬 영혼 소모(인게임 "사용 시 영혼 소모: 50", PoB cost.Soul — 10-09 C234). 없으면 null
    Integer soulCost,
    // 재사용 사용 횟수(GrantedEffectsPerLevel.StoredUses — 인게임 "3.50초 (3회)", 10-23 Q49b). 재사용이 있고 2 이상일
    // 때만, 없으면 null
    Integer cooldownUses,
    // 고정 마나 점유(GrantedEffectsPerLevel.ManaReservationFlat — 명료함 · 정밀 · 활력 · 지뢰, 인게임 "점유: 마나 34",
    // 10-23 Q51). 없으면 null
    Integer reservationFlat) {

  /** 예전 인자 순서(시험 · 사본 생성용) — 재사용 사용 횟수 없음. */
  public PoeGemLevel(
      int level,
      int requiredLevel,
      Integer cost,
      String costType,
      Double reservation,
      Integer costMultiplier,
      Integer cooldownMs,
      Double critChance,
      Double damageEffectiveness,
      Double baseMultiplier,
      java.util.List<String> statLines,
      java.util.List<String> statLinesKo,
      java.util.List<java.util.List<String>> statReminders,
      Integer attackSpeedPct,
      Integer soulCost) {
    this(
        level,
        requiredLevel,
        cost,
        costType,
        reservation,
        costMultiplier,
        cooldownMs,
        critChance,
        damageEffectiveness,
        baseMultiplier,
        statLines,
        statLinesKo,
        statReminders,
        attackSpeedPct,
        soulCost,
        null,
        null);
  }
}
