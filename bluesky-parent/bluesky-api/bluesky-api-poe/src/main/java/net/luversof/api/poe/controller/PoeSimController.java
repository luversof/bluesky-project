package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.PoeGemRank;
import net.luversof.api.poe.service.PoeSimService;

/** 젬 DPS 랭킹 배치 잡 API — 시작/상태/랭킹. */
@RestController
@RequestMapping(value = "/api/poe/sim", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeSimController {

  private final PoeSimService poeSimService;

  public PoeSimController(PoeSimService poeSimService) {
    this.poeSimService = poeSimService;
  }

  public record SimStatus(
      boolean available,
      boolean running,
      String status,
      int progressDone,
      int progressTotal,
      List<String> logLines) {}

  public record SimRanking(String patch, List<PoeGemRank> ranking) {}

  @PostMapping("/start")
  public void start() {
    poeSimService.start();
  }

  @GetMapping("/status")
  public SimStatus status() {
    return new SimStatus(
        poeSimService.isAvailable(),
        poeSimService.isRunning(),
        poeSimService.lastStatus().name(),
        poeSimService.progressDone(),
        poeSimService.progressTotal(),
        poeSimService.logTail());
  }

  @GetMapping("/ranking")
  public SimRanking ranking() {
    return new SimRanking(poeSimService.rankingPatch(), poeSimService.ranking());
  }
}
