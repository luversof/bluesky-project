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
    List<List<String>> qualityReminders,
    // 요구 능력치 비율 %(SkillGems.Strength/Dexterity/IntelligenceRequirementPercent — 10-09 C225, 게이트가
    // PoB
    // getGemStatRequirement 식으로 레벨별 수치를 낸다). 없으면 null
    Integer reqStrPct,
    Integer reqDexPct,
    Integer reqIntPct,
    // 자연 최대 레벨(API PoeGem 과 쌍 — PoB naturalMaxLevel: 각성 5 · 특출 보조 3 · 그 밖 20). 없으면 null
    Integer naturalMaxLevel) {

  /**
   * 표시 레벨(10-23 Q48) — 요청이 없으면 min(20, 자연 최대)(예전엔 늘 20 이라 각성 젬이 'Level 20 · Requires Level 100'),
   * 데이터에 있는 레벨 중 요청 이하에서 가장 높은 레벨로 맞춘다(데이터 끝이 20 미만인 젬이 빈 툴팁이던 것).
   */
  public int displayLevel(Integer requested) {
    int natural = naturalMaxLevel != null && naturalMaxLevel > 0 ? naturalMaxLevel : 20;
    int want = requested != null ? requested : Math.min(20, natural);
    if (levels == null || levels.isEmpty()) {
      return Math.max(want, 1);
    }
    int pick = levels.get(0).level();
    for (PoeGemLevel l : levels) {
      if (l.level() <= want && l.level() > pick) {
        pick = l.level();
      }
    }
    return pick;
  }

  /**
   * 목록 카드 레벨(10-23 Q99) — 인게임 젬 툴팁의 1레벨 요구 레벨(GrantedEffectsPerLevel). 예전엔 BaseItemTypes.DropLevel
   * 이라 엠파워 38(실제 1) · 질투 1(실제 60) · 화염구 5(실제 1) 처럼 151건이 달랐다. 레벨 표가 없으면 드롭 레벨.
   */
  public int listLevel() {
    return levels != null && !levels.isEmpty() ? levels.get(0).requiredLevel() : dropLevel;
  }

  /** 자연 최대 레벨인가 — 인게임 레벨 줄 '{0} (Max)' / '{0} (최대)'(ClientStrings ItemDisplaySkillGemMaxLevel). */
  public boolean isMaxLevel(int level) {
    return naturalMaxLevel != null && naturalMaxLevel > 0 && level == naturalMaxLevel;
  }
}
