package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.bookkeeping.constant.EntryType;

/** api-bookkeeping 반복 내역. 자동으로 넣지 않는다 - 화면이 회차를 보여 주고 사용자가 넣는다. 날짜 인자는 yyyy-MM-dd 문자열. */
@HttpExchange(
    url = "/api/books/{bookId}/recurrings",
    contentType = MediaType.APPLICATION_JSON_VALUE)
public interface RecurringClient {

  /** cycle: MONTHLY · WEEKLY · YEARLY. 주기에 맞는 칸만 채워져 있다. */
  record Recurring(
      UUID id,
      UUID bookId,
      EntryType type,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo,
      String cycle,
      Integer dayOfMonth,
      Integer dayOfWeek,
      Integer monthOfYear,
      LocalDate startDate,
      LocalDate endDate,
      UUID createdBy,
      Instant createdDate) {}

  record RecurringCommand(
      EntryType type,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo,
      String cycle,
      Integer dayOfMonth,
      Integer dayOfWeek,
      Integer monthOfYear,
      LocalDate startDate,
      LocalDate endDate) {}

  /** 아직 넣지 않은 회차. */
  record Due(Recurring recurring, LocalDate date) {}

  @GetExchange
  List<Recurring> list(@PathVariable UUID bookId, @RequestParam UUID userId);

  @PostExchange
  Recurring create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody RecurringCommand command);

  @DeleteExchange("/{recurringId}")
  void delete(@PathVariable UUID bookId, @PathVariable UUID recurringId, @RequestParam UUID userId);

  @GetExchange("/due")
  List<Due> due(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam String from,
      @RequestParam String to);

  @PostExchange("/{recurringId}/confirm")
  EntryClient.Entry confirm(
      @PathVariable UUID bookId,
      @PathVariable UUID recurringId,
      @RequestParam UUID userId,
      @RequestParam String date);
}
