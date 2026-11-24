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

  /**
   * 한 달의 합계(통화별).
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @param month 가계부 월, 비우면 오늘이 속한 달
   * @return 월 합계
   */
  @GetMapping("/month")
  public MonthStats month(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam(required = false) YearMonth month) {
    return statsService.month(bookId, userId, month);
  }

  /**
   * 월별 합계 추이. 내역이 있는 통화마다 모든 달을 채운다(없는 달은 0).
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @param end 마지막 달, 비우면 오늘이 속한 달
   * @param months 개월 수(1~36, 기본 12)
   * @return 통화 · 월별 합계
   */
  @GetMapping("/trend")
  public List<MonthTotal> trend(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam(required = false) YearMonth end,
      @RequestParam(defaultValue = "12") int months) {
    return statsService.trend(bookId, userId, end, months);
  }
}
