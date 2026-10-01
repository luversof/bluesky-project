package net.luversof.api.poe.poe2;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.poe.service.NinjaSnapshotSync;

/** PoE2 정적 게임 데이터 조회 API (게이트가 httpexchange 로 호출). PoE1 은 /api/poe — 경로·서비스를 섞지 않는다. */
@RestController
@RequestMapping(value = "/api/poe2", produces = MediaType.APPLICATION_JSON_VALUE)
public class Poe2DataController {

  private final Poe2DataService data;
  private final Poe2ExtractService extract;
  private final Poe2BuildService build;
  private final Poe2NinjaService ninja;
  private final Poe2NinjaSyncService ninjaSync;
  private final Poe2RefineService refine;
  private final Poe2SimService sim;
  private final Poe2SimRankingService ranking;

  public Poe2DataController(
      Poe2DataService data,
      Poe2ExtractService extract,
      Poe2BuildService build,
      Poe2NinjaService ninja,
      Poe2NinjaSyncService ninjaSync,
      Poe2RefineService refine,
      Poe2SimService sim,
      Poe2SimRankingService ranking) {
    this.data = data;
    this.extract = extract;
    this.build = build;
    this.ninja = ninja;
    this.ninjaSync = ninjaSync;
    this.refine = refine;
    this.sim = sim;
    this.ranking = ranking;
  }

  /**
   * 시뮬레이터 폼 선택지 — 목록은 게임 데이터 전부(액티브 스킬·전직), 순서는 poe.ninja 스냅샷 집계(고른 전직 → 스킬 순서, 고른 스킬 → 전직 순서). 인원은
   * 내보내지 않는다. 스냅샷은 새 버전이 나오면 Poe2NinjaSyncService 가 다시 받는다.
   */
  @GetMapping("/sim/options")
  public Poe2NinjaService.Options simOptions(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String skill) {
    return ninja.options(ascendancy, skill);
  }

  /** 시뮬레이터 미리보기 — (전직, 스킬) 아키타입, 전직이 비었으면 스킬 단위 집계(poe.ninja 스냅샷). 없으면 null. */
  @GetMapping("/sim/archetype")
  public Poe2NinjaService.Archetype simArchetype(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam String skill) {
    Poe2NinjaService.Archetype a = ninja.findOrSkill(ascendancy, skill);
    if (a == null || a.start() == null) {
      return a;
    }
    // 출발점 코드는 무거워 미리보기엔 싣지 않는다
    return new Poe2NinjaService.Archetype(
        a.ascendancy(),
        a.mainSkill(),
        a.sample(),
        a.medianLevel(),
        a.medianLife(),
        a.medianEs(),
        a.medianEhp(),
        a.medianDps(),
        a.lean(),
        a.topKeystones(),
        a.topCoSkills(),
        a.facetTotal(),
        a.topItems(),
        a.topAnointed(),
        null);
  }

  /** poe.ninja 스냅샷 동기 상태(저장본·현재 버전, 마지막 갱신·엔진 단계). */
  @GetMapping("/ninja/sync")
  public NinjaSnapshotSync.Status ninjaSyncStatus() {
    return ninjaSync.status();
  }

  /** 지금 버전 확인 → 바뀌었으면 수집(요청 안에서 돈다 — 수 분 걸릴 수 있다). 새로 받았으면 true. */
  @PostMapping("/ninja/sync")
  public boolean ninjaSyncNow() {
    return ninjaSync.syncBuilds();
  }

  /** 시뮬레이션 시작 — 이미 (시뮬·자동 다듬기가) 돌고 있으면 false. scenario = None | Boss | Pinnacle | Uber | keep. */
  @PostMapping(value = "/sim/start", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public boolean simStart(
      @RequestParam String skill,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "Pinnacle") String scenario,
      // 고정 고유 slug(방어구·장신구) — 10-01
      @RequestParam(required = false) String unique) {
    return sim.start(skill, ascendancy, scenario, unique);
  }

  @GetMapping("/sim/status")
  public Poe2SimService.SimStatus simStatus() {
    return sim.status();
  }

  // ── 결과 이력·젬 DPS 랭킹 — PoE1 /api/poe/optimize/history·result 와 /api/poe/sim/{start,status,ranking}
  // 에 대응 ──

  /** 최근 시뮬레이션 결과 목록(최신순, 최대 30). */
  @GetMapping("/sim/history")
  public List<Poe2SimService.HistoryEntry> simHistory() {
    return sim.history();
  }

  /** 이력 결과 한 건 전체 — 없으면 404. */
  @GetMapping("/sim/result")
  public Poe2SimService.SimResult simResult(@RequestParam long id) {
    Poe2SimService.SimResult r = sim.historyResult(id);
    if (r == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    return r;
  }

  /** 이력 한 건 삭제 — 삭제했으면 true. */
  @org.springframework.web.bind.annotation.DeleteMapping("/sim/history/{id}")
  public boolean deleteSimHistory(@PathVariable long id) {
    return sim.deleteHistory(id);
  }

  /** 젬 DPS 랭킹 배치 시작 — 이미(랭킹·시뮬·다듬기가) 돌고 있으면 false. */
  @PostMapping("/sim/ranking/start")
  public boolean simRankingStart() {
    return ranking.start();
  }

  @GetMapping("/sim/ranking/status")
  public Poe2SimRankingService.RankingStatus simRankingStatus() {
    return ranking.status();
  }

  @GetMapping("/sim/ranking")
  public Poe2SimRankingService.RankingData simRanking() {
    return ranking.ranking();
  }

  /** poe.ninja 실빌드 아키타입(전직×메인 스킬) 전체 — 시뮬레이터 화면. 실빌드 출발점 코드는 무거워 여기선 뺀다(real-start 로 한 건씩). */
  @GetMapping("/ninja/overview")
  public Poe2NinjaService.Overview ninjaOverview() {
    Poe2NinjaService.Overview o = ninja.overview();
    return new Poe2NinjaService.Overview(
        o.league(),
        o.globalMedianDps(),
        o.globalMedianEhp(),
        o.archetypes().stream()
            .map(
                a ->
                    a.start() == null
                        ? a
                        : new Poe2NinjaService.Archetype(
                            a.ascendancy(),
                            a.mainSkill(),
                            a.sample(),
                            a.medianLevel(),
                            a.medianLife(),
                            a.medianEs(),
                            a.medianEhp(),
                            a.medianDps(),
                            a.lean(),
                            a.topKeystones(),
                            a.topCoSkills(),
                            a.facetTotal(),
                            a.topItems(),
                            a.topAnointed(),
                            new Poe2NinjaService.RealStart(
                                a.start().level(),
                                a.start().dps(),
                                a.start().ehp(),
                                a.start().life(),
                                a.start().es(),
                                a.start().measured(),
                                a.start().medianDps(),
                                a.start().medianEhp(),
                                null)))
            .toList());
  }

  /** 한 아키타입(출발점 코드 포함) — 없으면 null. */
  @GetMapping("/ninja/archetype")
  public Poe2NinjaService.Archetype ninjaArchetype(
      @RequestParam String ascendancy, @RequestParam String skill) {
    return ninja.find(ascendancy, skill);
  }

  /** 자동 다듬기 시작 — 가이드 교체안(고유·보조젬) 중 가장 이득이 큰 것을 실제로 적용하고 다시 재기를 반복한다. 이미 돌고 있으면 false. */
  @PostMapping(
      value = "/build/refine/start",
      consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public boolean startRefine(@RequestParam String code) {
    return refine.start(code);
  }

  @GetMapping("/build/refine/status")
  public Poe2RefineService.Status refineStatus() {
    return refine.status();
  }

  /** PoB(PoE2) 코드 → 빌드 요약. 코드는 수 KB~수십 KB 라 form 본문으로 받는다(URL 쿼리는 헤더 한도 8KB 에 걸린다). */
  @PostMapping(value = "/build/import", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public Poe2.BuildSummary importBuild(@RequestParam String code) {
    try {
      return build.summarize(code);
    } catch (IllegalArgumentException e) {
      // ResponseStatusException 은 공통 예외 처리기가 500 으로 바꾼다 — 4xx 는 상태를 실은 BlueskyException 으로(WARN 로그)
      throw new io.github.luversof.boot.exception.BlueskyException("POE2_INVALID_BUILD_CODE", 400);
    }
  }

  /** 패시브 트리 평가(10-01) — 찍은 노드로 엔진 계산. nodes = 콤마 구분 노드 id, skill = 젬 영문 이름(선택). */
  @PostMapping(value = "/tree/eval", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public Poe2.TreeEval treeEval(
      @RequestParam String className,
      @RequestParam(required = false) String ascendancy,
      @RequestParam(required = false, defaultValue = "") String nodes,
      @RequestParam(required = false) String skill,
      // 능력치 노드 선택 "노드:1|2|3,…"(1 힘 · 2 민첩 · 3 지능)
      @RequestParam(required = false, defaultValue = "") String attrs) {
    java.util.Map<Integer, Integer> picks = new java.util.HashMap<>();
    for (String s : attrs.split(",")) {
      String[] kv = s.trim().split(":");
      try {
        if (kv.length == 2) {
          picks.put(Integer.valueOf(kv[0]), Integer.valueOf(kv[1]));
        }
      } catch (NumberFormatException ignored) {
        // 숫자가 아닌 조각은 버린다
      }
    }
    java.util.List<Integer> ids = new java.util.ArrayList<>();
    for (String s : nodes.split(",")) {
      try {
        if (!s.isBlank()) {
          ids.add(Integer.valueOf(s.trim()));
        }
      } catch (NumberFormatException ignored) {
        // 숫자가 아닌 조각은 버린다
      }
    }
    try {
      return build.treeEval(className, ascendancy, ids, skill, picks);
    } catch (IllegalArgumentException e) {
      throw new io.github.luversof.boot.exception.BlueskyException("POE2_INVALID_TREE", 400);
    }
  }

  /** 엔진 재계산 — PoB-PoE2 로 다시 계산해 저장값과 대조·인게임 경고. 요약과 따로(약 2초라 요약을 먼저 보인다). */
  @PostMapping(value = "/build/recalc", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public Poe2.BuildRecalc recalcBuild(@RequestParam String code) {
    try {
      return build.recalc(code);
    } catch (IllegalArgumentException e) {
      throw new io.github.luversof.boot.exception.BlueskyException("POE2_INVALID_BUILD_CODE", 400);
    }
  }

  /** 업그레이드 가이드 — PoB-PoE2 비교 계산기로 칸·보조젬·패시브 기여와 고유 교체 상위(약 3초). */
  @PostMapping(value = "/build/guide", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  public Poe2.BuildGuide guideBuild(
      @RequestParam String code, @RequestParam(required = false) Integer set) {
    try {
      return build.guide(code, set);
    } catch (IllegalArgumentException e) {
      throw new io.github.luversof.boot.exception.BlueskyException("POE2_INVALID_BUILD_CODE", 400);
    }
  }

  private static <T> T found(java.util.Optional<T> value, String what) {
    return value.orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, what + " 을(를) 찾지 못했습니다"));
  }

  @GetMapping("/meta")
  public Poe2.Meta meta() {
    return data.meta();
  }

  // 젬
  @GetMapping("/gems/search")
  public List<Poe2.Gem> gems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String color,
      @RequestParam(required = false) String tag) {
    return data.searchGems(q, kind, color, tag);
  }

  @GetMapping("/gems/tags")
  public List<Poe2.ModLine> gemTags() {
    return data.gemTags();
  }

  @GetMapping("/gems/tag-groups")
  public List<Poe2.TagGroup> gemTagGroups() {
    return data.gemTagGroups();
  }

  @GetMapping("/gems/{slug}")
  public Poe2.Gem gem(@PathVariable String slug) {
    return found(data.gem(slug), "젬 " + slug);
  }

  // 베이스 아이템
  @GetMapping("/base-items/classes")
  public List<Poe2.ItemClass> itemClasses() {
    return data.itemClasses();
  }

  @GetMapping("/base-items/search")
  public List<Poe2.BaseItem> bases(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String itemClass,
      @RequestParam(required = false) String category) {
    return data.searchBases(q, itemClass, category);
  }

  @GetMapping("/base-items/{slug}")
  public Poe2.BaseItem base(@PathVariable String slug) {
    return found(data.base(slug), "베이스 " + slug);
  }

  // 옵션
  @GetMapping("/mods/pools")
  public List<Poe2.ModPoolSummary> modPools(@RequestParam(required = false) String itemClass) {
    return data.modPools(itemClass);
  }

  @GetMapping("/mods/pool")
  public Poe2.ModPool modPool(@RequestParam String key) {
    return found(data.modPool(key), "옵션 풀 " + key);
  }

  @GetMapping("/mods/for-base")
  public Poe2.ModPool modPoolForBase(@RequestParam String name) {
    return found(data.modPoolForBase(name), "옵션 풀(베이스 " + name + ")");
  }

  // 증강물
  @GetMapping("/augments/search")
  public List<Poe2.Augment> augments(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String itemClass) {
    return data.searchAugments(q, kind, itemClass);
  }

  @GetMapping("/augments/{slug}")
  public Poe2.Augment augment(@PathVariable String slug) {
    return found(data.augment(slug), "증강물 " + slug);
  }

  // 고유
  @GetMapping("/uniques/classes")
  public List<Poe2.ItemClass> uniqueClasses() {
    return data.uniqueClasses();
  }

  @GetMapping("/uniques/search")
  public List<Poe2.Unique> uniques(
      @RequestParam(required = false) String q, @RequestParam(required = false) String itemClass) {
    return data.searchUniques(q, itemClass);
  }

  @GetMapping("/uniques/{slug}")
  public Poe2.Unique unique(@PathVariable String slug) {
    return found(data.unique(slug), "고유 아이템 " + slug);
  }

  // 데이터 관리
  @PostMapping("/reload")
  public Poe2.Meta reload() {
    data.reload();
    return data.meta();
  }

  @GetMapping("/extract/status")
  public Poe2ExtractService.Status extractStatus() {
    return extract.status();
  }

  @PostMapping("/extract/start")
  public Map<String, Boolean> extractStart() {
    return Map.of("started", extract.start());
  }
}
