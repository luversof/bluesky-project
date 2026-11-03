package net.luversof.web.gate.bookkeeping.httpexchange;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
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
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.bookkeeping.constant.EntryType;

/** api-bookkeeping 내역. */
@HttpExchange(url = "/api/books/{bookId}/entries", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface EntryClient {

  record Entry(
      UUID id,
      UUID bookId,
      EntryType type,
      LocalDate entryDate,
      LocalTime entryTime,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo,
      UUID createdBy,
      Instant createdDate) {}

  /** 같은 통화 이체는 toAmount 를 비워도 된다. */
  record EntryCommand(
      EntryType type,
      LocalDate entryDate,
      LocalTime entryTime,
      BigDecimal amount,
      BigDecimal toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo) {}

  /**
   * 기간 검색(양 끝 포함), 최근 날짜 먼저.
   *
   * @param from yyyy-MM-dd(LocalDate.toString()). 날짜 형식이 포맷 설정을 타지 않게 문자열로 넘긴다
   * @param to yyyy-MM-dd
   */
  @GetExchange
  List<Entry> search(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID assetId);

  @PostExchange
  Entry create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody EntryCommand command);

  @PutExchange("/{entryId}")
  Entry update(
      @PathVariable UUID bookId,
      @PathVariable UUID entryId,
      @RequestParam UUID userId,
      @RequestBody EntryCommand command);

  @DeleteExchange("/{entryId}")
  void delete(@PathVariable UUID bookId, @PathVariable UUID entryId, @RequestParam UUID userId);
}
