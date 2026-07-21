package net.luversof.web.gate.poe.dto;

import java.util.List;

/**
 * 트리 에디터에서 찍은 트리를 PoB 엔진으로 실계산한 결과 — API {@code /api/poe/build/tree-stats} 응답 매핑용.
 *
 * <p>최적화기와 달리 탐색이 없어 엔진 1회 호출로 끝난다(장비/보조젬 없음 = 순수 트리 기여분).
 */
public record PoeTreeEvaluation(
    String className,
    String classNameKo,
    String ascendancy,
    String gemName,
    String gemNameKo,
    int nodeCount,
    List<PoeBuild.PlayerStat> stats,
    String pobCode,
    long durationMs) {}
