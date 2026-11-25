package net.luversof.api.poe.poe2;

import java.util.HashSet;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * PoE2 제작
 *
 * <p>제작 에뮬레이터(10-15 C351). 아이템 상태를 그대로 주고받는다(서버 무상태). 징조 = 이번 화폐에 걸 징조 키 목록.
 */
@RestController
@RequestMapping(value = "/api/poe2/craft", produces = MediaType.APPLICATION_JSON_VALUE)
public class Poe2CraftController {

  /** arg = 화폐 등급(greater · perfect, 없으면 기본), seed = 고정 난수(시험용). */
  public record ApplyRequest(
      Poe2CraftEngine.Item item,
      Poe2CraftEngine.Action action,
      String arg,
      List<String> omens,
      Long seed) {}

  /** 재조합기(C407) — left = 지금 아이템, right = 보관한 재료, chance = 인게임 화면의 성공 확률(%), lucky = 재조합의 징조. */
  public record RecombineRequest(
      Poe2CraftEngine.Item left,
      Poe2CraftEngine.Item right,
      List<String> leftIds,
      List<String> rightIds,
      int chance,
      boolean lucky,
      Long seed) {}

  private final Poe2CraftService craft;
  private final Poe2CraftCalcService calc;
  private final Poe2CraftOptimizer optimizer;
  private final Poe2CraftImportService importer;
  private final Poe2DataService data2;

  public Poe2CraftController(
      Poe2CraftService craft,
      Poe2CraftCalcService calc,
      Poe2CraftOptimizer optimizer,
      Poe2CraftImportService importer,
      Poe2DataService data2) {
    this.craft = craft;
    this.calc = calc;
    this.optimizer = optimizer;
    this.importer = importer;
    this.data2 = data2;
  }

  /**
   * 목표까지 연타
   *
   * <p>10-20 C508.
   */
  @PostMapping(value = "/spam", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.SpamResult spam(@RequestBody Poe2CraftCalcService.SpamRequest req) {
    return calc.spam(req);
  }

  /**
   * 화폐 한 번의 목표 적중 확률 조회
   *
   * <p>에뮬레이터 화폐 단추마다 쓴다(10-20 C500).
   */
  @PostMapping(value = "/hit-chance", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<Poe2CraftCalcService.Hit> hitChance(
      @RequestBody Poe2CraftCalcService.HitRequest req) {
    return calc.hitChance(req);
  }

  /**
   * 무작위 레어 이름 생성
   *
   * <p>그 종류의 무작위 레어 이름 {en, ko}(10-20 C499). 낱말표가 없으면 빈 값.
   */
  @GetMapping("/rare-name")
  public java.util.Map<String, String> rareName(
      @org.springframework.web.bind.annotation.RequestParam String itemClass) {
    String[] n = data2.randomRareName(itemClass, java.util.concurrent.ThreadLocalRandom.current());
    return n == null ? java.util.Map.of() : java.util.Map.of("en", n[0], "ko", n[1]);
  }

  /** 아이템 가져오기 요청 — text = 게임에서 Ctrl+C · Ctrl+Alt+C 로 복사한 글. */
  public record ImportRequest(String text) {}

  /**
   * 게임 아이템 가져오기
   *
   * <p>PoE2 게임 아이템 복사 글을 에뮬레이터 아이템으로 바꾼다(10-20 C486). 못 읽으면 error.
   */
  @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftImportService.ImportResult importItem(@RequestBody ImportRequest req) {
    String t = req.text() == null ? "" : req.text();
    return importer.parse(t.length() > 20_000 ? t.substring(0, 20_000) : t);
  }

  /**
   * 최적 제작 경로 계산
   *
   * <p>화폐 × 등급 × 징조 · 에센스 · 훼손 · 분열 · 새 베이스를 섞은 최소 기대 비용(디바인)(C461).
   */
  @PostMapping(value = "/optimize", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftOptimizer.Result optimize(@RequestBody Poe2CraftOptimizer.Request req) {
    check(req.item());
    Poe2CraftCalcService.override(req.prices());
    try {
      return optimizer.optimize(req);
    } finally {
      Poe2CraftCalcService.override(null);
    }
  }

  /**
   * 다음 제작 수단 조회
   *
   * <p>에뮬레이터 따라 하기(C464). 끝난 작업의 정책으로 이 아이템의 다음 수단을 알려 준다. 모르는 작업이면 404.
   */
  @PostMapping(value = "/optimize/{id}/next", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftOptimizer.Next optimizeNext(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestBody Poe2CraftEngine.Item item) {
    check(item);
    Poe2CraftOptimizer.Next n = optimizer.next(id, item);
    if (n == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "모르는 작업: " + id);
    }
    return n;
  }

  /**
   * 제작 최적화 시작
   *
   * <p>최적 제작 경로를 백그라운드로 찾기 시작하고 작업 id 를 돌려준다(C461). 진행은 /optimize/{id}.
   */
  @PostMapping(value = "/optimize/start", consumes = MediaType.APPLICATION_JSON_VALUE)
  public java.util.Map<String, String> optimizeStart(@RequestBody Poe2CraftOptimizer.Request req) {
    check(req.item());
    return java.util.Map.of("id", optimizer.start(req));
  }

  /**
   * 제작 최적화 상태 조회
   *
   * @param id 작업 id
   * @return 작업 상태, 모르는 id 면 404
   */
  @GetMapping("/optimize/{id}")
  public Poe2CraftOptimizer.JobStatus optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id) {
    Poe2CraftOptimizer.JobStatus s = optimizer.status(id);
    if (s == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "모르는 작업: " + id);
    }
    return s;
  }

  /**
   * 쓴 화폐 · 총비용 계산
   *
   * <p>에뮬레이터에서 쓴 화폐와 총비용, 디바인(10-16 C366).
   */
  @PostMapping(value = "/spent", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.SpentResult spent(
      @RequestBody Poe2CraftCalcService.SpentRequest req) {
    Poe2CraftCalcService.override(req.prices());
    try {
      return calc.spent(req);
    } finally {
      Poe2CraftCalcService.override(null);
    }
  }

  /**
   * 제작 방법별 성공률 계산
   *
   * <p>목표 → 방법별 성공률 · 성공 한 번당 화폐 개수(균등 가정, 참고용)(C352).
   */
  @PostMapping(value = "/calc", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.Response calc(@RequestBody Poe2CraftCalcService.Request req) {
    check(req.item());
    Poe2CraftCalcService.override(req.prices());
    try {
      return calc.calc(req);
    } finally {
      Poe2CraftCalcService.override(null);
    }
  }

  /**
   * 제작 결과 분포 모의
   *
   * <p>지금 아이템에 화폐를 여러 번 따로 적용한 결과의 계열별 비율(C534).
   */
  @org.springframework.web.bind.annotation.PostMapping(
      value = "/distribution",
      consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.DistResult distribution(
      @org.springframework.web.bind.annotation.RequestBody Poe2CraftCalcService.DistRequest req) {
    check(req.item());
    return calc.distribution(req);
  }

  /**
   * 제작 시세 조회
   *
   * <p>게이트 '가격 직접 입력' 칸의 기본값, 디바인(C530).
   */
  @org.springframework.web.bind.annotation.GetMapping("/prices")
  public Poe2CraftCalcService.Prices prices() {
    return calc.prices();
  }

  /**
   * 다단계 제작 전략 모의
   *
   * <p>단계(화폐 · 인자 · 징조 · 목표까지 반복 · 묶음)를 모의해 성공률 · 성공 한 번당 비용(디바인)을 낸다(C355).
   */
  @PostMapping(value = "/strategy", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.StrategyResult strategy(
      @RequestBody Poe2CraftCalcService.StrategyRequest req) {
    check(req.item());
    Poe2CraftCalcService.override(req.prices());
    try {
      return calc.strategy(req);
    } finally {
      Poe2CraftCalcService.override(null);
    }
  }

  /**
   * 제작 선택지 조회
   *
   * <p>화폐 · 징조 · 등급 등.
   *
   * @return 선택지
   */
  @GetMapping("/options")
  public Poe2CraftService.Options options() {
    return craft.options();
  }

  /**
   * 훼손된 속성 조회
   *
   * <p>이 풀의 훼손된 속성. 시뮬레이터 목표 고르기용(C420).
   */
  @GetMapping("/desecrated")
  public List<Poe2CraftService.PoolLine> desecrated(
      @org.springframework.web.bind.annotation.RequestParam String poolKey,
      // 아이템 레벨(C428) — 이보다 높은 레벨의 훼손 옵션은 뺀다(없으면 전부)
      @org.springframework.web.bind.annotation.RequestParam(required = false) Integer ilvl) {
    return craft.desecratedLines(poolKey).stream()
        .filter(l -> ilvl == null || l.level() <= ilvl)
        .toList();
  }

  /**
   * 에센스 목록 조회
   *
   * <p>이 풀(아이템 종류)에 쓸 수 있는 에센스와 보장 옵션 문구(C353).
   */
  @GetMapping("/essences")
  public List<Poe2CraftService.EssenceOption> essences(
      @org.springframework.web.bind.annotation.RequestParam String poolKey) {
    return craft.essences(poolKey);
  }

  /**
   * 화폐 기준 옵션 표 조회
   *
   * <p>고른 화폐 기준 옵션 표(10-22 Q2). 요청은 화폐 적용과 같다(씨앗은 무시).
   */
  @PostMapping(value = "/action-pool", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftService.ActionPool actionPool(@RequestBody ApplyRequest req) {
    check(req.item());
    if (req.action() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action 이 없습니다");
    }
    return craft.actionPool(req.item(), req.action(), req.arg(), req.omens());
  }

  /**
   * 제작 행동 적용
   *
   * <p>서버는 상태를 들고 있지 않는다.
   *
   * @param req 아이템 · 행동 · 인자 · 걸어 둔 징조
   * @return 적용한 뒤의 아이템 화면 값
   */
  @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftService.View apply(@RequestBody ApplyRequest req) {
    check(req.item());
    if (req.action() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action 이 없습니다");
    }
    return craft.apply(
        req.item(),
        req.action(),
        req.arg(),
        req.omens() == null ? java.util.Set.of() : new HashSet<>(req.omens()),
        req.seed());
  }

  /**
   * 아이템 재조합
   *
   * <p>남길 옵션을 양쪽에서 고른다.
   *
   * @param req 왼쪽 · 오른쪽 아이템과 남길 옵션 id
   * @return 재조합한 아이템 화면 값
   */
  @PostMapping(value = "/recombine", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftService.View recombine(@RequestBody RecombineRequest req) {
    check(req.left());
    if (req.right() != null) {
      check(req.right());
    }
    return craft.recombine(
        req.left(),
        req.right(),
        req.leftIds() == null ? java.util.Set.of() : new java.util.LinkedHashSet<>(req.leftIds()),
        req.rightIds() == null ? java.util.Set.of() : new java.util.LinkedHashSet<>(req.rightIds()),
        req.chance(),
        req.lucky(),
        req.seed());
  }

  /**
   * 아이템 화면 값 조회
   *
   * <p>PoE2 아이템 상태를 화면 값으로 바꾼다.
   *
   * @param item 아이템 상태
   * @return 아이템 화면 값
   */
  @PostMapping(value = "/view", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftService.View view(@RequestBody Poe2CraftEngine.Item item) {
    check(item);
    return craft.view(item);
  }

  /**
   * 아이템별 옵션 줄 조회
   *
   * <p>에뮬레이터 단계 기록용, 100개까지(C453).
   */
  @PostMapping(value = "/lines", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<List<Poe2CraftService.Line>> lines(@RequestBody List<Poe2CraftEngine.Item> items) {
    List<Poe2CraftEngine.Item> in =
        items == null ? List.of() : items.subList(0, Math.min(100, items.size()));
    in.forEach(Poe2CraftController::check);
    return craft.linesOf(in);
  }

  /**
   * 옵션 표 조회
   *
   * <p>지금 PoE2 아이템에 붙을 수 있는 옵션 표.
   *
   * @param item 아이템 상태
   * @param omens 걸어 둔 징조(선택, 엑잘 징조만 표에 반영)
   * @param tier 화폐 등급(선택, greater · perfect)
   * @return 옵션 줄 목록
   */
  @PostMapping(value = "/pool", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<Poe2CraftService.PoolLine> pool(
      @RequestBody Poe2CraftEngine.Item item,
      // 걸어 둔 징조(C396) — 엑잘 징조만 표에 반영
      @org.springframework.web.bind.annotation.RequestParam(required = false) List<String> omens,
      // 고른 화폐 등급(C441) — greater · perfect, 없으면 기본
      @org.springframework.web.bind.annotation.RequestParam(required = false) String tier) {
    check(item);
    return craft.pool(item, omens, tier);
  }

  private static void check(Poe2CraftEngine.Item item) {
    if (item == null || item.poolKey() == null || item.poolKey().isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item.poolKey 가 없습니다");
    }
  }
}
