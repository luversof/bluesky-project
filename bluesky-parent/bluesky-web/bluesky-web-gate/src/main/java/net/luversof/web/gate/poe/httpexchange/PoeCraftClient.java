package net.luversof.web.gate.poe.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeCraft;

/** PoE1 제작 에뮬레이터 API(10-15 C335) — 상태는 게이트가 들고 다닌다. */
@HttpExchange(url = "/api/poe/craft", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeCraftClient {

  @PostExchange(value = "/apply", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.View apply(@RequestBody PoeCraft.ApplyRequest request);

  @PostExchange(value = "/view", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.View view(@RequestBody PoeCraft.Item item);

  /** 결과 분포(C533). */
  @PostExchange(value = "/distribution", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.DistResult distribution(@RequestBody PoeCraft.DistRequest request);

  /** 계산기 목표 칸 덧붙임(C548) — 계열별 작업대 값어치 · 바알 타락 고정 속성 · 영향력 옵션 id. */
  @GetExchange("/calc-extras")
  PoeCraft.CalcExtras calcExtras(
      @RequestParam String itemClass,
      @RequestParam String variant,
      @RequestParam String influence,
      @RequestParam int ilvl);

  /** 제작 시세(C529 — 가격 직접 입력 칸의 기본값). */
  @GetExchange("/prices")
  PoeCraft.Prices prices();

  /** 목표까지 연타(C506). */
  @PostExchange(value = "/spam", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.SpamResult spam(@RequestBody PoeCraft.SpamRequest request);

  /** 같은 옵션을 얻는 다른 길(10-22 P4). */
  @PostExchange(value = "/family-ways", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.FamilyWays familyWays(@RequestBody PoeCraft.FamilyWaysRequest request);

  /** 고른 화폐 기준 옵션 표(10-22 P3) — 요청은 화폐 적용과 같다. */
  @PostExchange(value = "/action-pool", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.ActionPool actionPool(@RequestBody PoeCraft.ApplyRequest request);

  /** 화폐 한 번의 목표 적중 확률(C494). */
  @PostExchange(value = "/hit-chance", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<PoeCraft.Hit> hitChance(@RequestBody PoeCraft.HitRequest request);

  /** 그 종류의 무작위 레어 이름 {en, ko}(C488) — 낱말표가 없으면 빈 맵. */
  @org.springframework.web.service.annotation.GetExchange("/rare-name")
  java.util.Map<String, String> rareName(
      @org.springframework.web.bind.annotation.RequestParam String itemClass);

  /** 게임 아이템 복사 글 → 아이템(C485). */
  @PostExchange(value = "/import", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.ImportResult importItem(@RequestBody PoeCraft.ImportRequest request);

  @PostExchange(value = "/pool", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<PoeCraft.PoolLine> pool(@RequestBody PoeCraft.PoolRequest request);

  @PostExchange(value = "/calc", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.CalcResponse calc(@RequestBody PoeCraft.CalcRequest request);

  /** 에뮬레이터 따라 하기(C464) — 끝난 작업 정책으로 다음 수단. */
  @PostExchange(value = "/optimize/{id}/next", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.OptimizeNext optimizeNext(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestBody PoeCraft.OptimizeNextRequest body);

  /** 최적 조합 백그라운드 시작(C457k) — {"id": …}. */
  @PostExchange(value = "/optimize/start", contentType = MediaType.APPLICATION_JSON_VALUE)
  java.util.Map<String, String> optimizeStart(@RequestBody PoeCraft.OptimizeRequest request);

  @org.springframework.web.service.annotation.GetExchange("/optimize/{id}")
  PoeCraft.OptimizeStatus optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id);

  @PostExchange(value = "/strategy", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.StrategyResult strategy(@RequestBody PoeCraft.StrategyRequest request);

  /** 쓴 화폐 · 총비용(C365). */
  @PostExchange(value = "/spent", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.SpentResult spent(@RequestBody PoeCraft.SpentRequest request);

  /** 상태마다 옵션 줄(C455 — 단계 기록의 붙은 · 지운 옵션). */
  @PostExchange(value = "/lines", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<List<PoeCraft.Line>> lines(@RequestBody List<PoeCraft.Item> items);

  @GetExchange("/options")
  PoeCraft.Options options(@RequestParam String itemClass, @RequestParam String variant);

  /** 이 종류의 장막 해제 옵션(C421). */
  @GetExchange("/veiled")
  List<PoeCraft.VeiledLine> veiled(@RequestParam String itemClass, @RequestParam String variant);

  @GetExchange("/essences")
  List<PoeCraft.Essence> essences(@RequestParam String itemClass);
}
