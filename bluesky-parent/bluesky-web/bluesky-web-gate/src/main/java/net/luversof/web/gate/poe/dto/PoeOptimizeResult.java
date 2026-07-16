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
    List<Integer> treeNodeIds,
    List<String> treeNotables,
    List<SupportPick> jewels,
    List<ItemPick> items,
    List<SlotTierCompare> tierComparisons,
    List<ScenarioCell> scenarioMatrix,
    List<DefenseHit> defenseHits,
    List<PoeBuild.PlayerStat> stats,
    String baselineValue,
    String finalValue,
    String pobCode,
    long durationMs,
    int evalCount) {

  public record SupportPick(String slug, String name, String nameKo) {}

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
      List<String> modLines) {}

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
}
