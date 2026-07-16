package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.PoeExtractService;

/** 게임 데이터 추출 파이프라인(poe-extract) 잡 API — 시작/상태. 관리자 전용은 게이트가 게이팅. */
@RestController
@RequestMapping(value = "/api/poe/extract", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeExtractController {

  private final PoeExtractService poeExtractService;

  public PoeExtractController(PoeExtractService poeExtractService) {
    this.poeExtractService = poeExtractService;
  }

  public record ExtractStatus(
      boolean available,
      boolean imageMagickInstalled,
      boolean running,
      String status,
      List<String> logLines) {}

  @PostMapping("/start")
  public boolean start() {
    return poeExtractService.start();
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
