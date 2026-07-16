package net.luversof.web.gate.poe.dto;

import java.util.List;

/** 헤드리스 엔진 재계산 결과 — API {@code /api/poe/build/recalculate} 응답 매핑용. */
public record EngineResult(List<PoeBuild.PlayerStat> stats, long durationMs) {}
