package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * poe.ninja 실빌드 아키타입 벤치마크 — 최적화 결과를 실빌드 중앙값과 비교 표시하는 용도. bluesky-api-poe {@code
 * /api/poe/optimize/archetype} 응답과 필드명 일치.
 */
public record ArchetypeBenchmark(
    String ascendancy,
    String mainSkill,
    int sample,
    long life,
    long energyShield,
    long ehp,
    long dps,
    long physicalMax,
    long fireMax,
    long coldMax,
    long lightningMax,
    long chaosMax,
    int fireRes,
    int coldRes,
    int lightningRes,
    int chaosRes,
    // 생명 재생/초(RF 등 지속형 생존 핵심) + 방어층(armour/evasion/block/suppress) + 최약 최대피격. 구 히스토리엔 없어 0.
    long lifeRegen,
    long armour,
    long evasion,
    int block,
    int suppress,
    long lowestMax,
    // 전 컬럼 참조(스샷 전 컬럼) — 부가 자원/속성/충전/주문막기·회피/물리피해전환/클러스터주얼·유니크·미러 개수. 구 히스토리엔 없어 0.
    long ward,
    long mana,
    int itemRarity,
    int movementSpeed,
    int spellBlock,
    int spellDodge,
    int physTakenAs,
    int str,
    int dex,
    int intel,
    int enduranceCharges,
    int frenzyCharges,
    int powerCharges,
    int clusterJewels,
    int largeCluster,
    int mediumCluster,
    int smallCluster,
    int uniqueEquip,
    int mirroredItems,
    int mirroredWeapons,
    int mirroredArmours,
    List<String> topKeystones,
    List<String> topCoSkills,
    // 실빌드 마스터리(효과 텍스트) — 캐릭터 상세 JSON 채집. 구 데이터엔 없어 null.
    List<String> topMasteries,
    // 실빌드 성향: dps(공격특화·저EHP) | ehp(생존특화·고EHP) | balanced(균형). 구 히스토리엔 없어 null 가능.
    String lean,
    // 전 컬럼 특화 판정 키 목록(dps/ehp/life/es/liferegen/armour/evasion/block/suppress/maxres). 구 히스토리엔 없어
    // null.
    List<String> specializations,
    // 멀티스킬 조합 벤치의 스킬별 전용 DPS 중앙값 — dps 필드는 메인(첫 선택) 스킬 전용. 단일 스킬 벤치에선 null.
    List<SkillDpsEntry> skillDps,
    // 패싯(poe.ninja 검색 사이드바 집계) — 전체 모집단 기준. facetTotal = % 분모. 구 데이터엔 없어 0/null.
    long facetTotal,
    java.util.Map<String, List<FacetEntry>> facets) {

  /** 조합 벤치 스킬별 전용 DPS — count = 해당 스킬 전용 DPS 를 보유한 표본 수. */
  public record SkillDpsEntry(String name, long dps, int count) {}

  /** 패싯 항목 — 모집단 중 count 명이 사용. */
  public record FacetEntry(String name, int count, String nameKo) {}
}
