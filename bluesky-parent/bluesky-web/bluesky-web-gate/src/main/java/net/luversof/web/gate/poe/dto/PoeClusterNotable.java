package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 클러스터 주얼 주요 스킬 — API {@code /api/poe/tree/cluster-notables} 응답 매핑용(브라우징 페이지). */
public record PoeClusterNotable(
    String name,
    String nameKo,
    List<String> stats,
    List<String> statsKo,
    String icon,
    // 핵심노드(10-23 Q75 — 주요 스킬과 섞여 보였다). 아니면 null
    Boolean keystone) {}
