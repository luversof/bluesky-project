package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 클러스터 주얼 노터블 — API {@code /api/poe/tree/cluster-notables} 응답 매핑용(브라우징 페이지). */
public record PoeClusterNotable(
    String name, String nameKo, List<String> stats, List<String> statsKo, String icon) {}
