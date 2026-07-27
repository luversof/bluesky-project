package net.luversof.web.gate.poe.dto;

import java.util.List;

/** PoE 백엔드 잡 상태 응답 DTO 모음 — API 컨트롤러 응답 JSON 을 게이트가 받아 뷰 모델로 푼다(각자 정의). */
public final class PoeJobStatus {

  private PoeJobStatus() {}

  /** {@code GET /api/poe/optimize/status} */
  public record Optimize(
      boolean available,
      boolean running,
      String status,
      String phase,
      int phaseDone,
      int phaseTotal,
      int evalCount,
      List<String> logLines,
      PoeOptimizeResult result) {}

  /** {@code GET /api/poe/optimize/history} — 최근 결과 목록(최신순, 목록 표시용 요약). id = 저장 시각 epochMs. */
  public record OptimizeHistoryEntry(
      long id,
      String gemName,
      String gemNameKo,
      String className,
      String classNameKo,
      String ascendancy,
      String ascendancyKo,
      String objective,
      String scenario,
      String scenarioKo,
      boolean combatBuffs,
      String finalValue,
      int evalCount,
      long durationMs) {}

  /** {@code GET /api/poe/sim/status} */
  public record Sim(
      boolean available,
      boolean running,
      String status,
      int progressDone,
      int progressTotal,
      List<String> logLines) {}

  /** {@code GET /api/poe/sim/ranking} */
  public record SimRanking(String patch, List<PoeGemRank> ranking) {}

  /** {@code GET /api/poe/extract/status} */
  public record Extract(
      boolean available,
      boolean imageMagickInstalled,
      boolean running,
      String status,
      List<String> logLines) {}

  /** {@code GET /api/poe/extract/version} — 현재 데이터/설정/최신 패치 버전 + PoB 계산 엔진 버전 */
  public record ExtractVersion(
      String dataPatch,
      String configPatch,
      String latestPatch,
      boolean upToDate,
      String pobVersion) {}

  /** {@code GET /api/poe/gems/meta} */
  public record GemMeta(String patch, int totalCount) {}

  /** {@code GET /api/poe/uniques/meta}, {@code /base-items/meta} */
  public record CountMeta(int totalCount) {}
}
