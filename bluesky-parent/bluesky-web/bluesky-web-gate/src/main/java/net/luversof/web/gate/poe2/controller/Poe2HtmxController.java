package net.luversof.web.gate.poe2.controller;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.poe2.dto.Poe2;
import net.luversof.web.gate.poe2.httpexchange.Poe2DataClient;

/** PoE2 htmx 조각 — 목록 · 호버 툴팁 · 옵션 표 · 데이터 추출 상태. */
@Controller
@RequestMapping(value = "/poe2/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class Poe2HtmxController {

  private static final Logger logger = LoggerFactory.getLogger(Poe2HtmxController.class);

  private final Poe2DataClient client;
  private final net.luversof.web.gate.poe2.httpexchange.Poe2EngineClient engine;
  private final Poe2ViewController view;

  public Poe2HtmxController(
      Poe2DataClient client,
      net.luversof.web.gate.poe2.httpexchange.Poe2EngineClient engine,
      Poe2ViewController view) {
    this.engine = engine;
    this.client = client;
    this.view = view;
  }

  private static String blankToNull(String s) {
    return s == null || s.isBlank() || "all".equals(s) ? null : s;
  }

  /** 방어구 속성 칩 판정 — 순수 3 · 이중 3 · 삼중 1 로 겹침 없이 나눈다(PoE1 베이스 목록과 같은 규칙). 방어 값이 없으면 어느 칩에도 안 든다. */
  static boolean matchesAttr(Poe2.Armour a, String attr) {
    if (attr == null || "all".equals(attr)) {
      return true;
    }
    if (a == null) {
      return false;
    }
    boolean ar = a.armour() != null && a.armour() > 0;
    boolean ev = a.evasion() != null && a.evasion() > 0;
    boolean es = a.energyShield() != null && a.energyShield() > 0;
    return switch (attr) {
      case "str" -> ar && !ev && !es;
      case "dex" -> ev && !ar && !es;
      case "int" -> es && !ar && !ev;
      case "strdex" -> ar && ev && !es;
      case "strint" -> ar && es && !ev;
      case "dexint" -> ev && es && !ar;
      case "strdexint" -> ar && ev && es;
      default -> true;
    };
  }

  /** API 오류는 조각 자리에 안내로 — 화면 전체가 깨지지 않게. 원인은 로그에 남긴다. */
  @ExceptionHandler(RuntimeException.class)
  public String apiError(RuntimeException e, Model model) {
    // 없는 항목(404)과 일시 장애를 가른다 — PoE1 조각(poe/htmx/apiError.jte)과 같은 구분(10-01). 404 는 재시도를 권하면 영영 안 되는
    // 걸 누르게 된다
    boolean notFound =
        (e instanceof io.github.luversof.boot.exception.BlueskyException be
                && be.getStatus() == 404)
            || net.luversof.web.gate.poe.controller.PoeHtmxController.isNotFound(e);
    if (notFound) {
      logger.info("PoE2 조각 요청 — 없는 항목: {}", e.toString());
    } else {
      logger.warn("PoE2 조각 요청 실패", e);
    }
    model.addAttribute("message", e.getMessage());
    model.addAttribute("notFound", notFound);
    return "poe2/htmx/error";
  }

  @GetMapping("/gems")
  public String gems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String color,
      @RequestParam(required = false) String tag,
      Model model) {
    model.addAttribute(
        "gems",
        client.searchGems(blankToNull(q), blankToNull(kind), blankToNull(color), blankToNull(tag)));
    model.addAttribute("total", client.meta().gems());
    return "poe2/htmx/gemList";
  }

  @GetMapping("/gems/detail")
  public String gemDetail(
      @RequestParam String slug, @RequestParam(required = false) Integer level, Model model) {
    model.addAttribute("gem", client.gem(slug));
    // 툴팁 안 레벨 단추(PoE1 과 같다) — 없는 레벨이면 툴팁이 최대 레벨로
    model.addAttribute("level", level);
    return "poe2/htmx/gemTooltip";
  }

  @GetMapping("/uniques")
  public String uniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String itemClass,
      Model model) {
    model.addAttribute("items", client.searchUniques(blankToNull(q), blankToNull(itemClass)));
    model.addAttribute("total", client.meta().uniques());
    return "poe2/htmx/uniqueList";
  }

  @GetMapping("/uniques/detail")
  public String uniqueDetail(
      @RequestParam String slug, @RequestParam(required = false) Integer variant, Model model) {
    Poe2.Unique item = client.unique(slug);
    model.addAttribute("item", item);
    model.addAttribute("base", view.baseOrNull(item.baseType()));
    model.addAttribute("variant", variant);
    return "poe2/htmx/uniqueTooltip";
  }

  @GetMapping("/items")
  public String items(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String itemClass,
      // 방어구 속성 베이스 필터 — PoE1 베이스 목록(PoeHtmxController.baseItemList)과 같은 규칙(10-01).
      // 순수 str=방어도 / dex=회피 / int=보호막, 이중·삼중은 겹침 없이 분할. 방어구 부위에서만 적용(다른 부위면 무시해 빈 목록을 막는다)
      @RequestParam(required = false, defaultValue = "all") String attr,
      Model model) {
    var items = client.searchBases(blankToNull(q), blankToNull(itemClass), null);
    boolean armourClass =
        itemClass != null
            && switch (itemClass) {
              case "Helmet", "Body Armour", "Gloves", "Boots", "Shield" -> true;
              default -> false;
            };
    if (armourClass && !"all".equals(attr)) {
      items = items.stream().filter(it -> matchesAttr(it.armour(), attr)).toList();
    }
    model.addAttribute("items", items);
    model.addAttribute("total", client.meta().baseItems());
    return "poe2/htmx/itemList";
  }

  @GetMapping("/items/detail")
  public String itemDetail(@RequestParam String slug, Model model) {
    model.addAttribute("base", client.base(slug));
    return "poe2/htmx/itemTooltip";
  }

  @GetMapping("/mods")
  public String mods(@RequestParam String key, Model model) {
    model.addAttribute("pool", client.modPool(key));
    return "poe2/htmx/modPool";
  }

  @GetMapping("/augments")
  public String augments(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String kind,
      @RequestParam(required = false) String itemClass,
      Model model) {
    model.addAttribute(
        "items", client.searchAugments(blankToNull(q), blankToNull(kind), blankToNull(itemClass)));
    model.addAttribute("total", client.meta().augments());
    model.addAttribute("itemClass", blankToNull(itemClass));
    return "poe2/htmx/augmentList";
  }

  /** PoB(PoE2) 코드 → 빌드 요약 조각. 코드를 못 읽으면 API 가 400 + 사유를 준다 — 그 사유를 그대로 보인다. */
  @PostMapping("/build/import")
  public String importBuild(@RequestParam(required = false) String code, Model model) {
    if (code == null || code.isBlank()) {
      model.addAttribute("error", "empty");
      return "poe2/htmx/buildSummary";
    }
    try {
      model.addAttribute("build", client.importBuild(code.trim()));
    } catch (io.github.luversof.boot.exception.BlueskyException e) {
      // 게이트 API 클라이언트는 4xx 응답 본문을 BlueskyException 으로 바꿔 던진다(상태 400 = 코드를 못 읽음)
      if (e.getStatus() >= 500) {
        throw e;
      }
      logger.info("PoE2 빌드 임포트 거절: {} ({})", e.getErrorCode(), e.getStatus());
      model.addAttribute("error", "invalid");
    } catch (org.springframework.web.client.HttpClientErrorException e) {
      logger.info("PoE2 빌드 임포트 거절: {}", e.getMessage());
      model.addAttribute("error", "invalid");
    }
    return "poe2/htmx/buildSummary";
  }

  /**
   * 시뮬레이터 "실빌드에서 출발" — 그 아키타입의 대표 실빌드 코드를 받아 요약 조각과, 코드 칸을 그 코드로 채우는 OOB 조각을 함께 돌려준다(요약이 뜨자마자 부르는
   * 재계산·가이드가 코드 칸을 hx-include 로 보내므로 칸이 먼저 채워져야 한다 — htmx 는 OOB 를 본문보다 먼저 바꾼다).
   */
  @GetMapping("/build/real-start")
  public String realStart(
      @RequestParam String ascendancy, @RequestParam String skill, Model model) {
    Poe2.NinjaArchetype a = null;
    try {
      a = client.ninjaArchetype(ascendancy, skill);
    } catch (RuntimeException e) {
      logger.warn("PoE2 실빌드 출발점 조회 실패({} / {}): {}", ascendancy, skill, e.toString());
    }
    if (a == null || a.start() == null || a.start().code() == null) {
      model.addAttribute("error", "invalid");
      return "poe2/htmx/buildSummary";
    }
    model.addAttribute("archetype", a);
    model.addAttribute("oobCode", a.start().code());
    return importInto(a.start().code(), model);
  }

  /** 시뮬레이터 선택지 — 전직·스킬 중 하나가 바뀌면 둘을 다시 그린다(서로의 인원 순으로 재배치). */
  @GetMapping("/sim/selects")
  public String simSelects(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String skill,
      Model model) {
    // 목록은 게임 데이터 전부(선택이 목록에서 빠지지 않는다), 순서만 요청 시점 poe.ninja 집계 — 스킬은 고른 전직 기준, 전직은 고른 스킬 기준
    Poe2.SimOptions options = null;
    try {
      options = client.simOptions(ascendancy, skill);
    } catch (RuntimeException e) {
      logger.warn("PoE2 시뮬레이터 선택지 조회 실패: {}", e.toString());
    }
    model.addAttribute(
        "skills",
        options == null || options.skills() == null ? java.util.List.of() : options.skills());
    model.addAttribute(
        "ascendancies",
        options == null || options.ascendancies() == null
            ? java.util.List.of()
            : options.ascendancies());
    model.addAttribute(
        "popularAscendancies",
        options == null || options.popularAscendancies() == null
            ? java.util.List.of()
            : options.popularAscendancies());
    model.addAttribute(
        "classes",
        options == null || options.classes() == null ? java.util.List.of() : options.classes());
    model.addAttribute("skill", skill);
    model.addAttribute("ascendancy", ascendancy);
    return "poe2/htmx/simSelects";
  }

  /** 시뮬레이터 미리보기 — 고른 (전직×스킬) 실빌드 성향·중앙값(전직 자동이면 스킬 단위). */
  @GetMapping("/sim/archetype")
  public String simArchetype(
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "") String skill,
      Model model) {
    Poe2.NinjaArchetype a = null;
    if (!skill.isBlank()) {
      try {
        a = client.simArchetype(ascendancy, skill);
      } catch (RuntimeException e) {
        logger.warn("PoE2 시뮬레이터 미리보기 조회 실패({} / {}): {}", ascendancy, skill, e.toString());
      }
    }
    model.addAttribute("archetype", a);
    return "poe2/htmx/simArchetype";
  }

  /** 시뮬레이션 실행(로그인 필요 — poe.ninja 요청과 엔진 계산을 수십 번 한다). */
  @PostMapping("/sim/start")
  public String simStart(
      @RequestParam(required = false, defaultValue = "") String skill,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false, defaultValue = "Pinnacle") String scenario,
      // 고정 고유 slug — PoE1 시뮬 "고유 고정"의 짝(10-01, 방어구·장신구만)
      @RequestParam(required = false, defaultValue = "") String unique,
      // 내 트리에서 출발(10-02) — 트리 화면 "→ 시뮬"이 넘긴 직업·노드·능력치 선택(시뮬 폼 숨은 칸)
      @RequestParam(required = false, defaultValue = "") String treeClass,
      @RequestParam(required = false, defaultValue = "") String treeNodes,
      @RequestParam(required = false, defaultValue = "") String treeAttrs,
      @RequestParam(required = false, defaultValue = "") String treeSets,
      java.security.Principal principal,
      Model model) {
    if (principal == null) {
      model.addAttribute("loginRequired", true);
      return "poe2/htmx/simStatus";
    }
    boolean started = false;
    try {
      started =
          !skill.isBlank()
              && client.simStart(
                  skill,
                  ascendancy,
                  scenario,
                  blankToNull(unique),
                  blankToNull(treeClass),
                  blankToNull(treeNodes),
                  blankToNull(treeAttrs),
                  blankToNull(treeSets));
    } catch (RuntimeException e) {
      logger.warn("PoE2 시뮬레이션 시작 실패({} / {}): {}", ascendancy, skill, e.toString());
    }
    if (!started) {
      model.addAttribute("status", safeSimStatus());
      model.addAttribute("notStarted", true);
      return "poe2/htmx/simStatus";
    }
    return "poe2/htmx/simWrap";
  }

  /** 시뮬레이션 상태 — 끝나면 결과 빌드 요약을 붙이고 HTTP 286 으로 폴링을 멈춘다. */
  @GetMapping("/sim/status")
  public String simStatus(Model model, jakarta.servlet.http.HttpServletResponse response) {
    Poe2.SimStatus status = safeSimStatus();
    model.addAttribute("status", status);
    if (status == null || !Boolean.TRUE.equals(status.running())) {
      response.setStatus(286);
      if (status != null && status.result() != null && status.result().code() != null) {
        try {
          model.addAttribute("build", client.importBuild(status.result().code()));
        } catch (RuntimeException e) {
          logger.warn("PoE2 시뮬레이션 결과 요약 실패: {}", e.toString());
        }
      }
    }
    return "poe2/htmx/simStatus";
  }

  // ── 결과 이력 — PoE1 /poe/htmx/sim/optimize/{history,result} 와 같은 흐름(목록 → 폴링과 분리된 지난-결과 뷰어) ──

  @GetMapping("/sim/history")
  public String simHistory(Model model) {
    java.util.List<Poe2.SimHistoryEntry> history = java.util.List.of();
    try {
      history = client.simHistory();
    } catch (RuntimeException e) {
      logger.warn("PoE2 시뮬 이력 조회 실패: {}", e.toString());
    }
    model.addAttribute("history", history);
    return "poe2/htmx/simHistory";
  }

  /** 지난 결과 한 건 — 결과 조각만(빌드 요약은 빌드 화면에서: "빌드 화면에서 열기"가 그 결과로 연다). */
  @GetMapping("/sim/result")
  public String simHistoryResult(@RequestParam long id, Model model) {
    Poe2.SimResult r = null;
    try {
      r = client.simHistoryResult(id);
    } catch (RuntimeException e) {
      logger.warn("PoE2 시뮬 이력 결과 조회 실패 {}: {}", id, e.toString());
    }
    model.addAttribute("result", r);
    model.addAttribute("historyId", id);
    return "poe2/htmx/simResult";
  }

  /** 이력 삭제 — 로그인했을 때만(PoE1 과 같다). 갱신된 목록을 돌려준다. */
  @org.springframework.web.bind.annotation.DeleteMapping("/sim/history/{id}")
  public String deleteSimHistory(
      @PathVariable long id, java.security.Principal principal, Model model) {
    if (principal == null) {
      model.addAttribute("loginRequired", true);
    } else {
      try {
        client.deleteSimHistory(id);
      } catch (RuntimeException e) {
        logger.warn("PoE2 시뮬 이력 삭제 실패 {}: {}", id, e.toString());
      }
    }
    return simHistory(model);
  }

  // ── 젬 DPS 랭킹 탭 — PoE1 /poe/htmx/sim/{status,run,ranking} 와 같은 폴링(유휴면 286) ──

  @GetMapping("/sim/ranking/status")
  public String simRankingStatus(
      Model model,
      java.security.Principal principal,
      jakarta.servlet.http.HttpServletResponse response) {
    Poe2.SimRankingStatus st = null;
    try {
      st = client.simRankingStatus();
    } catch (RuntimeException e) {
      logger.warn("PoE2 젬 랭킹 상태 조회 실패: {}", e.toString());
    }
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", st != null && st.available());
    model.addAttribute("running", st != null && st.running());
    model.addAttribute("status", st == null ? "IDLE" : st.status());
    model.addAttribute("progressDone", st == null ? 0 : st.progressDone());
    model.addAttribute("progressTotal", st == null ? 0 : st.progressTotal());
    model.addAttribute("logLines", st == null ? java.util.List.of() : st.logLines());
    if (st == null || !st.running()) {
      response.setStatus(286);
    }
    return "poe2/htmx/rankingStatus";
  }

  @PostMapping("/sim/ranking/run")
  public String simRankingRun(java.security.Principal principal) {
    if (principal != null) {
      try {
        client.simRankingStart();
      } catch (RuntimeException e) {
        logger.warn("PoE2 젬 랭킹 시작 실패: {}", e.toString());
      }
    }
    return "poe2/htmx/rankingWrap";
  }

  @GetMapping("/sim/ranking")
  public String simRanking(
      // 시즌 비교(10-02 사용자 요청, PoE1 과 같은 규칙) — season 비면 지금, against 비면 바로 이전 시즌
      @RequestParam(required = false, defaultValue = "") String season,
      @RequestParam(required = false, defaultValue = "") String against,
      Model model) {
    Poe2.SimRankingData data = null;
    java.util.List<String> seasons = java.util.List.of();
    try {
      seasons = client.simRankingSeasons();
      data = season.isBlank() ? client.simRanking() : client.simRanking(season);
    } catch (RuntimeException e) {
      logger.warn("PoE2 젬 랭킹 조회 실패: {}", e.toString());
    }
    java.util.List<Poe2.SimGemRank> rows =
        data == null || data.ranking() == null ? java.util.List.of() : data.ranking();
    String viewed = season.isBlank() && !seasons.isEmpty() ? seasons.get(0) : season;
    String vs =
        against.isBlank()
            ? net.luversof.web.gate.poe.RankCompare.previousSeason(seasons, viewed)
            : against;
    Poe2.SimRankingData prev = null;
    if (vs != null && !vs.equals(viewed)) {
      try {
        prev = client.simRanking(vs);
      } catch (RuntimeException e) {
        logger.warn("PoE2 젬 랭킹 비교 시즌 조회 실패 {}: {}", vs, e.toString());
      }
    }
    java.util.List<Poe2.SimGemRank> prevRows =
        prev == null || prev.ranking() == null ? java.util.List.of() : prev.ranking();
    model.addAttribute("ranking", rows);
    model.addAttribute("rankingPatch", data == null || data.patch() == null ? "" : data.patch());
    model.addAttribute("seasons", seasons);
    model.addAttribute("season", viewed == null ? "" : viewed);
    model.addAttribute("against", prevRows.isEmpty() ? "" : vs);
    model.addAttribute("againstPatch", prev == null || prev.patch() == null ? "" : prev.patch());
    model.addAttribute(
        "changes",
        net.luversof.web.gate.poe.RankCompare.compare(
            rows.stream().map(Poe2.SimGemRank::slug).toList(),
            rows.stream().map(Poe2.SimGemRank::dps).toList(),
            prevRows.stream().map(Poe2.SimGemRank::slug).toList(),
            prevRows.stream().map(Poe2.SimGemRank::dps).toList()));
    return "poe2/htmx/ranking";
  }

  private Poe2.SimStatus safeSimStatus() {
    try {
      return client.simStatus();
    } catch (RuntimeException e) {
      logger.warn("PoE2 시뮬레이션 상태 조회 실패: {}", e.toString());
      return null;
    }
  }

  /** 거래소 리그(PoE2 메타, 10-08 C172) — 못 읽으면 null(주소는 Standard 로). */
  private String tradeLeague() {
    try {
      return client.meta().tradeLeague();
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** 빌드 화면 "시뮬레이션 결과 열기" — 마지막 시뮬레이션 결과 빌드를 불러오고 코드 칸도 채운다(OOB). */
  @GetMapping("/build/sim-result")
  public String simResult(@RequestParam(required = false) Long id, Model model) {
    // id = 결과 이력의 그 결과(지난 결과 뷰어의 "빌드 화면에서 열기"), 없으면 마지막 시뮬레이션 결과
    String code = null;
    if (id != null) {
      try {
        Poe2.SimResult r = client.simHistoryResult(id);
        code = r == null ? null : r.code();
      } catch (RuntimeException e) {
        logger.warn("PoE2 시뮬 이력 결과 조회 실패 {}: {}", id, e.toString());
      }
    } else {
      Poe2.SimStatus st = safeSimStatus();
      code = st == null || st.result() == null ? null : st.result().code();
    }
    if (code == null) {
      model.addAttribute("error", "invalid");
      return "poe2/htmx/buildSummary";
    }
    model.addAttribute("oobCode", code);
    model.addAttribute("refined", true);
    model.addAttribute("fromSim", true); // 안내 문구를 PoE1 과 같게(10-08)
    return importInto(code, model);
  }

  /** 코드를 불러오면서 코드 칸도 그 코드로 바꾼다(OOB) — 다듬은 빌드 "불러오기"용. */
  @PostMapping("/build/import-replace")
  public String importReplace(@RequestParam String code, Model model) {
    model.addAttribute("oobCode", code);
    model.addAttribute("refined", true);
    return importInto(code, model);
  }

  private String importInto(String code, Model model) {
    try {
      model.addAttribute("build", client.importBuild(code.trim()));
    } catch (RuntimeException e) {
      logger.warn("PoE2 빌드 임포트 실패(코드 {}자): {}", code.length(), e.toString());
      model.addAttribute("error", "invalid");
      return "poe2/htmx/buildSummary";
    }
    return "poe2/htmx/buildRealStart";
  }

  /** 자동 다듬기 시작(로그인 필요 — 엔진 평가를 수십 번 한다). 이미 돌고 있으면 그 사실을 보인다. */
  @PostMapping("/build/refine/start")
  public String startRefine(
      @RequestParam String code, java.security.Principal principal, Model model) {
    if (principal == null) {
      model.addAttribute("loginRequired", true);
      return "poe2/htmx/buildRefineStatus";
    }
    boolean started = false;
    try {
      started = client.startRefine(code.trim());
    } catch (RuntimeException e) {
      logger.warn("PoE2 자동 다듬기 시작 실패(코드 {}자): {}", code.length(), e.toString());
    }
    if (!started) {
      model.addAttribute("status", safeRefineStatus());
      model.addAttribute("notStarted", true);
      return "poe2/htmx/buildRefineStatus";
    }
    return "poe2/htmx/buildRefineWrap";
  }

  /** 자동 다듬기 상태 — 진행 중이 아니면 HTTP 286 으로 htmx 폴링을 멈춘다. */
  @GetMapping("/build/refine/status")
  public String refineStatus(Model model, jakarta.servlet.http.HttpServletResponse response) {
    Poe2.RefineStatus status = safeRefineStatus();
    model.addAttribute("status", status);
    if (status == null || !Boolean.TRUE.equals(status.running())) {
      response.setStatus(286);
    }
    return "poe2/htmx/buildRefineStatus";
  }

  private Poe2.RefineStatus safeRefineStatus() {
    try {
      return client.refineStatus();
    } catch (RuntimeException e) {
      logger.warn("PoE2 자동 다듬기 상태 조회 실패: {}", e.toString());
      return null;
    }
  }

  /** 패시브 트리 평가 조각(10-01, PoE1 트리 "트리 계산"의 짝) — 트리 화면 폼(tree2.js 가 직업·전직·노드를 채운다). */
  @PostMapping("/tree/eval")
  public String treeEval(
      @RequestParam(required = false, defaultValue = "") String className,
      @RequestParam(required = false) String ascendancy,
      @RequestParam(required = false, defaultValue = "") String nodes,
      @RequestParam(required = false) String skill,
      @RequestParam(required = false, defaultValue = "") String attrs,
      @RequestParam(required = false, defaultValue = "") String sets,
      @RequestParam(required = false, defaultValue = "") String jewels,
      Model model) {
    if (className.isBlank()) {
      model.addAttribute("error", "noclass");
      return "poe2/htmx/treeEval";
    }
    try {
      model.addAttribute(
          "eval",
          engine.treeEval(
              className,
              blankToNull(ascendancy),
              nodes,
              blankToNull(skill),
              attrs,
              sets,
              blankToNull(jewels)));
    } catch (io.github.luversof.boot.exception.BlueskyException e) {
      if (e.getStatus() >= 500) {
        throw e;
      }
      model.addAttribute("error", "invalid");
    }
    return "poe2/htmx/treeEval";
  }

  /** 엔진 재계산 조각 — 요약이 붙은 뒤 hx-trigger=load 로 따로 부른다. 실패해도 요약은 그대로라 여기서는 안내만. */
  @PostMapping("/build/recalc")
  public String recalcBuild(@RequestParam(required = false) String code, Model model) {
    if (code == null || code.isBlank()) {
      model.addAttribute("error", "empty");
      return "poe2/htmx/buildRecalc";
    }
    try {
      model.addAttribute("recalc", engine.recalcBuild(code.trim()));
    } catch (io.github.luversof.boot.exception.BlueskyException e) {
      if (e.getStatus() >= 500) {
        throw e;
      }
      model.addAttribute("error", "invalid");
    }
    return "poe2/htmx/buildRecalc";
  }

  /** 업그레이드 가이드 조각 — 재계산과 같이 hx-trigger=load 로 따로. */
  @PostMapping("/build/guide")
  public String guideBuild(
      @RequestParam(required = false) String code,
      // 무기 세트별 가이드 — set = 볼 세트(없으면 API 가 주 세트로), main = 처음 응답의 주 세트(다른 세트로 바꿔 볼 때 표시 유지용)
      @RequestParam(required = false) Integer set,
      @RequestParam(required = false) Integer main,
      Model model) {
    if (code == null || code.isBlank()) {
      model.addAttribute("error", "empty");
      model.addAttribute("tradeLeague", tradeLeague()); // 가이드 제안 거래소 링크의 리그(10-08 C172)
      return "poe2/htmx/buildGuide";
    }
    try {
      Poe2.BuildGuide guide =
          set == null ? engine.guideBuild(code.trim()) : engine.guideBuild(code.trim(), set);
      model.addAttribute("guide", guide);
      model.addAttribute("mainSet", guide.mainSet() != null ? guide.mainSet() : main);
    } catch (io.github.luversof.boot.exception.BlueskyException e) {
      if (e.getStatus() >= 500) {
        throw e;
      }
      model.addAttribute("error", "invalid");
    }
    model.addAttribute("tradeLeague", tradeLeague()); // 가이드 제안 거래소 링크의 리그(10-08 C172)
    return "poe2/htmx/buildGuide";
  }

  /** 가이드 "좋은 레어로 바꾸면" 조각 — 가이드가 붙은 뒤 hx-trigger=load 로 따로(10-02, 본 가이드가 4~6초 느려지지 않게). */
  @PostMapping("/build/guide/rares")
  public String guideRares(
      @RequestParam(required = false) String code,
      @RequestParam(required = false) Integer set,
      Model model) {
    if (code == null || code.isBlank()) {
      model.addAttribute("tradeLeague", tradeLeague());
      return "poe2/htmx/guideRares";
    }
    try {
      model.addAttribute("rares", engine.guideRares(code.trim(), set));
    } catch (io.github.luversof.boot.exception.BlueskyException e) {
      if (e.getStatus() >= 500) {
        throw e;
      }
    }
    model.addAttribute("tradeLeague", tradeLeague());
    return "poe2/htmx/guideRares";
  }

  @GetMapping("/admin/status")
  public String adminStatus(Model model, Principal principal) {
    model.addAttribute("status", client.extractStatus());
    model.addAttribute("meta", client.meta());
    model.addAttribute("loggedIn", principal != null);
    return "poe2/htmx/extractStatus";
  }

  /** 데이터 추출 시작 — 로그인 사용자만(PoE1 관리 화면과 같은 규칙). */
  @PostMapping("/admin/extract")
  public String adminExtract(Model model, Principal principal) {
    if (principal != null) {
      client.extractStart();
    }
    return adminStatus(model, principal);
  }
}
