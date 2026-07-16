package net.luversof.web.gate.poe.dto;

/** 스킬젬의 레벨별 수치 한 줄 (skill-gems.json levels[]). */
public record PoeGemLevel(
    int level,
    int requiredLevel,
    Integer cost,
    String costType,
    Integer costMultiplier,
    Integer cooldownMs,
    Double critChance,
    Double damageEffectiveness,
    Double baseMultiplier,
    java.util.List<String> statLines,
    java.util.List<String> statLinesKo) {}
