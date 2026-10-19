package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.poe.service.PoeCraftDataService;
import net.luversof.api.poe.service.PoeCraftEngine;
import net.luversof.api.poe.service.PoeCraftService;

/** PoE1 제작 에뮬레이터 API(10-15 C335) — 아이템 상태를 그대로 주고받는다(서버는 상태를 들고 있지 않는다). */
@RestController
@RequestMapping(value = "/api/poe/craft", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeCraftController {

  /** 화폐 적용 요청 — essence = 에센스 슬러그(ESSENCE 일 때), seed = 고정 난수(시험용, 없으면 무작위). */
  public record ApplyRequest(
      PoeCraftEngine.Item item,
      PoeCraftEngine.Action action,
      String essence,
      Long seed,
      // 화폐 인자(10-15 C338) — 화석 슬러그 쉼표 목록 · 제작대 옵션 id · 수확 id · 정복자 영향력 키. 없으면 essence
      String arg) {}

  /** 옵션 풀 요청 — rarity = 그 희귀도로 볼 자리(없으면 일반 → 레어, 그 밖엔 지금 희귀도). */
  public record PoolRequest(PoeCraftEngine.Item item, PoeCraftEngine.Rarity rarity) {}

  private final PoeCraftService craft;
  private final net.luversof.api.poe.service.PoeCraftCalcService calc;
  private final net.luversof.api.poe.service.PoeCraftOptimizer optimizer;

  public PoeCraftController(
      PoeCraftService craft,
      net.luversof.api.poe.service.PoeCraftCalcService calc,
      net.luversof.api.poe.service.PoeCraftOptimizer optimizer) {
    this.craft = craft;
    this.calc = calc;
    this.optimizer = optimizer;
  }

  /**
   * 최적 제작 경로(10-19 C457 사용자 요청) — 에센스 · 제작대 · 짐승 · 화폐를 섞어 목표까지 성공 한 번당 최소 기대 비용과 상태별 수단(정책). 시간
   * 예산(budgetMs, 기본 8초) 안에서.
   */
  /** 최적 제작 경로 백그라운드 시작(C457k) — 작업 id. 기본 20초, 진행은 /optimize/{id}. */
  @PostMapping(value = "/optimize/start", consumes = MediaType.APPLICATION_JSON_VALUE)
  public java.util.Map<String, String> optimizeStart(
      @RequestBody net.luversof.api.poe.service.PoeCraftOptimizer.Request req) {
    check(req.item());
    return java.util.Map.of("id", optimizer.start(req));
  }

  @GetMapping("/optimize/{id}")
  public net.luversof.api.poe.service.PoeCraftOptimizer.JobStatus optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id) {
    net.luversof.api.poe.service.PoeCraftOptimizer.JobStatus s = optimizer.status(id);
    if (s == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "모르는 작업: " + id);
    }
    return s;
  }

  @PostMapping(value = "/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftOptimizer.Result optimize(
      @RequestBody net.luversof.api.poe.service.PoeCraftOptimizer.Request req) {
    check(req.item());
    return optimizer.optimize(req);
  }

  /** 계산기(10-15 C336) — 목표 옵션 묶음 → 방법별 성공 확률 · 기대 시도 · 기대 비용(카오스, 비용 순). */
  @PostMapping(value = "/calc", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.Response calc(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.Request req) {
    check(req.item());
    return calc.calc(req);
  }

  @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
  public PoeCraftService.View apply(@RequestBody ApplyRequest req) {
    check(req.item());
    if (req.action() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action 이 없습니다");
    }
    return craft.apply(
        req.item(), req.action(), req.arg() != null ? req.arg() : req.essence(), req.seed());
  }

  @PostMapping(value = "/view", consumes = MediaType.APPLICATION_JSON_VALUE)
  public PoeCraftService.View view(@RequestBody PoeCraftEngine.Item item) {
    check(item);
    return craft.view(item);
  }

  @PostMapping(value = "/pool", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<PoeCraftService.PoolLine> pool(@RequestBody PoolRequest req) {
    check(req.item());
    return craft.pool(req.item(), req.rarity());
  }

  /** 다단계 전략 시뮬레이터(10-15 C339) — 단계(화폐 · 인자 · 목표 일부를 채울 때까지 반복)를 모의해 성공률 · 성공 한 번당 기대 비용. */
  @PostMapping(value = "/strategy", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.StrategyResult strategy(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.StrategyRequest req) {
    check(req.item());
    return calc.strategy(req);
  }

  /** 에뮬레이터에서 쓴 화폐 · 총비용(10-16 C365) — 단계 목록 → 화폐별 개수 · 단가 · 합계(카오스). */
  @PostMapping(value = "/spent", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.SpentResult spent(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.SpentRequest req) {
    return calc.spent(req);
  }

  /** 상태마다 옵션 줄(C455 — 에뮬레이터 단계 기록, 최대 100개). */
  @PostMapping(value = "/lines", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<List<PoeCraftService.Line>> lines(@RequestBody List<PoeCraftEngine.Item> items) {
    List<PoeCraftEngine.Item> in =
        items == null ? List.of() : items.subList(0, Math.min(100, items.size()));
    in.forEach(this::check);
    return craft.linesOf(in);
  }

  /** 아이템 종류에 쓸 수 있는 제작 수단(10-15 C338) — 에센스 · 화석 · 공명기 · 수확 · 제작대 · 정복자. */
  @GetMapping("/options")
  public PoeCraftService.Options options(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant) {
    return craft.options(itemClass, variant);
  }

  /** 이 종류의 장막 해제 옵션(C421) — 시뮬레이터 목표 고르기. */
  @GetMapping("/veiled")
  public List<net.luversof.api.poe.service.PoeCraftService.VeiledLine> veiled(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant) {
    return craft.veiledLines(itemClass, variant);
  }

  @GetMapping("/essences")
  public List<PoeCraftDataService.Essence> essences(@RequestParam String itemClass) {
    return craft.essences(itemClass);
  }

  private void check(PoeCraftEngine.Item item) {
    if (item == null || item.itemClass() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item 이 없습니다");
    }
    if (!craft.knowsPool(item)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "모르는 풀: " + item.poolKey());
    }
  }
}
