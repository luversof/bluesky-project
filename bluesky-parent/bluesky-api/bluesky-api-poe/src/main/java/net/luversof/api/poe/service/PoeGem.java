package net.luversof.api.poe.service;

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
    // 레벨 statReminders 의 Id → 영 · 한 문구(이 젬이 쓰는 것만, 10-04 C115). 없으면 null
    java.util.Map<String, PoeReminderText> reminderText,
    // qualityStatLines 와 같은 길이의 줄별 리마인더 Id(10-04 C120). 없으면 null
    List<List<String>> qualityReminders) {

  /** 레벨별 데이터를 뺀 사본 — 이름 · 태그만 쓰는 고르기 목록용(10-02: 액티브 젬 전체가 15.6MB · 0.6초였다). */
  public PoeGem withoutLevels() {
    return new PoeGem(
        id,
        slug,
        name,
        nameKo,
        isSupport,
        isVaal,
        soulPreventionSeconds,
        storedUses,
        color,
        dropLevel,
        requiresStr,
        requiresDex,
        requiresInt,
        castTimeMs,
        description,
        descriptionKo,
        tags,
        tagsKo,
        qualityStatLines,
        qualityStatLinesKo,
        List.of(),
        null,
        null);
  }
}
