package net.luversof.api.poe.service;

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
    // 마나 예약 상한에 걸려 못 띄운 오라 — 사용자가 "왜 오라가 이것뿐인지" 알 수 있게 부족 마나와 함께 노출
    List<BlockedAura> blockedAuras,
    List<SupportPick> additionalSkills,
    List<Integer> treeNodeIds,
    List<String> treeNotables,
    /** treeNotables 와 평행한 영문 이름 — 결과는 실행 시점에 저장되므로 화면에서 뒤늦게 번역할 수 없다. */
    List<String> treeNotablesEn,
    /** treeNotables 와 인덱스 평행 — 트리 focus 딥링크용 노드 id(클러스터 생성 노터블은 0=링크 없음) */
    List<Integer> treeNotableIds,
    List<SupportPick> jewels,
    List<ItemPick> items,
    // 캐릭터 속성이 모자라 실제로는 장착 불가능한 장비 — 조용히 넘어가면 게임에서 못 쓰는 빌드가 나온다
    List<UnmetRequirement> unmetRequirements,
    List<SlotTierCompare> tierComparisons,
    List<ScenarioCell> scenarioMatrix,
    List<DefenseHit> defenseHits,
    List<PoeBuild.PlayerStat> stats,
    String baselineValue,
    String finalValue,
    /** 어픽스 예산 축 — 레어 "필수 N개만 T1, 나머지 중위" 가정에서의 값(N=2/3/4). 레어가 없으면 빈 목록. */
    List<AffixBudgetPoint> affixBudget,
    String pobCode,
    long durationMs,
    int evalCount,
    // 결과를 트리 에디터로 되돌릴 때 필요한 것들 — 없으면 클러스터 생성 노드가 "존재하지 않는 id" 가 되고
    // 주얼도 빠져, 결과 화면의 수치와 트리 화면의 수치가 어긋난다.
    String treeClusters,
    String treeJewels,
    /** 트리 링크(tt=)로 되돌아갈 수 있게 남기는 문신 구성 "노드:영문명|…" */
    String treeTattoos,
    /** 트리 링크로 되돌아갈 마스터리 선택 "노드:효과,…" — 없으면 트리 화면이 마스터리 스탯 빠진 약한 트리를 보여준다 */
    String treeMasteries,
    /** 표시용 마스터리 요약("마스터리명 — 효과 첫 줄") — 자동 채택된 효과를 사용자가 결과에서 봐야 한다 */
    List<String> treeMasteryLabels,
    /** treeMasteryLabels 의 영문판(노드명 + 효과 첫 줄). */
    List<String> treeMasteryLabelsEn,
    /** 표시용 문신 요약("한글명 ×N") — treeTattoos 는 링크용 영문 dn 이라 그대로 보여줄 수 없다 */
    List<String> treeTattooLabels,
    /** treeTattooLabels 의 영문판("English name ×N"). */
    List<String> treeTattooLabelsEn,
    /** 트리 링크(an=)로 되돌아갈 도유 노터블 id — 없으면(null) 도유 미채택 */
    Integer treeAnoint,
    /**
     * P3 메타 기준선 게이트 — balanced 잡에서 최종 DPS·EHP 가 실빌드 중앙값을 **둘 다** 하회하면 true(지배당함, 결과 화면 경고). 비교
     * 불가(비-balanced/벤치 없음/구 히스토리)면 null.
     */
    Boolean belowMeta,
    /** 추가 스킬(화염덫 등) slug → 1b 패스가 선발한 전용 보조젬 — 표시용(계산 XML 에는 이미 링크됨). 구 히스토리 null. */
    java.util.Map<String, List<SupportPick>> additionalSkillSupports) {

  /**
   * 보조젬·추가 스킬·주얼 공통 표시 항목.
   *
   * <p>{@code lines}/{@code linesEn} 은 <b>제작 레어 주얼</b>용이다. 유니크는 slug 로 상세/툴팁을 띄우지만 제작 주얼은 붙일 상세가 없어
   * 화면에 이름만 뜨고 어떤 모드가 붙었는지 알 수 없었다 — 계산에는 생명/원소 피해 접두가 실제로 들어가 있는데 표시만 비어 있어 "속성이 표기 안 된다"는 보고가
   * 나왔다. 보조젬 등 나머지 용도는 3-인자 생성자로 빈 목록을 쓴다.
   */
  public record SupportPick(
      String slug, String name, String nameKo, List<String> lines, List<String> linesEn) {

    public SupportPick(String slug, String name, String nameKo) {
      this(slug, name, nameKo, List.of(), List.of());
    }
  }

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
   * @param modTiers modLines 와 1:1 정렬된 티어 라벨("T{순위}/{총티어}") — 티어 없는 줄(임플리싯/도유/명중가정/유니크)은 빈 문자열. 레어
   *     익스플리싯만 채워진다(인게임 Alt 고급 모드 설명의 티어 파리티).
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
      List<String> modTiers,
      // 거래소 검색 쿼리(q JSON, 레어 전용) — 베이스(한글명)+스탯 필터(min=티어 최저 롤). null 이면 링크 없음.
      String tradeQuery,
      // 실속형 쿼리 — 필수(픽 우선순위 상위) 모드만, min=2티어 최저 롤(T2 이상 매물). null 이면 링크 없음.
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

  /** 어픽스 예산 축의 한 점 — 레어 필수 essentialCount 개를 T1 로 잡았을 때의 포맷된 값. */
  public record AffixBudgetPoint(int essentialCount, String value) {}
}
