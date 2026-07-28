package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeJobStatus;
import net.luversof.web.gate.poe.dto.PoeOptimizeResult;

/** bluesky-api-poe 최적 조합 탐색 잡 클라이언트. */
@HttpExchange(url = "/api/poe/optimize", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeOptimizeClient {

  @PostExchange("/start")
  boolean start(
      @RequestParam String slug,
      @RequestParam(required = false) String objective,
      @RequestParam(required = false) String scenario,
      @RequestParam(required = false) Boolean buffs,
      @RequestParam(required = false) String className,
      @RequestParam(required = false) String ascendancy,
      @RequestParam(required = false) String uniques,
      @RequestParam(required = false) String skills,
      @RequestParam(required = false) String treeNodes,
      @RequestParam(required = false) String masteries,
      @RequestParam(required = false) String jewels,
      @RequestParam(required = false) String clusters,
      @RequestParam(required = false) String tattoos,
      @RequestParam(required = false) String anoint);

  @GetExchange("/status")
  PoeJobStatus.Optimize status();

  /** 실행 중인 잡 중지 — 실행 중이었으면 true. */
  @PostExchange("/stop")
  boolean stop();

  /** 최근 결과 목록(최신순, 목록 표시용 요약). */
  @GetExchange("/history")
  java.util.List<PoeJobStatus.OptimizeHistoryEntry> history();

  /** 이력 결과 한 건 전체 조회(id = 저장 시각 epochMs) — 없으면 null. */
  @GetExchange("/result")
  PoeOptimizeResult result(@RequestParam long id);

  /** poe.ninja 실빌드 벤치마크(결과 비교 표시용) — 데이터 없으면 null. */
  @GetExchange("/archetype")
  net.luversof.web.gate.poe.dto.ArchetypeBenchmark archetype(
      @RequestParam String skill, @RequestParam String ascendancy);
}
