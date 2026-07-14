package net.luversof.web.gate.poe.service;

import java.util.List;

/** 최적 조합 탐색 결과 (sim/optimize-last.json 으로 영속, 재시작 후에도 마지막 결과 표시). */
public record PoeOptimizeResult(
    String gemSlug,
    String gemName,
    String gemNameKo,
    String objective,
    String className,
    String classNameKo,
    List<SupportPick> supports,
    List<Integer> treeNodeIds,
    List<String> treeNotables,
    List<ItemPick> items,
    List<PoeBuild.PlayerStat> stats,
    String baselineValue,
    String finalValue,
    String pobCode,
    long durationMs,
    int evalCount) {

  public record SupportPick(String slug, String name, String nameKo) {}

  public record ItemPick(String slot, String slotKo, String slug, String name, String nameKo) {}
}
