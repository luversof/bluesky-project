package net.luversof.api.bookkeeping.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.service.StatsService;
import net.luversof.api.bookkeeping.service.StatsService.MonthStats;
import net.luversof.api.bookkeeping.service.StatsService.MonthTotal;

/** 월 화면 · 통계 화면 합계(통화별, 환산 없음). month · end 는 "2026-11" 형식이고 비우면 오늘이 속한 가계부 월. */
@RestController
@RequestMapping(value = "/api/books/{bookId}/stats", produces = MediaType.APPLICATION_JSON_VALUE)
public class StatsController {

  private final StatsService statsService;

  public StatsController(StatsService statsService) {
    this.statsService = statsService;
  }

  @GetMapping("/month")
  public MonthStats month(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam(required = false) YearMonth month) {
    return statsService.month(bookId, userId, month);
  }

  @GetMapping("/trend")
  public List<MonthTotal> trend(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam(required = false) YearMonth end,
      @RequestParam(defaultValue = "12") int months) {
    return statsService.trend(bookId, userId, end, months);
  }
}
