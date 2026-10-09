package net.luversof.web.gate.poe.dto;

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
    java.util.List<java.util.List<String>> statReminders) {}
