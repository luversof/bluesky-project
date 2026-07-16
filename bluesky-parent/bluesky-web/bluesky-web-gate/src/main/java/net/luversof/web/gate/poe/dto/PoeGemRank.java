package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 시뮬레이터 젬 DPS 랭킹 한 건 (gem-ranking.json). 템플릿 빌드(레벨 90 사이온, 트리/지원 젬 없음, 태그별 표준 무기)에 젬 하나를 끼워 PoB
 * 엔진으로 계산한 기준 성능이다 — 절대값이 아니라 상대 비교용.
 */
public record PoeGemRank(
    String slug,
    String name,
    String nameKo,
    String color,
    List<String> tagsKo,
    String weapon,
    double dps,
    double averageDamage,
    double speed) {}
