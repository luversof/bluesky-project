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
      @RequestParam(required = false) String masteries,
      @RequestParam(required = false) String jewels,
      @RequestParam(required = false) String clusters,
      @RequestParam(required = false) String tattoos) {
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
    // jewels = "소켓노드:유니크slug,..." — 소켓 노드가 할당돼 있어야만 실제 장착된다
    java.util.Map<Integer, String> jewelSlugs = new java.util.LinkedHashMap<>();
    if (jewels != null && !jewels.isBlank()) {
      for (String pair : jewels.split(",")) {
        String[] kv = pair.trim().split(":", 2);
        if (kv.length == 2 && kv[0].matches("\\d+") && !kv[1].isBlank()) {
          jewelSlugs.put(Integer.valueOf(kv[0]), kv[1].trim());
        }
      }
    }
    // clusters = "소켓:크기:노드수:스킬키,..." — 프론트가 생성한 서브트리를 PoB 도 같게 만들려면 주얼 자체가 필요하다
    java.util.List<PoeOptimizeService.ClusterSpec> clusterSpecs = new java.util.ArrayList<>();
    if (clusters != null && !clusters.isBlank()) {
      for (String entry : clusters.split(",")) {
        String[] parts = entry.trim().split(":");
        if (parts.length >= 3 && parts[0].matches("[0-9]+") && parts[2].matches("[0-9]+")) {
          // 5번째 필드 = 노터블 이름들('|' 구분). 이름은 PoB 가 트리에서 찾는 영문 원문 그대로여야 한다.
          java.util.List<String> notables =
              parts.length > 4 && !parts[4].isBlank()
                  ? java.util.Arrays.stream(parts[4].split("\\|"))
                      .map(String::trim)
                      .filter(s -> !s.isEmpty())
                      .toList()
                  : java.util.List.of();
          clusterSpecs.add(
              new PoeOptimizeService.ClusterSpec(
                  Integer.parseInt(parts[0]),
                  parts[1],
                  Integer.parseInt(parts[2]),
                  parts.length > 3 ? parts[3] : "",
                  notables,
                  // 6번째 필드 = 주얼 소켓 수(중첩 클러스터를 꽂을 자리)
                  parts.length > 5 && parts[5].matches("[0-9]+") ? Integer.parseInt(parts[5]) : 0));
        }
      }
    }
    // tattoos = "노드:문신영문명,..." — 그 패시브를 문신 노드로 통째 교체한다(할당된 노드만 유효)
    java.util.Map<Integer, String> tattooDns = new java.util.LinkedHashMap<>();
    if (tattoos != null && !tattoos.isBlank()) {
      for (String pair : tattoos.split(",")) {
        String[] kv = pair.trim().split(":", 2);
        if (kv.length == 2 && kv[0].matches("[0-9]+") && !kv[1].isBlank()) {
          tattooDns.put(Integer.valueOf(kv[0]), kv[1].trim());
        }
      }
    }
    try {
      return poeOptimizeService.evaluateTree(
          classId, ascendancy, nodeIds, gem, masteryEffects, jewelSlugs, clusterSpecs, tattooDns);
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
