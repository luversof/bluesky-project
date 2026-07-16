package net.luversof.web.gate.poe.controller;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
  public String gems(Model model) {
    var meta = poeDataClient.gemMeta();
    model.addAttribute("patch", meta.patch());
    model.addAttribute("totalCount", meta.totalCount());
    return "poe/gems";
  }

  @GetMapping("/uniques")
  public String uniques(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("totalCount", poeDataClient.uniqueMeta().totalCount());
    model.addAttribute("categories", poeDataClient.uniqueCategories());
    return "poe/uniques";
  }

  @GetMapping("/tree")
  public String tree(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    return "poe/tree";
  }

  @GetMapping("/items")
  public String items(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
    model.addAttribute("totalCount", poeDataClient.baseItemMeta().totalCount());
    model.addAttribute("itemClasses", poeDataClient.itemClasses());
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
        poeDataClient.searchGems(null, "active", "all").stream()
            .sorted(
                java.util.Comparator.comparing(
                    gem -> gem.nameKo() != null ? gem.nameKo() : gem.name()))
            .toList());
    return "poe/sim";
  }

  @GetMapping("/admin")
  public String admin(Model model) {
    model.addAttribute("patch", poeDataClient.gemMeta().patch());
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
