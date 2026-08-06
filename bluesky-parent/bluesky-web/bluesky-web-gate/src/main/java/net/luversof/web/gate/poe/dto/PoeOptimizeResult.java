package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 최적 조합 탐색 결과 (sim/optimize-last.json 으로 영속, 재시작 후에도 마지막 결과 표시). */
public record PoeOptimizeResult(
    String gemSlug,
    String gemName,
    String gemNameKo,
    String objective,
    String scenario,
    String scenarioKo,
    boolean combatBuffs,
    String className,
    String classNameKo,
    String ascendancy,
    String ascendancyKo,
    String bloodline,
    String bloodlineKo,
    List<SupportPick> supports,
    List<SupportPick> auras,
    // 마나 예약 상한에 걸려 못 띄운 오라(부족 마나 포함) — 결과 화면 설명용
    List<BlockedAura> blockedAuras,
    List<SupportPick> additionalSkills,
    List<Integer> treeNodeIds,
    List<String> treeNotables,
    List<Integer> treeNotableIds,
    List<SupportPick> jewels,
    List<ItemPick> items,
    // 속성 부족으로 실제 장착이 불가능한 장비
    List<UnmetRequirement> unmetRequirements,
    List<SlotTierCompare> tierComparisons,
    List<ScenarioCell> scenarioMatrix,
    List<DefenseHit> defenseHits,
    List<PoeBuild.PlayerStat> stats,
    String baselineValue,
    String finalValue,
    /** 어픽스 예산 축 — 레어 "필수 N개만 T1, 나머지 중위"(N=2/3/4) 값. 레어 없으면 빈 목록. */
    java.util.List<AffixBudgetPoint> affixBudget,
    String pobCode,
    long durationMs,
    int evalCount,
    // 결과 → 트리 에디터 왕복용(클러스터 c= / 주얼 j= 형식 문자열)
    String treeClusters,
    String treeJewels,
    /** 트리 링크(tt=)로 되돌아갈 문신 구성 "노드:영문명|…" */
    String treeTattoos,
    /** 트리 링크용 마스터리 선택 "노드:효과,…" */
    String treeMasteries,
    /** 표시용 마스터리 요약("마스터리명 — 효과 첫 줄") */
    java.util.List<String> treeMasteryLabels,
    /** 표시용 문신 요약("한글명 ×N") */
    java.util.List<String> treeTattooLabels,
    Integer treeAnoint,
    // P3 메타 기준선 게이트 — balanced 최종치가 실빌드 중앙값(DPS·EHP) 둘 다 하회하면 true. 구 히스토리는 null.
    Boolean belowMeta,
    /** 추가 스킬 slug → 전용 보조젬(1b 선발) — 표시용. 구 히스토리 null. */
    java.util.Map<String, List<SupportPick>> additionalSkillSupports) {

  public record SupportPick(String slug, String name, String nameKo) {}

  /** 예약 초과로 제외된 오라. shortfall = 부족한 마나(양수). */
  public record BlockedAura(String name, String nameKo, int shortfall) {}

  /** 장착 요구 속성 미달 (attribute = str|dex|int) */
  public record UnmetRequirement(
      String name, String nameKo, String attribute, int required, int actual) {}

  /**
   * 장착 아이템 하나.
   *
   * @param rarity UNIQUE | RARE
   * @param slug 유니크면 상세 링크용 slug, 레어면 null
   * @param modLines 레어면 한국어 모드 라인, 유니크면 빈 목록
   */
  public record ItemPick(
      String slot,
      String slotKo,
      String rarity,
      String slug,
      String name,
      String nameKo,
      List<String> modLines,
      List<String> modLinesEn,
      // modLines 와 1:1 정렬된 티어 라벨("T{순위}/{총티어}"), 티어 없는 줄은 빈 문자열. 구 히스토리엔 없어 null 가능.
      List<String> modTiers,
      // 거래소 검색 쿼리(q JSON, 레어 전용 — daum 서버 기준). null/구 히스토리면 링크 미표시.
      String tradeQuery,
      // 실속형 쿼리 — 필수 모드만, min=2티어 최저 롤(T2 이상 매물). null/구 히스토리면 링크 미표시.
      String tradeQueryBudget) {}

  /** 레어 슬롯의 티어별 성능 비교 (T1/중간/하위) */
  public record SlotTierCompare(String slot, String slotKo, List<TierRow> rows) {}

  public record TierRow(String label, String value) {}

  /**
   * 가정별 성능 매트릭스 한 행 — 적 시나리오 하나에 대해 전투 버프 off/on 두 상태의 DPS.
   *
   * @param scenario PoB enemyIsBoss 값 (None|Boss|Pinnacle|Uber)
   * @param dpsBuffOff 버프 미가정 DPS (포맷된 문자열)
   * @param dpsBuffOn 버프 가정 DPS (포맷된 문자열)
   */
  public record ScenarioCell(
      String scenario, String scenarioKo, String dpsBuffOff, String dpsBuffOn) {}

  /**
   * 유형별 최대 피격 생존 — 해당 데미지 유형의 단일 히트를 얼마까지 버티는지(PoB MaximumHitTaken).
   *
   * @param type physical|fire|cold|lightning|chaos (색상/라벨 키)
   * @param value 포맷된 최대 피격량
   */
  public record DefenseHit(String type, String value) {}

  /** 어픽스 예산 축의 한 점 — 레어 필수 essentialCount 개를 T1 로 잡았을 때의 값. */
  public record AffixBudgetPoint(int essentialCount, String value) {}
}
