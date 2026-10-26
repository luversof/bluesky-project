package net.luversof.web.gate.poe2.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe2.dto.Poe2Craft;

/** PoE2 제작 에뮬레이터 API(10-15 C351) — 상태는 게이트가 들고 다닌다. */
@HttpExchange(url = "/api/poe2/craft", accept = MediaType.APPLICATION_JSON_VALUE)
public interface Poe2CraftClient {

  @GetExchange("/options")
  Poe2Craft.Options options();

  /** 결과 분포(C534) — 결과 모양은 PoE1 과 같다. */
  @PostExchange(value = "/distribution", contentType = MediaType.APPLICATION_JSON_VALUE)
  net.luversof.web.gate.poe.dto.PoeCraft.DistResult distribution(
      @RequestBody Poe2Craft.DistRequest request);

  /** 제작 시세(C530 — 가격 직접 입력 칸의 기본값, 디바인). */
  @GetExchange("/prices")
  Poe2Craft.Prices prices();

  @PostExchange(value = "/apply", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.View apply(@RequestBody Poe2Craft.ApplyRequest request);

  /** 고른 화폐 기준 옵션 표(10-22 Q2) — 요청은 화폐 적용과 같다. */
  @PostExchange(value = "/action-pool", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.ActionPool actionPool(@RequestBody Poe2Craft.ApplyRequest request);

  /** 재조합기(C407). */
  @PostExchange(value = "/recombine", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.View recombine(@RequestBody Poe2Craft.RecombineRequest request);

  @PostExchange(value = "/view", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.View view(@RequestBody Poe2Craft.Item item);

  /** 목표까지 연타(C508). */
  @PostExchange(value = "/spam", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.SpamResult spam(@RequestBody Poe2Craft.SpamRequest request);

  /** 화폐 한 번의 목표 적중 확률(C500). */
  @PostExchange(value = "/hit-chance", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<Poe2Craft.Hit> hitChance(@RequestBody Poe2Craft.HitRequest request);

  /** 그 종류의 무작위 레어 이름 {en, ko}(C499). */
  @GetExchange("/rare-name")
  java.util.Map<String, String> rareName(
      @org.springframework.web.bind.annotation.RequestParam String itemClass);

  /** 게임 아이템 복사 글 → 아이템(C486). */
  @PostExchange(value = "/import", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.ImportResult importItem(@RequestBody Poe2Craft.ImportRequest request);

  /** 아이템 여럿의 옵션 줄(C453 — 단계 기록의 붙은 · 지운 옵션 문구). */
  @PostExchange(value = "/lines", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<List<Poe2Craft.Line>> lines(@RequestBody List<Poe2Craft.Item> items);

  @PostExchange(value = "/pool", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<Poe2Craft.PoolLine> pool(
      @RequestBody Poe2Craft.Item item,
      // 걸어 둔 징조(C396) — 엑잘 징조를 표에 반영, 없으면 null
      @org.springframework.web.bind.annotation.RequestParam(required = false) List<String> omens,
      // 고른 화폐 등급(C441) — greater · perfect, 기본이면 null
      @org.springframework.web.bind.annotation.RequestParam(required = false) String tier);

  default List<Poe2Craft.PoolLine> pool(Poe2Craft.Item item, List<String> omens) {
    return pool(item, omens, null);
  }

  default List<Poe2Craft.PoolLine> pool(Poe2Craft.Item item) {
    return pool(item, null, null);
  }

  /** 이 풀의 훼손된 속성(C420) — family = 군주(없으면 null). */
  @GetExchange("/desecrated")
  List<Poe2Craft.PoolLine> desecrated(
      @org.springframework.web.bind.annotation.RequestParam String poolKey,
      @org.springframework.web.bind.annotation.RequestParam Integer ilvl);

  @GetExchange("/essences")
  List<Poe2Craft.EssenceOption> essences(
      @org.springframework.web.bind.annotation.RequestParam String poolKey);

  @PostExchange(value = "/strategy", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.StrategyResult strategy(@RequestBody Poe2Craft.StrategyRequest request);

  @PostExchange(value = "/calc", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.CalcResponse calc(@RequestBody Poe2Craft.CalcRequest request);

  /** 에뮬레이터 따라 하기(C464) — 끝난 작업 정책으로 다음 수단. */
  @PostExchange(value = "/optimize/{id}/next", contentType = MediaType.APPLICATION_JSON_VALUE)
  net.luversof.web.gate.poe.dto.PoeCraft.OptimizeNext optimizeNext(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestBody Poe2Craft.Item item);

  /** 최적 조합 백그라운드 시작(C461) — {"id": …}. 결과 모양은 PoE1 과 같아 PoeCraft.OptimizeStatus 로 받는다. */
  @PostExchange(value = "/optimize/start", contentType = MediaType.APPLICATION_JSON_VALUE)
  java.util.Map<String, String> optimizeStart(@RequestBody Poe2Craft.OptimizeRequest request);

  @GetExchange("/optimize/{id}")
  net.luversof.web.gate.poe.dto.PoeCraft.OptimizeStatus optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id);

  /** 쓴 화폐 · 총비용(C366). */
  @PostExchange(value = "/spent", contentType = MediaType.APPLICATION_JSON_VALUE)
  Poe2Craft.SpentResult spent(@RequestBody Poe2Craft.SpentRequest request);
}
