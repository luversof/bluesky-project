package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;

/** api-bookkeeping 합계(통화별, 환산 없음). month · end 는 "2026-11", null 이면 오늘이 속한 가계부 월. */
@HttpExchange("/api/books/{bookId}/stats")
public interface StatsClient {

  /** 가계부 한 달(양 끝 포함). 월 시작일 25 면 2026-11 = 10-25 ~ 11-24. */
  record MonthRange(YearMonth month, LocalDate from, LocalDate to) {}

  record DailyTotal(LocalDate date, String currency, BigDecimal income, BigDecimal expense) {}

  /** 내역이 가리킨 분류 그대로. 대분류로 묶는 건 화면이 parentId 로 한다. */
  record CategoryTotal(UUID categoryId, CategoryKind kind, String currency, BigDecimal amount) {}

  record MonthStats(MonthRange range, List<DailyTotal> daily, List<CategoryTotal> categories) {}

  record MonthTotal(YearMonth month, String currency, BigDecimal income, BigDecimal expense) {}

  @GetExchange("/month")
  MonthStats month(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam(required = false) String month);

  /** 내역이 있는 통화마다 모든 달을 채운다(없는 달은 0). months 1~36. */
  @GetExchange("/trend")
  List<MonthTotal> trend(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam(required = false) String end,
      @RequestParam int months);
}
