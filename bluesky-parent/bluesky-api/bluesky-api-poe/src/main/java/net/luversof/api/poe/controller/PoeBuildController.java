package net.luversof.api.poe.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.poe.service.PoeBuild;
import net.luversof.api.poe.service.PoeOptimizeService;
import net.luversof.api.poe.service.PoePobEngineService;
import net.luversof.api.poe.service.PoePobImportService;

/** PoB 공유 코드 임포트 + 헤드리스 엔진 재계산 API. */
@RestController
@RequestMapping(value = "/api/poe/build", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeBuildController {

  private final PoePobImportService poePobImportService;
  private final PoePobEngineService poePobEngineService;
  private final PoeOptimizeService poeOptimizeService;

  public PoeBuildController(
      PoePobImportService poePobImportService,
      PoePobEngineService poePobEngineService,
      PoeOptimizeService poeOptimizeService) {
    this.poePobImportService = poePobImportService;
    this.poePobEngineService = poePobEngineService;
    this.poeOptimizeService = poeOptimizeService;
  }

  /** PoB 코드 → 빌드 요약. 형식 오류는 400. */
  @PostMapping("/import")
  public PoeBuild importBuild(@RequestParam String code) {
    try {
      return poePobImportService.importCode(code);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /** 헤드리스 엔진(LuaJIT+PoB) 가용 여부 — 게이트 admin/빌드 화면 표시용. */
  @GetMapping("/available")
  public boolean available() {
    return poePobEngineService.isAvailable();
  }

  /** 트리 에디터에서 찍은 노드 그대로 실계산 — 탐색 없이 엔진 1회. */
  @PostMapping("/tree-stats")
  public PoeOptimizeService.TreeEvaluation treeStats(
      @RequestParam(defaultValue = "0") int classId,
      @RequestParam(required = false) String ascendancy,
      @RequestParam String nodes,
      @RequestParam(required = false) String gem,
      @RequestParam(required = false) String masteries) {
    java.util.Set<Integer> nodeIds =
        java.util.Arrays.stream(nodes.split(","))
            .map(String::trim)
            .filter(s -> s.matches("\\d+"))
            .map(Integer::valueOf)
            .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    if (nodeIds.isEmpty()) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, "nodes 가 비어 있습니다");
    }
    // masteries = "노드:효과,노드:효과" — 마스터리는 고른 효과까지 넘겨야 PoB 계산에 반영된다
    java.util.Map<Integer, Integer> masteryEffects = new java.util.LinkedHashMap<>();
    if (masteries != null && !masteries.isBlank()) {
      for (String pair : masteries.split(",")) {
        String[] kv = pair.trim().split(":");
        if (kv.length == 2 && kv[0].matches("\\d+") && kv[1].matches("\\d+")) {
          masteryEffects.put(Integer.valueOf(kv[0]), Integer.valueOf(kv[1]));
        }
      }
    }
    try {
      return poeOptimizeService.evaluateTree(classId, ascendancy, nodeIds, gem, masteryEffects);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /** PoB 코드 → 헤드리스 엔진 실계산 스탯. */
  @PostMapping("/recalculate")
  public PoePobEngineService.EngineResult recalculate(@RequestParam String code) {
    try {
      String buildXml = poePobImportService.decodeToXml(code);
      return poePobEngineService.recalculate(buildXml);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
