package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.PoeOptimizeResult;
import net.luversof.api.poe.service.PoeOptimizeService;

/** 최적 조합 탐색 잡 API — 시작/상태/결과. 로그인 게이팅은 게이트가 담당(여긴 잡만 구동). */
@RestController
@RequestMapping(value = "/api/poe/optimize", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeOptimizeController {

  private final PoeOptimizeService poeOptimizeService;

  public PoeOptimizeController(PoeOptimizeService poeOptimizeService) {
    this.poeOptimizeService = poeOptimizeService;
  }

  /**
   * poe.ninja 실빌드 벤치마크 조회 — 결과 페이지에서 최적화 결과를 실빌드 중앙값과 비교 표시하는 용도. 정확 키(전직+스킬) 우선, 없으면 스킬 폴백. 데이터
   * 없으면 null(게이트가 미표시).
   */
  @GetMapping("/archetype")
  public PoeOptimizeService.ArchetypeBenchmark archetype(
      @RequestParam(required = false, defaultValue = "") String skill,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      // 멀티스킬 조합(콤마 구분 젬 이름) — 지정 시 그 스킬 **전부** 쓰는 캐릭터만 즉석 집계(단일이면 기존 경로)
      @RequestParam(required = false, defaultValue = "") String skills) {
    if (!skills.isBlank()) {
      return poeOptimizeService.ninjaComboBenchmark(ascendancy, List.of(skills.split(",")));
    }
    return poeOptimizeService.ninjaBenchmark(ascendancy, skill);
  }

  /** 실행 중인 최적 조합 탐색 잡 중지 — 취소 요청 성공(실행 중이었음) 시 true. */
  @PostMapping("/stop")
  public boolean stop() {
    return poeOptimizeService.cancel();
  }

  /** 진행 상태 + (완료 시) 결과 스냅샷 */
  public record OptimizeStatus(
      boolean available,
      boolean running,
      String status,
      String phase,
      int phaseDone,
      int phaseTotal,
      int evalCount,
      List<String> logLines,
      PoeOptimizeResult result) {}

  @PostMapping("/start")
  public boolean start(
      @RequestParam(required = false, defaultValue = "") String slug,
      // 기본은 auto — 화면에서 목표 셀렉트를 없앤 뒤(2026-07-30) 사용자 경로는 auto(→balanced) 뿐이다.
      // 기본값이 dps 면 파라미터를 빼고 호출했을 때 없어진 모드(EHP 바닥 없는 유리대포)로 돌아간다.
      @RequestParam(required = false, defaultValue = "auto") String objective,
      @RequestParam(required = false, defaultValue = "Pinnacle") String scenario,
      @RequestParam(required = false, defaultValue = "false") boolean buffs,
      @RequestParam(required = false, defaultValue = "") String className,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String uniques,
      @RequestParam(required = false, defaultValue = "") String skills,
      @RequestParam(required = false, defaultValue = "") String treeNodes,
      @RequestParam(required = false, defaultValue = "") String masteries,
      @RequestParam(required = false, defaultValue = "") String jewels,
      @RequestParam(required = false, defaultValue = "") String clusters,
      @RequestParam(required = false, defaultValue = "") String tattoos,
      @RequestParam(required = false, defaultValue = "") String anoint,
      // 최근 결과 이력 저장 여부 — 사용자 실행은 true(기본), QA 배터리는 false 로 이력 오염 방지
      // 삿된(Foulborn) 후보 포함 여부 — 기본 켬. A/B 실측·회귀 배터리가 끄고 돌릴 수 있어야 "삿된이 실제로 이득인지"를 잰다.
      @RequestParam(required = false, defaultValue = "true") boolean foulborn,
      @RequestParam(required = false, defaultValue = "true") boolean saveHistory) {
    poeOptimizeService.setFoulbornEnabled(foulborn);
    return poeOptimizeService.start(
        slug,
        objective,
        scenario,
        buffs,
        className,
        ascendancy,
        uniques,
        skills,
        treeNodes,
        masteries,
        jewels,
        clusters,
        tattoos,
        anoint,
        saveHistory);
  }

  @GetMapping("/status")
  public OptimizeStatus status() {
    return new OptimizeStatus(
        poeOptimizeService.isAvailable(),
        poeOptimizeService.isRunning(),
        poeOptimizeService.lastStatus().name(),
        poeOptimizeService.phase(),
        poeOptimizeService.phaseDone(),
        poeOptimizeService.phaseTotal(),
        poeOptimizeService.evalCount(),
        poeOptimizeService.logTail(),
        poeOptimizeService.lastResult());
  }

  /** 최근 결과 목록(최신순) — 목록 표시용 요약만. 전체 결과는 {@code /result?id=} 로 조회. */
  @GetMapping("/history")
  public List<PoeOptimizeService.OptimizeHistoryEntry> history() {
    return poeOptimizeService.history();
  }

  /** 이력 결과 한 건 전체 조회 — 없거나 깨졌으면 null. */
  @GetMapping("/result")
  public PoeOptimizeResult result(@RequestParam long id) {
    return poeOptimizeService.historyResult(id);
  }

  /** 이력 한 건 삭제(사용자 요청). 삭제 성공 여부 반환. */
  @DeleteMapping("/history/{id}")
  public boolean deleteHistory(@PathVariable long id) {
    return poeOptimizeService.deleteHistory(id);
  }
}
