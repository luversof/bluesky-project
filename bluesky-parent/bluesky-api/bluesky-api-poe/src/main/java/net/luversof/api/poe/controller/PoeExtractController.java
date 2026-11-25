package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.NinjaSnapshotSync;
import net.luversof.api.poe.service.PoeExtractService;
import net.luversof.api.poe.service.PoeNinjaSyncService;
import net.luversof.api.poe.service.PoePobEngineService;

/**
 * PoE1 데이터 추출
 *
 * <p>게임 데이터 추출 파이프라인(poe-extract) 작업. 관리자 전용 여부는 게이트가 막는다.
 */
@RestController
@RequestMapping(value = "/api/poe/extract", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeExtractController {

  private final PoeExtractService poeExtractService;
  private final PoePobEngineService poePobEngineService;
  private final PoeNinjaSyncService poeNinjaSyncService;

  public PoeExtractController(
      PoeExtractService poeExtractService,
      PoePobEngineService poePobEngineService,
      PoeNinjaSyncService poeNinjaSyncService) {
    this.poeExtractService = poeExtractService;
    this.poePobEngineService = poePobEngineService;
    this.poeNinjaSyncService = poeNinjaSyncService;
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

  /**
   * 게임 데이터 버전 조회
   *
   * <p>지금 데이터의 패치, 설정의 패치, 최신 패치, 최신인지, PoB 버전.
   *
   * @return 버전 정보
   */
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

  /**
   * 게임 데이터 추출 시작
   *
   * <p>관리 화면에서 부른다. 이미 돌고 있으면 시작하지 않는다.
   *
   * @param toLatest true 면 최신 패치로 바꿔 추출한다
   * @return 시작했으면 true
   */
  @PostMapping("/start")
  public boolean start(@RequestParam(required = false, defaultValue = "false") boolean toLatest) {
    return poeExtractService.start(toLatest);
  }

  /**
   * 게임 데이터 추출 상태 조회
   *
   * <p>작업 상태와 로그.
   *
   * @return 추출 가능 여부 · ImageMagick 설치 여부 · 진행 상태 · 로그
   */
  @GetMapping("/status")
  public ExtractStatus status() {
    return new ExtractStatus(
        poeExtractService.isAvailable(),
        poeExtractService.isImageMagickInstalled(),
        poeExtractService.isRunning(),
        poeExtractService.lastStatus().name(),
        poeExtractService.logTail());
  }

  /**
   * poe.ninja 동기화 상태 조회
   *
   * <p>저장본 · 현재 버전, 마지막 갱신 · 엔진 단계(PoeNinjaSyncService).
   */
  @GetMapping("/ninja-sync")
  public NinjaSnapshotSync.Status ninjaSyncStatus() {
    return poeNinjaSyncService.status();
  }

  /**
   * poe.ninja 동기화 실행
   *
   * <p>지금 버전을 확인하고 바뀌었으면 수집한다(요청 안에서 돈다 - 수 분 걸릴 수 있다). 새로 받았으면 true.
   */
  @PostMapping("/ninja-sync")
  public boolean ninjaSyncNow() {
    return poeNinjaSyncService.syncBuilds();
  }
}
