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
import net.luversof.web.gate.poe.service.PoePobImportService;
import net.luversof.web.gate.poe.service.PoeUniqueDataService;

@Controller
@RequestMapping(value = "/poe/htmx", produces = MediaType.TEXT_HTML_VALUE)
public class PoeHtmxController {

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final PoeExtractService poeExtractService;
  private final PoePobImportService poePobImportService;

  public PoeHtmxController(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      PoeExtractService poeExtractService,
      PoePobImportService poePobImportService) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.poeExtractService = poeExtractService;
    this.poePobImportService = poePobImportService;
  }

  /** PoB 공유 코드 임포트 → 빌드 요약 fragment. 형식 오류는 같은 fragment 의 오류 상태로 표시한다. */
  @org.springframework.web.bind.annotation.PostMapping("/build/import")
  public String importBuild(@RequestParam String code, Model model) {
    try {
      model.addAttribute("build", poePobImportService.importCode(code));
    } catch (IllegalArgumentException e) {
      model.addAttribute("importError", true);
    }
    return "poe/htmx/buildSummary";
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

  /** 추출 파이프라인 상태 fragment — 실행 중이면 스스로 폴링한다 */
  @GetMapping("/admin/status")
  public String extractStatus(java.security.Principal principal, Model model) {
    model.addAttribute("isAuthenticated", principal != null);
    model.addAttribute("available", poeExtractService.isAvailable());
    model.addAttribute("running", poeExtractService.isRunning());
    model.addAttribute("status", poeExtractService.lastStatus().name());
    model.addAttribute("logLines", poeExtractService.logTail());
    return "poe/htmx/extractStatus";
  }

  /** 추출 파이프라인 시작 (로그인 필요) */
  @org.springframework.web.bind.annotation.PostMapping("/admin/extract")
  public String startExtract(java.security.Principal principal, Model model) {
    if (principal != null) {
      poeExtractService.start();
    }
    return extractStatus(principal, model);
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
