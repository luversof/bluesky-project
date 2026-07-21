package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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
      @RequestParam(required = false, defaultValue = "dps") String objective,
      @RequestParam(required = false, defaultValue = "Pinnacle") String scenario,
      @RequestParam(required = false, defaultValue = "false") boolean buffs,
      @RequestParam(required = false, defaultValue = "") String className,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String uniques,
      @RequestParam(required = false, defaultValue = "") String skills) {
    return poeOptimizeService.start(
        slug, objective, scenario, buffs, className, ascendancy, uniques, skills);
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
}
