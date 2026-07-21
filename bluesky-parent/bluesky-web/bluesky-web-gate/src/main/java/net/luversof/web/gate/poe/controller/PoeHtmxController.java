package net.luversof.web.gate.poe.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientException;

import net.luversof.web.gate.poe.config.PoeIconVersion;
import net.luversof.web.gate.poe.dto.PoeBaseItem;
import net.luversof.web.gate.poe.dto.PoeGem;
import net.luversof.web.gate.poe.dto.PoeJobStatus;
import net.luversof.web.gate.poe.dto.PoeUniqueItem;
import net.luversof.web.gate.poe.httpexchange.PoeBuildClient;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import net.luversof.web.gate.poe.httpexchange.PoeExtractClient;
import net.luversof.web.gate.poe.httpexchange.PoeOptimizeClient;
import net.luversof.web.gate.poe.httpexchange.PoeSimClient;

/**
 * PoE htmx fragment 컨트롤러 — 데이터/잡은 bluesky-api-poe 로 위임하고 여기선 로그인 게이팅 + fragment 렌더만 담당한다. 잡 시작은
 * {@code principal != null} 일 때만 클라이언트로 전달한다.
 */
@Controller
@RequestMapping(value = "/poe/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class PoeHtmxController {

  private final PoeDataClient poeDataClient;
  private final PoeBuildClient poeBuildClient;
  private final PoeOptimizeClient poeOptimizeClient;
  private final PoeSimClient poeSimClient;
  private final PoeExtractClient poeExtractClient;
  private final PoeIconVersion poeIconVersion;

  public PoeHtmxController(
      PoeDataClient poeDataClient,
      PoeBuildClient poeBuildClient,
      PoeOptimizeClient poeOptimizeClient,
      PoeSimClient poeSimClient,
      PoeExtractClient poeExtractClient,
      PoeIconVersion poeIconVersion) {
    this.poeDataClient = poeDataClient;
    this.poeBuildClient = poeBuildClient;
    this.poeOptimizeClient = poeOptimizeClient;
    this.poeSimClient = poeSimClient;
    this.poeExtractClient = poeExtractClient;
    this.poeIconVersion = poeIconVersion;
  }

  /**
   * 최적 조합 탐색 상태 fragment — 고정 래퍼가 3초 간격으로 폴링한다(체인식 폴링은 요청 하나만 실패해도 영구히 끊겨 화면이 멈춘다). 유휴/완료 상태면 결과를
   * 인라인으로 담고 HTTP 286 을 돌려줘 htmx 폴링을 중단시킨다.
   */
  @GetMapping("/sim/optimize/status")
  public String optimizeStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    PoeJobStatus.Optimize status = poeOptimizeClient.status();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", status.available());
    model.addAttribute("running", status.running());
    model.addAttribute("status", status.status());
    model.addAttribute("phase", status.phase());
    model.addAttribute("phaseDone", status.phaseDone());
    model.addAttribute("phaseTotal", status.phaseTotal());
    model.addAttribute("evalCount", status.evalCount());
    model.addAttribute("logLines", status.logLines());
    model.addAttribute("result", status.result());
    if (!status.running()) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/simOptimizeStatus";
  }

  /** 최적 조합 탐색 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @PostMapping("/sim/optimize")
  public String startOptimize(
      @RequestParam(required = false, defaultValue = "") String slug,
      @RequestParam(required = false, defaultValue = "dps") String objective,
      @RequestParam(required = false, defaultValue = "Pinnacle") String scenario,
      @RequestParam(required = false, defaultValue = "false") boolean buffs,
      @RequestParam(required = false, defaultValue = "") String className,
      @RequestParam(required = false, defaultValue = "") String ascendancy,
      @RequestParam(required = false) java.util.List<String> uniques,
      @RequestParam(required = false) java.util.List<String> skills,
      java.security.Principal principal) {
    if (principal != null) {
      // 멀티셀렉트(반복 파라미터) 또는 콤마 텍스트 둘 다 수용 → 콤마 문자열로 합쳐 API 로 전달
      poeOptimizeClient.start(
          slug,
          objective,
          scenario,
          buffs,
          className,
          ascendancy,
          joinCsv(uniques),
          joinCsv(skills));
    }
    return "poe/htmx/simOptimizeWrap";
  }

  /** 멀티셀렉트/텍스트 입력을 콤마 문자열로 정규화 (빈 값 제거). */
  private static String joinCsv(java.util.List<String> values) {
    if (values == null || values.isEmpty()) {
      return "";
    }
    return values.stream()
        .filter(v -> v != null && !v.isBlank())
        .map(String::trim)
        .reduce((a, b) -> a + "," + b)
        .orElse("");
  }

  /** 젬 랭킹 배치 상태 fragment — 고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단 */
  @GetMapping("/sim/status")
  public String simStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    PoeJobStatus.Sim status = poeSimClient.status();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", status.available());
    model.addAttribute("running", status.running());
    model.addAttribute("status", status.status());
    model.addAttribute("progressDone", status.progressDone());
    model.addAttribute("progressTotal", status.progressTotal());
    model.addAttribute("logLines", status.logLines());
    if (!status.running()) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/simStatus";
  }

  /** 젬 랭킹 배치 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @PostMapping("/sim/run")
  public String startSim(java.security.Principal principal) {
    if (principal != null) {
      poeSimClient.start();
    }
    return "poe/htmx/simWrap";
  }

  /** 젬 DPS 랭킹 목록 fragment */
  @GetMapping("/sim/ranking")
  public String simRanking(Model model) {
    PoeJobStatus.SimRanking ranking = poeSimClient.ranking();
    model.addAttribute("ranking", ranking.ranking());
    model.addAttribute("rankingPatch", ranking.patch());
    return "poe/htmx/simRanking";
  }

  /** PoB 공유 코드 임포트 → 빌드 요약 fragment. 형식 오류는 같은 fragment 의 오류 상태로 표시한다. */
  @PostMapping("/build/import")
  public String importBuild(@RequestParam String code, Model model) {
    try {
      model.addAttribute("build", poeBuildClient.importBuild(code));
      model.addAttribute("engineAvailable", poeBuildClient.available());
    } catch (RestClientException e) {
      model.addAttribute("importError", true);
    }
    return "poe/htmx/buildSummary";
  }

  /** PoB 계산 엔진(헤드리스)으로 빌드 스탯 재계산 → 결과 fragment */
  @PostMapping("/build/recalc")
  public String recalcBuild(@RequestParam String code, Model model) {
    try {
      model.addAttribute("engineResult", poeBuildClient.recalculate(code));
    } catch (RestClientException e) {
      model.addAttribute("engineError", true);
    }
    return "poe/htmx/buildEngineResult";
  }

  /** 트리 에디터에서 찍은 트리를 PoB 엔진으로 실계산 — 순수 트리 기여분(장비/보조젬 없음). */
  @PostMapping("/tree/stats")
  public String treeStats(
      @RequestParam(defaultValue = "0") int classId,
      @RequestParam(required = false) String ascendancy,
      @RequestParam String nodes,
      @RequestParam(required = false) String gem,
      @RequestParam(required = false) String masteries,
      Model model) {
    try {
      model.addAttribute(
          "treeEval",
          poeBuildClient.treeStats(
              classId,
              ascendancy == null || ascendancy.isBlank() ? null : ascendancy,
              nodes,
              gem,
              masteries));
    } catch (RestClientException e) {
      model.addAttribute("treeEvalError", true);
    }
    return "poe/htmx/treeStats";
  }

  /** 목록 렌더 상한 — 전체(1000+)를 한 번에 그리면 느려서 상위 N개만, 나머지는 검색/부위로 좁힌다 */
  private static final int LIST_LIMIT = 90;

  @GetMapping("/items")
  public String baseItemList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      Model model) {
    var matched = poeDataClient.searchBaseItems(q, itemClass);
    model.addAttribute(
        "items", matched.size() > LIST_LIMIT ? matched.subList(0, LIST_LIMIT) : matched);
    model.addAttribute("matchedCount", matched.size());
    model.addAttribute("totalCount", poeDataClient.baseItemMeta().totalCount());
    return "poe/htmx/itemList";
  }

  @GetMapping("/items/detail")
  public String baseItemDetail(@RequestParam String slug, Model model) {
    PoeBaseItem item = poeDataClient.baseItem(slug);
    model.addAttribute("item", item);
    // 이 베이스에 붙을 수 있는 모드 패밀리(티어표) — 큐레이티드 모드 풀 기준
    model.addAttribute("modFamilies", poeDataClient.modFamiliesForItemClass(item.itemClass()));
    return "poe/htmx/itemDetail";
  }

  /** 추출 파이프라인 상태 fragment — 고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단 */
  @GetMapping("/admin/status")
  public String extractStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    PoeJobStatus.Extract status = poeExtractClient.status();
    PoeJobStatus.ExtractVersion version = poeExtractClient.version(); // 최신 버전은 API 에서 캐시(10분)
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", status.available());
    model.addAttribute("running", status.running());
    model.addAttribute("status", status.status());
    model.addAttribute("logLines", status.logLines());
    model.addAttribute("latestPatch", version.latestPatch());
    model.addAttribute("outdated", version.latestPatch() != null && !version.upToDate());
    if (!status.running()) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/extractStatus";
  }

  /** 추출 파이프라인 시작 (로그인 필요) — config 패치를 최신으로 자동 교체 후 실행, 래퍼를 새로 내려 재장전 */
  @PostMapping("/admin/extract")
  public String startExtract(java.security.Principal principal) {
    if (principal != null) {
      poeExtractClient.start(true);
    }
    return "poe/htmx/extractWrap";
  }

  @GetMapping("/gems")
  public String gemList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag,
      Model model) {
    var meta = poeDataClient.gemMeta();
    model.addAttribute("gems", poeDataClient.searchGems(q, type, color, tag));
    model.addAttribute("totalCount", meta.totalCount());
    model.addAttribute("iconVersion", poeIconVersion.value()); // 아이콘 URL 캐시버스터(재생성 때마다 갱신)
    return "poe/htmx/gemList";
  }

  @GetMapping("/uniques")
  public String uniqueList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      Model model) {
    var matched = poeDataClient.searchUniques(q, itemClass);
    model.addAttribute(
        "items", matched.size() > LIST_LIMIT ? matched.subList(0, LIST_LIMIT) : matched);
    model.addAttribute("matchedCount", matched.size());
    model.addAttribute("totalCount", poeDataClient.uniqueMeta().totalCount());
    return "poe/htmx/uniqueList";
  }

  @GetMapping("/uniques/detail")
  public String uniqueDetail(@RequestParam String slug, Model model) {
    PoeUniqueItem item = poeDataClient.unique(slug);
    model.addAttribute("item", item);
    // 베이스 아이템(무기/방어 속성·아이템 클래스·요구사항)을 조인해 게임 툴팁처럼 채워 보여준다
    model.addAttribute("base", poeDataClient.baseItemByName(item.baseType()));
    return "poe/htmx/uniqueDetail";
  }

  /** 게임 툴팁 형태의 젬 상세 레이어. level 파라미터로 표시 레벨을 바꾼다 (기본 20). */
  @GetMapping("/gems/detail")
  public String gemDetail(
      @RequestParam String slug,
      @RequestParam(required = false, defaultValue = "20") int level,
      Model model) {
    PoeGem gem = poeDataClient.gem(slug);
    int maxLevel = gem.levels().isEmpty() ? 1 : gem.levels().get(gem.levels().size() - 1).level();
    int displayLevel = Math.min(Math.max(level, 1), maxLevel);
    model.addAttribute("gem", gem);
    model.addAttribute("displayLevel", displayLevel);
    return "poe/htmx/gemDetail";
  }
}
