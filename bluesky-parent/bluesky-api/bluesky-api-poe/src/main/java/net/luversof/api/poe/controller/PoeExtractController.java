package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.PoeExtractService;
import net.luversof.api.poe.service.PoePobEngineService;

/** 게임 데이터 추출 파이프라인(poe-extract) 잡 API — 시작/상태. 관리자 전용은 게이트가 게이팅. */
@RestController
@RequestMapping(value = "/api/poe/extract", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeExtractController {

  private final PoeExtractService poeExtractService;
  private final PoePobEngineService poePobEngineService;

  public PoeExtractController(
      PoeExtractService poeExtractService, PoePobEngineService poePobEngineService) {
    this.poeExtractService = poeExtractService;
    this.poePobEngineService = poePobEngineService;
  }

  public record ExtractStatus(
      boolean available,
      boolean imageMagickInstalled,
      boolean running,
      String status,
      List<String> logLines) {}

  /** 데이터 버전 정보 — 현재 로드된 데이터/설정/최신 버전 + 최신 일치 여부 + PoB 계산 엔진 버전 */
  public record VersionInfo(
      String dataPatch,
      String configPatch,
      String latestPatch,
      boolean upToDate,
      String pobVersion) {}

  @GetMapping("/version")
  public VersionInfo version() {
    String data = poeExtractService.dataPatch();
    String latest = poeExtractService.latestPatch();
    return new VersionInfo(
        data,
        poeExtractService.configPatch(),
        latest,
        latest != null && latest.equals(data),
        poePobEngineService.pobVersion());
  }

  @PostMapping("/start")
  public boolean start(@RequestParam(required = false, defaultValue = "false") boolean toLatest) {
    return poeExtractService.start(toLatest);
  }

  @GetMapping("/status")
  public ExtractStatus status() {
    return new ExtractStatus(
        poeExtractService.isAvailable(),
        poeExtractService.isImageMagickInstalled(),
        poeExtractService.isRunning(),
        poeExtractService.lastStatus().name(),
        poeExtractService.logTail());
  }
}
