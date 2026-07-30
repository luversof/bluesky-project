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
    List<String> topKeystones,
    List<String> topCoSkills,
    // 실빌드 성향: dps(공격특화·저EHP) | ehp(생존특화·고EHP) | balanced(균형). 구 히스토리엔 없어 null 가능.
    String lean) {}
