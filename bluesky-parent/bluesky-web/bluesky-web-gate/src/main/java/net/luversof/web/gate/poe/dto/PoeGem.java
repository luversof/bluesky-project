package net.luversof.web.gate.poe.dto;

import java.util.List;

/** tools/poe-extract 파이프라인이 생성한 표시용 스킬젬 한 건 (resources/poe/skill-gems.json). */
public record PoeGem(
    String id,
    String slug,
    String name,
    String nameKo,
    boolean isSupport,
    boolean isVaal,
    Double soulPreventionSeconds,
    Integer storedUses,
    String color,
    int dropLevel,
    boolean requiresStr,
    boolean requiresDex,
    boolean requiresInt,
    Integer castTimeMs,
    String description,
    String descriptionKo,
    List<String> tags,
    List<String> tagsKo,
    List<String> qualityStatLines,
    List<String> qualityStatLinesKo,
    List<PoeGemLevel> levels,
    // 레벨 statReminders 의 Id → 영 · 한 문구(10-04 C115, API 와 쌍). 없으면 null
    java.util.Map<String, PoeReminderText> reminderText,
    // qualityStatLines 와 같은 길이의 줄별 리마인더 Id(10-04 C120). 없으면 null
    List<List<String>> qualityReminders) {}
