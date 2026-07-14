package net.luversof.web.gate.poe.controller;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.web.gate.poe.service.PoeBaseItemDataService;
import net.luversof.web.gate.poe.service.PoeGemDataService;
import net.luversof.web.gate.poe.service.PoeUniqueDataService;

@Controller
@RequestMapping(value = "/poe", produces = MediaType.TEXT_HTML_VALUE)
public class PoeViewController {

  private final PoeGemDataService poeGemDataService;
  private final PoeUniqueDataService poeUniqueDataService;
  private final PoeBaseItemDataService poeBaseItemDataService;
  private final String dataDir;

  public PoeViewController(
      PoeGemDataService poeGemDataService,
      PoeUniqueDataService poeUniqueDataService,
      PoeBaseItemDataService poeBaseItemDataService,
      @Value("${poe.data-dir:${user.home}/.poe-gamedata}") String dataDir) {
    this.poeGemDataService = poeGemDataService;
    this.poeUniqueDataService = poeUniqueDataService;
    this.poeBaseItemDataService = poeBaseItemDataService;
    this.dataDir = dataDir;
  }

  @GetMapping
  public String gems(Model model) {
    model.addAttribute("patch", poeGemDataService.patch());
    model.addAttribute("totalCount", poeGemDataService.totalCount());
    return "poe/gems";
  }

  @GetMapping("/uniques")
  public String uniques(Model model) {
    model.addAttribute("patch", poeGemDataService.patch());
    model.addAttribute("totalCount", poeUniqueDataService.totalCount());
    model.addAttribute("categories", poeUniqueDataService.categories());
    return "poe/uniques";
  }

  @GetMapping("/tree")
  public String tree(Model model) {
    model.addAttribute("patch", poeGemDataService.patch());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    return "poe/tree";
  }

  @GetMapping("/items")
  public String items(Model model) {
    model.addAttribute("patch", poeGemDataService.patch());
    model.addAttribute("totalCount", poeBaseItemDataService.totalCount());
    model.addAttribute("itemClasses", poeBaseItemDataService.itemClasses());
    return "poe/items";
  }

  @GetMapping("/build")
  public String build(Model model) {
    model.addAttribute("patch", poeGemDataService.patch());
    return "poe/build";
  }

  @GetMapping("/admin")
  public String admin(Model model) {
    model.addAttribute("patch", poeGemDataService.patch());
    model.addAttribute("gemCount", poeGemDataService.totalCount());
    model.addAttribute("uniqueCount", poeUniqueDataService.totalCount());
    model.addAttribute("baseItemCount", poeBaseItemDataService.totalCount());
    model.addAttribute("hasTreeData", Files.exists(Path.of(dataDir, "passive-tree.json")));
    return "poe/admin";
  }
}
