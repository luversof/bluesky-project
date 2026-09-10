package net.luversof.api.stock.web.controller;

import java.time.Instant;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.PeriodSummaryService;
import net.luversof.api.stock.web.dto.response.PeriodSummary;

/** 기간 매매·배당 합계. 대시보드가 원장을 통째로 받지 않고 한 줄로 받는다. */
@RestController
@RequestMapping("/api/periodSummary")
public class PeriodSummaryController {

  @Autowired private PeriodSummaryService periodSummaryService;

  public void setPeriodSummaryService(PeriodSummaryService periodSummaryService) {
    this.periodSummaryService = periodSummaryService;
  }

  @GetMapping
  public PeriodSummary findPeriodSummary(
      @RequestParam UUID userId,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate) {
    return periodSummaryService.findPeriodSummary(userId, startDate, endDate);
  }
}
