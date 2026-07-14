package net.luversof.web.gate.poe.controller;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.poe.service.PoeBaseItemDataService;
import net.luversof.web.gate.poe.service.PoeExtractService;
import net.luversof.web.gate.poe.service.PoeGemDataService;
import net.luversof.web.gate.poe.service.PoeOptimizeService;
import net.luversof.web.gate.poe.service.PoePobEngineService;
import net.luversof.web.gate.poe.service.PoePobImportService;
import net.luversof.web.gate.poe.service.PoeSimService;
import net.luversof.web.gate.poe.service.PoeUniqueDataService;

@Controller
@RequestMapping(value = "/poe/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class PoeHtmxController {

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeExtractService poeExtractService;
  private final PoePobImportService poePobImportService;
  private final PoePobEngineService poePobEngineService;
  private final PoeSimService poeSimService;
  private final PoeOptimizeService poeOptimizeService;

  public PoeHtmxController(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeExtractService poeExtractService,
      PoePobImportService poePobImportService,
      PoePobEngineService poePobEngineService,
      PoeSimService poeSimService,
      PoeOptimizeService poeOptimizeService) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeExtractService = poeExtractService;
    this.poePobImportService = poePobImportService;
    this.poePobEngineService = poePobEngineService;
    this.poeSimService = poeSimService;
    this.poeOptimizeService = poeOptimizeService;
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
    boolean running = poeOptimizeService.isRunning();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", poeOptimizeService.isAvailable());
    model.addAttribute("running", running);
    model.addAttribute("status", poeOptimizeService.lastStatus().name());
    model.addAttribute("phase", poeOptimizeService.phase());
    model.addAttribute("phaseDone", poeOptimizeService.phaseDone());
    model.addAttribute("phaseTotal", poeOptimizeService.phaseTotal());
    model.addAttribute("evalCount", poeOptimizeService.evalCount());
    model.addAttribute("logLines", poeOptimizeService.logTail());
    model.addAttribute("result", poeOptimizeService.lastResult());
    if (!running) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/simOptimizeStatus";
  }

  /** 최적 조합 탐색 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @org.springframework.web.bind.annotation.PostMapping("/sim/optimize")
  public String startOptimize(
      @RequestParam String slug,
      @RequestParam(required = false, defaultValue = "dps") String objective,
      java.security.Principal principal) {
    if (principal != null) {
      poeOptimizeService.start(slug, objective);
    }
    return "poe/htmx/simOptimizeWrap";
  }

  /** 젬 랭킹 배치 상태 fragment — 고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단 */
  @GetMapping("/sim/status")
  public String simStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    boolean running = poeSimService.isRunning();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", poeSimService.isAvailable());
    model.addAttribute("running", running);
    model.addAttribute("status", poeSimService.lastStatus().name());
    model.addAttribute("progressDone", poeSimService.progressDone());
    model.addAttribute("progressTotal", poeSimService.progressTotal());
    model.addAttribute("logLines", poeSimService.logTail());
    if (!running) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/simStatus";
  }

  /** 젬 랭킹 배치 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @org.springframework.web.bind.annotation.PostMapping("/sim/run")
  public String startSim(java.security.Principal principal) {
    if (principal != null) {
      poeSimService.start();
    }
    return "poe/htmx/simWrap";
  }

  /** 젬 DPS 랭킹 목록 fragment */
  @GetMapping("/sim/ranking")
  public String simRanking(Model model) {
    model.addAttribute("ranking", poeSimService.ranking());
    model.addAttribute("rankingPatch", poeSimService.rankingPatch());
    return "poe/htmx/simRanking";
  }

  /** PoB 공유 코드 임포트 → 빌드 요약 fragment. 형식 오류는 같은 fragment 의 오류 상태로 표시한다. */
  @org.springframework.web.bind.annotation.PostMapping("/build/import")
  public String importBuild(@RequestParam String code, Model model) {
    try {
      model.addAttribute("build", poePobImportService.importCode(code));
      model.addAttribute("engineAvailable", poePobEngineService.isAvailable());
    } catch (IllegalArgumentException e) {
      model.addAttribute("importError", true);
    }
    return "poe/htmx/buildSummary";
  }

  /** PoB 계산 엔진(헤드리스)으로 빌드 스탯 재계산 → 결과 fragment */
  @org.springframework.web.bind.annotation.PostMapping("/build/recalc")
  public String recalcBuild(@RequestParam String code, Model model) {
    try {
      String buildXml = poePobImportService.decodeToXml(code);
      model.addAttribute("engineResult", poePobEngineService.recalculate(buildXml));
    } catch (IllegalArgumentException | IllegalStateException e) {
      model.addAttribute("engineError", true);
    }
    return "poe/htmx/buildEngineResult";
  }

  @GetMapping("/items")
  public String baseItemList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String itemClass,
      Model model) {
    model.addAttribute("items", poeBaseItemDataService.search(q, itemClass));
    model.addAttribute("totalCount", poeBaseItemDataService.totalCount());
    return "poe/htmx/itemList";
  }

  @GetMapping("/items/detail")
  public String baseItemDetail(@RequestParam String slug, Model model) {
    model.addAttribute(
        "item",
        poeBaseItemDataService
            .findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("unknown base item: " + slug)));
    return "poe/htmx/itemDetail";
  }

  /** 추출 파이프라인 상태 fragment — 고정 래퍼가 interval 폴링, 유휴 시 286 으로 중단 */
  @GetMapping("/admin/status")
  public String extractStatus(
      java.security.Principal principal,
      Model model,
      jakarta.servlet.http.HttpServletResponse response) {
    boolean running = poeExtractService.isRunning();
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", poeExtractService.isAvailable());
    model.addAttribute("running", running);
    model.addAttribute("status", poeExtractService.lastStatus().name());
    model.addAttribute("logLines", poeExtractService.logTail());
    if (!running) {
      response.setStatus(286); // htmx: 폴링 중단
    }
    return "poe/htmx/extractStatus";
  }

  /** 추출 파이프라인 시작 (로그인 필요) — 폴링 래퍼를 새로 내려 interval 을 재장전한다 */
  @org.springframework.web.bind.annotation.PostMapping("/admin/extract")
  public String startExtract(java.security.Principal principal) {
    if (principal != null) {
      poeExtractService.start();
    }
    return "poe/htmx/extractWrap";
  }

  @GetMapping("/gems")
  public String gemList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      Model model) {
    model.addAttribute("gems", poeGemDataService.search(q, type, color));
    model.addAttribute("totalCount", poeGemDataService.totalCount());
    return "poe/htmx/gemList";
  }

  @GetMapping("/uniques")
  public String uniqueList(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String category,
      Model model) {
    model.addAttribute("items", poeUniqueDataService.search(q, category));
    model.addAttribute("totalCount", poeUniqueDataService.totalCount());
    return "poe/htmx/uniqueList";
  }

  @GetMapping("/uniques/detail")
  public String uniqueDetail(@RequestParam String slug, Model model) {
    model.addAttribute(
        "item",
        poeUniqueDataService
            .findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("unknown unique: " + slug)));
    return "poe/htmx/uniqueDetail";
  }

  /** 게임 툴팁 형태의 젬 상세 레이어. level 파라미터로 표시 레벨을 바꾼다 (기본 20). */
  @GetMapping("/gems/detail")
  public String gemDetail(
      @RequestParam String slug,
      @RequestParam(required = false, defaultValue = "20") int level,
      Model model) {
    var gem =
        poeGemDataService
            .findBySlug(slug)
            .orElseThrow(() -> new IllegalArgumentException("unknown gem: " + slug));
    int maxLevel = gem.levels().isEmpty() ? 1 : gem.levels().get(gem.levels().size() - 1).level();
    int displayLevel = Math.min(Math.max(level, 1), maxLevel);
    model.addAttribute("gem", gem);
    model.addAttribute("displayLevel", displayLevel);
    return "poe/htmx/gemDetail";
  }
}
