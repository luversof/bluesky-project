package net.luversof.web.gate.poe2.httpexchange;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe2.dto.Poe2;

/** bluesky-api-poe 의 PoE2 정적 게임 데이터(/api/poe2) 클라이언트. PoE1 은 PoeDataClient(/api/poe). */
@HttpExchange(url = "/api/poe2", accept = MediaType.APPLICATION_JSON_VALUE)
public interface Poe2DataClient {

  @GetExchange("/meta")
  Poe2.Meta meta();

  // ── 시뮬레이터(/poe2/sim) ──
  @GetExchange("/sim/options")
  Poe2.SimOptions simOptions(@RequestParam String ascendancy, @RequestParam String skill);

  @GetExchange("/sim/archetype")
  Poe2.NinjaArchetype simArchetype(@RequestParam String ascendancy, @RequestParam String skill);

  @PostExchange(value = "/sim/start", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  boolean simStart(
      @RequestParam String skill,
      @RequestParam String ascendancy,
      @RequestParam String scenario,
      @RequestParam(required = false) String unique,
      @RequestParam(required = false) String treeClass,
      @RequestParam(required = false) String treeNodes,
      @RequestParam(required = false) String treeAttrs,
      @RequestParam(required = false) String treeSets);

  @GetExchange("/sim/status")
  Poe2.SimStatus simStatus();

  // 결과 이력·젬 DPS 랭킹 — PoE1 시뮬의 optimize/history·sim/ranking 에 대응
  @GetExchange("/sim/history")
  List<Poe2.SimHistoryEntry> simHistory();

  @GetExchange("/sim/result")
  Poe2.SimResult simHistoryResult(@RequestParam long id);

  @org.springframework.web.service.annotation.DeleteExchange("/sim/history/{id}")
  boolean deleteSimHistory(@PathVariable long id);

  @PostExchange("/sim/ranking/start")
  boolean simRankingStart();

  @GetExchange("/sim/ranking/status")
  Poe2.SimRankingStatus simRankingStatus();

  @GetExchange("/sim/ranking")
  Poe2.SimRankingData simRanking();

  /** 그 시즌 보관 랭킹(10-02). */
  @GetExchange("/sim/ranking")
  Poe2.SimRankingData simRanking(@RequestParam String season);

  /** 보관된 랭킹 시즌(새 것 먼저). */
  @GetExchange("/sim/ranking/seasons")
  List<String> simRankingSeasons();

  // ── poe.ninja 실빌드 시뮬레이터 ──
  @GetExchange("/ninja/overview")
  Poe2.NinjaOverview ninjaOverview();

  /** 한 아키타입(실빌드 출발점 코드 포함) — 없으면 null. */
  @GetExchange("/ninja/archetype")
  Poe2.NinjaArchetype ninjaArchetype(@RequestParam String ascendancy, @RequestParam String skill);

  /** 자동 다듬기 시작 — 코드는 폼 본문(URL 쿼리는 헤더 한도 8KB 에 걸린다). 이미 돌고 있으면 false. */
  @PostExchange(
      value = "/build/refine/start",
      contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  boolean startRefine(@RequestParam String code);

  @GetExchange("/build/refine/status")
  Poe2.RefineStatus refineStatus();

  // ── 젬 ──
  @GetExchange("/gems/search")
  List<Poe2.Gem> searchGems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String color,
      @RequestParam(required = false) String tag);

  @GetExchange("/gems/tags")
  List<Poe2.ModLine> gemTags();

  @GetExchange("/gems/tag-groups")
  List<Poe2.TagGroup> gemTagGroups();

  @GetExchange("/gems/{slug}")
  Poe2.Gem gem(@PathVariable String slug);

  // ── 베이스 ──
  @GetExchange("/base-items/classes")
  List<Poe2.ItemClass> itemClasses();

  @GetExchange("/base-items/search")
  List<Poe2.BaseItem> searchBases(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String itemClass,
      @RequestParam(required = false) String category);

  @GetExchange("/base-items/{slug}")
  Poe2.BaseItem base(@PathVariable String slug);

  // ── 옵션 ──
  @GetExchange("/mods/pools")
  List<Poe2.ModPoolSummary> modPools(@RequestParam(required = false) String itemClass);

  @GetExchange("/mods/pool")
  Poe2.ModPool modPool(@RequestParam String key);

  @GetExchange("/mods/for-base")
  Poe2.ModPool modPoolForBase(@RequestParam String name);

  // ── 증강물 ──
  @GetExchange("/augments/search")
  List<Poe2.Augment> searchAugments(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String itemClass);

  @GetExchange("/augments/{slug}")
  Poe2.Augment augment(@PathVariable String slug);

  // ── 고유 ──
  @GetExchange("/uniques/classes")
  List<Poe2.ItemClass> uniqueClasses();

  @GetExchange("/uniques/search")
  List<Poe2.Unique> searchUniques(
      @RequestParam(required = false) String q, @RequestParam(required = false) String itemClass);

  @GetExchange("/uniques/{slug}")
  Poe2.Unique unique(@PathVariable String slug);

  // ── 빌드 ──
  /** PoB(PoE2) 코드 → 요약. 코드가 커서 form 본문으로 보낸다(URL 쿼리는 헤더 한도 8KB). */
  @PostExchange(url = "/build/import", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  Poe2.BuildSummary importBuild(@RequestParam String code);

  // 엔진 재계산·가이드는 Poe2EngineClient(읽기 제한을 넓힌 전용 클라이언트)

  // ── 데이터 관리 ──
  @GetExchange("/extract/status")
  Poe2.ExtractStatus extractStatus();

  @PostExchange("/extract/start")
  Map<String, Boolean> extractStart();

  @PostExchange("/reload")
  Poe2.Meta reload();
}
