package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 에센스 한 항목 — API {@code /api/poe/essences/for-item-class} 응답 매핑용. family(계열 키)로 묶어 접이식 카드, tier(클수록
 * 상위) 내림차순 티어 사다리로 표시한다.
 */
public record PoeEssenceEntry(
    String family,
    String name,
    String nameKo,
    /** 아이콘 상대 경로(essences/*.png) — /poe-assets/ 아래에서 서빙. 없으면 null. */
    String icon,
    int tier,
    int ilvlMax,
    String gen,
    String modName,
    List<String> en,
    List<String> enMin,
    List<String> ko,
    List<String> koMin) {}
