package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PutExchange;

/** api-bookkeeping 월 예산(기준통화). categoryId 가 null 이면 전체 지출. */
@HttpExchange(url = "/api/books/{bookId}/budgets", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BudgetClient {

  record Budget(UUID id, UUID bookId, UUID categoryId, BigDecimal amount) {}

  /** amount 가 null 이거나 0 이면 지운다. */
  record BudgetCommand(UUID categoryId, BigDecimal amount) {}

  @GetExchange
  List<Budget> list(@PathVariable UUID bookId, @RequestParam UUID userId);

  @PutExchange
  List<Budget> set(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody BudgetCommand command);
}
