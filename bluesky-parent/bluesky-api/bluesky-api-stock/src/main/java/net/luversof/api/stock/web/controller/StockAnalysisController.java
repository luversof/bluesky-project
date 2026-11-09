package net.luversof.api.stock.web.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.analysis.StockAnalysisService;
import net.luversof.api.stock.service.analysis.StockNewsCollectService;

/**
 * 시황 분석. 작업 스케줄러 스크립트(tools/market-analysis)가 아침 · 저녁에 부른다.
 *
 * <p>아침: 시세 갱신(/api/stock/admin/price-histories) → news → input → (claude -p) → predictions. 저녁: 시세
 * 갱신 → news → grades.
 */
@RestController
@RequestMapping("/api/stock/analysis")
public class StockAnalysisController {

  @Autowired private StockAnalysisService stockAnalysisService;

  @Autowired private StockNewsCollectService stockNewsCollectService;

  @PostMapping("/news")
  public StockNewsCollectService.CollectResult collectNews(@RequestParam UUID userId) {
    return stockNewsCollectService.collect(userId, stockAnalysisService.findTargets());
  }

  @GetMapping("/input")
  public StockAnalysisService.AnalysisInput input() {
    return stockAnalysisService.buildInput();
  }

  @PostMapping("/predictions")
  public StockAnalysisService.SubmitResult submit(
      @RequestBody StockAnalysisService.SubmitRequest request) {
    return stockAnalysisService.submit(request);
  }

  @GetMapping("/predictions")
  public List<StockAnalysisService.RecentPrediction> recent(
      @RequestParam(defaultValue = "14") int days) {
    return stockAnalysisService.recent(Math.clamp(days, 1, 365));
  }

  /** 관리 화면: 기준일의 채점 전 예측 삭제. */
  @DeleteMapping("/predictions")
  public StockAnalysisService.DeleteResult deletePredictions(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate baseDate) {
    return stockAnalysisService.deleteUngraded(baseDate);
  }

  @PostMapping("/grades")
  public StockAnalysisService.GradeResult grade() {
    return stockAnalysisService.grade();
  }

  @GetMapping("/summary")
  public StockAnalysisService.Summary summary() {
    return stockAnalysisService.summary();
  }
}
