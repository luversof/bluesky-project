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
  private final net.luversof.api.poe.service.PoeCraftImportService importer;
  private final net.luversof.api.poe.service.PoeRareNameService rareNames;
  private final net.luversof.api.poe.service.PoeCraftPriceService priceService;

  public PoeCraftController(
      PoeCraftService craft,
      net.luversof.api.poe.service.PoeCraftCalcService calc,
      net.luversof.api.poe.service.PoeCraftOptimizer optimizer,
      net.luversof.api.poe.service.PoeCraftImportService importer,
      net.luversof.api.poe.service.PoeRareNameService rareNames,
      net.luversof.api.poe.service.PoeCraftPriceService priceService) {
    this.priceService = priceService;
    this.craft = craft;
    this.calc = calc;
    this.optimizer = optimizer;
    this.importer = importer;
    this.rareNames = rareNames;
  }

  /** 목표까지 연타(10-20 C506). */
  @PostMapping(value = "/spam", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.SpamResult spam(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.SpamRequest req) {
    check(req.item());
    return calc.spam(req);
  }

  /** 같은 옵션을 얻는 다른 길 요청(10-22 P4) — family = 접두/접미:계열(계산기 목표 키와 같다). */
  public record FamilyWaysRequest(PoeCraftEngine.Item item, String family) {}

  /** 같은 옵션을 얻는 다른 길(10-22 P4) — 에센스 · 제작대 · 화석 배수 · 수확. */
  @PostMapping(value = "/family-ways", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.FamilyWays familyWays(
      @RequestBody FamilyWaysRequest req) {
    check(req.item());
    return calc.familyWays(req.item(), req.family());
  }

  /** 고른 화폐 기준 옵션 표(10-22 P3) — 요청은 화폐 적용과 같다(씨앗은 무시). */
  @PostMapping(value = "/action-pool", consumes = MediaType.APPLICATION_JSON_VALUE)
  public PoeCraftService.ActionPool actionPool(@RequestBody ApplyRequest req) {
    check(req.item());
    if (req.action() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action 이 없습니다");
    }
    return craft.actionPool(
        req.item(), req.action(), req.arg() != null ? req.arg() : req.essence());
  }

  /** 화폐 한 번의 목표 적중 확률(10-20 C494) — 에뮬레이터 화폐 단추마다. */
  @PostMapping(value = "/hit-chance", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<net.luversof.api.poe.service.PoeCraftCalcService.Hit> hitChance(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.HitRequest req) {
    check(req.item());
    return calc.hitChance(req);
  }

  /** 그 종류의 무작위 레어 이름 {en, ko}(10-20 C488) — 낱말표가 없으면 빈 값. */
  @GetMapping("/rare-name")
  public java.util.Map<String, String> rareName(@RequestParam String itemClass) {
    String[] n = rareNames.randomName(itemClass, java.util.concurrent.ThreadLocalRandom.current());
    return n == null ? java.util.Map.of() : java.util.Map.of("en", n[0], "ko", n[1]);
  }

  /** 아이템 가져오기 요청 — text = 게임에서 Ctrl+C · Ctrl+Alt+C 로 복사한 글(영 · 한). */
  public record ImportRequest(String text) {}

  /** 게임 아이템 복사 글 → 에뮬레이터 아이템(10-20 C485, craftofexile 의 Import item). 못 읽으면 error. */
  @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftImportService.ImportResult importItem(
      @RequestBody ImportRequest req) {
    if (req.text() != null && req.text().length() > 20_000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "글이 너무 깁니다");
    }
    return importer.parse(req.text());
  }

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

  /** 에뮬레이터 따라 하기(C464) — 끝난 작업의 정책으로 이 아이템의 다음 수단. 모르는 작업이면 404. */
  @PostMapping(value = "/optimize/{id}/next", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftOptimizer.Next optimizeNext(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestBody net.luversof.api.poe.service.PoeCraftOptimizer.NextRequest body) {
    check(body.item());
    net.luversof.api.poe.service.PoeCraftOptimizer.Next n =
        optimizer.next(id, body.item(), body.imprint());
    if (n == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "모르는 작업: " + id);
    }
    return n;
  }

  /**
   * 최적 제작 경로(10-19 C457 사용자 요청) — 에센스 · 제작대 · 짐승 · 화폐를 섞어 목표까지 성공 한 번당 최소 기대 비용과 상태별 수단(정책). 시간
   * 예산(budgetMs, 기본 8초) 안에서. 동기 호출(게이트는 /optimize/start 를 쓴다).
   */
  @PostMapping(value = "/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftOptimizer.Result optimize(
      @RequestBody net.luversof.api.poe.service.PoeCraftOptimizer.Request req) {
    check(req.item());
    net.luversof.api.poe.service.PoeCraftPriceService.override(req.prices());
    try {
      return optimizer.optimize(req);
    } finally {
      net.luversof.api.poe.service.PoeCraftPriceService.override(null);
    }
  }

  /** 계산기(10-15 C336) — 목표 옵션 묶음 → 방법별 성공 확률 · 기대 시도 · 기대 비용(카오스, 비용 순). */
  @PostMapping(value = "/calc", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.Response calc(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.Request req) {
    check(req.item());
    net.luversof.api.poe.service.PoeCraftPriceService.override(req.prices());
    try {
      return calc.calc(req);
    } finally {
      net.luversof.api.poe.service.PoeCraftPriceService.override(null);
    }
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
    net.luversof.api.poe.service.PoeCraftPriceService.override(req.prices());
    try {
      return calc.spent(req);
    } finally {
      net.luversof.api.poe.service.PoeCraftPriceService.override(null);
    }
  }

  /** 결과 분포(C533, craftofexile 대량 모의) — 지금 아이템에 화폐를 여러 번 따로 적용한 결과의 계열별 비율. */
  @PostMapping(value = "/distribution", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.DistResult distribution(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.DistRequest req) {
    check(req.item());
    return calc.distribution(req);
  }

  /** 계산기 목표 칸 덧붙임(C548) — 계열별 제작대 값어치 · 바알 타락 임플리싯 · 영향력 옵션 id. */
  @GetMapping("/calc-extras")
  public net.luversof.api.poe.service.PoeCraftCalcService.CalcExtras calcExtras(
      @RequestParam String itemClass,
      @RequestParam(required = false, defaultValue = "") String variant,
      @RequestParam(required = false, defaultValue = "") String influence,
      @RequestParam(required = false, defaultValue = "84") int ilvl) {
    PoeCraftEngine.Item item =
        new PoeCraftEngine.Item(
            itemClass,
            variant,
            influence,
            Math.max(1, Math.min(100, ilvl)),
            PoeCraftEngine.Rarity.NORMAL,
            java.util.List.of(),
            false);
    check(item);
    return calc.extras(item);
  }

  /** 제작 시세(C529 — 게이트 '가격 직접 입력' 칸의 기본값) — 리그 · 받은 시각 · 이름 → 카오스. */
  @GetMapping("/prices")
  public net.luversof.api.poe.service.PoeCraftPriceService.Prices prices() {
    return priceService.prices();
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
