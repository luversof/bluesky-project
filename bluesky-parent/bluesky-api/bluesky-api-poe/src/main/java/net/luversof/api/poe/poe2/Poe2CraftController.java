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

/** PoE2 제작 에뮬레이터 API(10-15 C351) — 아이템 상태를 그대로 주고받는다(서버 무상태). 징조 = 이번 화폐에 걸 징조 키 목록. */
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

  public Poe2CraftController(Poe2CraftService craft, Poe2CraftCalcService calc) {
    this.craft = craft;
    this.calc = calc;
  }

  /** 시뮬레이터(C352) — 목표 → 방법별 성공률 · 성공 한 번당 화폐 개수(균등 가정, 참고용). */
  /** 에뮬레이터에서 쓴 화폐 · 총비용(10-16 C366, 디바인). */
  @PostMapping(value = "/spent", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.SpentResult spent(
      @RequestBody Poe2CraftCalcService.SpentRequest req) {
    return calc.spent(req);
  }

  @PostMapping(value = "/calc", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.Response calc(@RequestBody Poe2CraftCalcService.Request req) {
    check(req.item());
    return calc.calc(req);
  }

  /** 다단계 전략(C355) — 단계(화폐 · 인자 · 징조 · 목표까지 반복 · 묶음)를 모의해 성공률 · 성공 한 번당 비용(디바인). */
  @PostMapping(value = "/strategy", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftCalcService.StrategyResult strategy(
      @RequestBody Poe2CraftCalcService.StrategyRequest req) {
    check(req.item());
    return calc.strategy(req);
  }

  @GetMapping("/options")
  public Poe2CraftService.Options options() {
    return craft.options();
  }

  /** 이 풀(아이템 종류)에 쓸 수 있는 에센스 + 보장 옵션 문구(C353). */
  /** 이 풀의 훼손된 속성(C420) — 시뮬레이터 목표 고르기. */
  @GetMapping("/desecrated")
  public List<Poe2CraftService.PoolLine> desecrated(
      @org.springframework.web.bind.annotation.RequestParam String poolKey,
      // 아이템 레벨(C428) — 이보다 높은 레벨의 훼손 옵션은 뺀다(없으면 전부)
      @org.springframework.web.bind.annotation.RequestParam(required = false) Integer ilvl) {
    return craft.desecratedLines(poolKey).stream()
        .filter(l -> ilvl == null || l.level() <= ilvl)
        .toList();
  }

  @GetMapping("/essences")
  public List<Poe2CraftService.EssenceOption> essences(
      @org.springframework.web.bind.annotation.RequestParam String poolKey) {
    return craft.essences(poolKey);
  }

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

  @PostMapping(value = "/view", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Poe2CraftService.View view(@RequestBody Poe2CraftEngine.Item item) {
    check(item);
    return craft.view(item);
  }

  /** 아이템 여럿의 옵션 줄(C453 — 에뮬레이터 단계 기록), 100개까지. */
  @PostMapping(value = "/lines", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<List<Poe2CraftService.Line>> lines(@RequestBody List<Poe2CraftEngine.Item> items) {
    List<Poe2CraftEngine.Item> in =
        items == null ? List.of() : items.subList(0, Math.min(100, items.size()));
    in.forEach(Poe2CraftController::check);
    return craft.linesOf(in);
  }

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
