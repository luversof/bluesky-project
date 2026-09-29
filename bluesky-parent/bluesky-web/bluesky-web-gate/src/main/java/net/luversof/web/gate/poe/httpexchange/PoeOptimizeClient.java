package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeJobStatus;
import net.luversof.web.gate.poe.dto.PoeOptimizeResult;

/** bluesky-api-poe 최적 조합 탐색 잡 클라이언트. */
@HttpExchange(url = "/api/poe/optimize", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeOptimizeClient {

  /** 폼 본문으로 보낸다 — 확정 트리·용병 코드처럼 긴 값이 URL 쿼리에 실리면 API 헤더 한도(8KB)에 걸린다(PoeBuildClient 주석). */
  @PostExchange(value = "/start", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
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
      @RequestParam(required = false) String anoint,
      // 완주 모드(opt-in) — 혈맹 top-3 을 각각 끝까지 완주시켜 최선만 발행(최대 4 배 소요)
      @RequestParam(required = false) Boolean thorough,
      // 루미너리 용병 빌드 PoB 코드(선택) — 후보가 루미너리면 용병 오라·저주를 파티 탭으로 넣는다
      @RequestParam(required = false) String mercCode);

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

  @DeleteExchange("/history/{id}")
  boolean deleteHistory(@PathVariable long id);

  /** poe.ninja 실빌드 벤치마크(결과 비교 표시용) — 데이터 없으면 null. */
  @GetExchange("/archetype")
  net.luversof.web.gate.poe.dto.ArchetypeBenchmark archetype(
      @RequestParam String skill, @RequestParam String ascendancy);

  /** 멀티스킬 조합 벤치마크 — skills=콤마 젬 이름 목록, 그 스킬 전부 쓰는 캐릭터만 집계. 데이터 없으면 null. */
  @GetExchange("/archetype")
  net.luversof.web.gate.poe.dto.ArchetypeBenchmark archetypeCombo(
      @RequestParam String skills, @RequestParam String ascendancy);
}
