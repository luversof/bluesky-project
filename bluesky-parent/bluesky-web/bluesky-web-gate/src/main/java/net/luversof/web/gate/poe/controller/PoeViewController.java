package net.luversof.web.gate.poe.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.web.gate.poe.dto.PoeGroups;
import net.luversof.web.gate.poe.httpexchange.PoeBuildClient;
import net.luversof.web.gate.poe.httpexchange.PoeDataClient;
import net.luversof.web.gate.poe.httpexchange.PoeExtractClient;

/**
 * PoE 화면 컨트롤러 — 데이터/엔진은 bluesky-api-poe 로 위임하고 여기선 뷰만 조립한다. 정적 게임 에셋(passive-tree.json/아이콘)은 게이트가
 * {@code poe.data-dir}(dev=로컬, k8s=공유 볼륨)에서 계속 서빙하므로 트리 존재 여부만 로컬로 확인한다.
 */
@Controller
@RequestMapping(value = "/poe", produces = MediaType.TEXT_HTML_VALUE)
public class PoeViewController {

  private final PoeDataClient poeDataClient;
  private final PoeBuildClient poeBuildClient;
  private final PoeExtractClient poeExtractClient;
  private final String dataDir;

  public PoeViewController(
      PoeDataClient poeDataClient,
      PoeBuildClient poeBuildClient,
      PoeExtractClient poeExtractClient,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.poeDataClient = poeDataClient;
    this.poeBuildClient = poeBuildClient;
    this.poeExtractClient = poeExtractClient;
    this.dataDir = dataDir;
  }

  @GetMapping
  public String gems(
      @RequestParam(required = false) String q,
      @RequestParam(required = false, defaultValue = "all") String type,
      @RequestParam(required = false, defaultValue = "all") String color,
      @RequestParam(required = false, defaultValue = "all") String tag,
      Model model) {
    var meta = poeDataClient.gemMeta();
    model.addAttribute("patch", meta.patch());
    model.addAttribute("totalCount", meta.totalCount());
    model.addAttribute("tagGroups", poeDataClient.gemTagGroups());
    model.addAttribute("initialQ", q == null ? "" : q);
    model.addAttribute("initialType", type);
    model.addAttribute("initialColor", color);
    model.addAttribute("activeTag", tag);
    return "poe/gems";
  }

  @GetMapping("/uniques")
  public String uniques(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String slot,
      Model model) {
    var groups = poeDataClient.uniqueCategoryGroups();
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("totalCount", poeDataClient.uniqueMeta().totalCount());
    model.addAttribute("categoryGroups", groups);
    model.addAttribute("initialQ", q == null ? "" : q);
    model.addAttribute("activeValue", resolveSlot(groups, slot));
    return "poe/uniques";
  }

  /** 탭 전환 진입 slot 을 이 페이지의 필터 키로 해석 — slot 을 가진 첫 항목의 key, 없으면 all. */
  private static String resolveSlot(List<? extends PoeGroups.Group> groups, String slot) {
    if (slot == null || slot.isBlank()) {
      return "all";
    }
    for (PoeGroups.Group group : groups) {
      for (PoeGroups.Entry entry : group.entries()) {
        if (slot.equals(entry.slot())) {
          return entry.key();
        }
      }
    }
    return "all";
  }

  @GetMapping("/tree")
  public String tree(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    return "poe/tree";
  }

  @GetMapping("/atlas")
  public String atlas(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("hasAtlasData", Files.exists(Path.of(dataDir, "atlas-tree.json")));
    return "poe/atlas";
  }

  @GetMapping("/items")
  public String items(
      @RequestParam(required = false) String q,
      @RequestParam(required = false) String slot,
      Model model) {
    var groups = poeDataClient.itemClassGroups();
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("totalCount", poeDataClient.baseItemMeta().totalCount());
    model.addAttribute("classGroups", groups);
    model.addAttribute("initialQ", q == null ? "" : q);
    model.addAttribute("activeValue", resolveSlot(groups, slot));
    return "poe/items";
  }

  @GetMapping("/build")
  public String build(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    return "poe/build";
  }

  @GetMapping("/sim")
  public String sim(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute(
        "activeGems",
        poeDataClient.searchGems(null, "active", "all", null).stream()
            .sorted(
                java.util.Comparator.comparing(
                    gem -> gem.nameKo() != null ? gem.nameKo() : gem.name()))
            .toList());
    return "poe/sim";
  }

  @GetMapping("/admin")
  public String admin(Model model) {
    var version = poeExtractClient.version();
    model.addAttribute("patch", version.dataPatch());
    model.addAttribute("latestPatch", version.latestPatch());
    model.addAttribute("upToDate", version.upToDate());
    model.addAttribute("gemCount", poeDataClient.gemMeta().totalCount());
    model.addAttribute("uniqueCount", poeDataClient.uniqueMeta().totalCount());
    model.addAttribute("baseItemCount", poeDataClient.baseItemMeta().totalCount());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    model.addAttribute("dataDir", dataDir);
    model.addAttribute("imageMagickInstalled", poeExtractClient.status().imageMagickInstalled());
    model.addAttribute("engineAvailable", poeBuildClient.available());
    return "poe/admin";
  }
}
