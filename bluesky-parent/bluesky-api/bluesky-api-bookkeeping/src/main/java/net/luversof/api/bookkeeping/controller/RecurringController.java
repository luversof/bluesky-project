package net.luversof.api.bookkeeping.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.BookRecurring;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.RecurringService;
import net.luversof.api.bookkeeping.service.RecurringService.Due;
import net.luversof.api.bookkeeping.service.RecurringService.RecurringCommand;

/** 반복 내역(자동으로 넣지 않는다 - 도래한 회차를 사용자가 넣는다). 날짜는 yyyy-MM-dd. */
@RestController
@RequestMapping(
    value = "/api/books/{bookId}/recurrings",
    produces = MediaType.APPLICATION_JSON_VALUE)
public class RecurringController {

  private final RecurringService recurringService;

  public RecurringController(RecurringService recurringService) {
    this.recurringService = recurringService;
  }

  @GetMapping
  public List<BookRecurring> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return recurringService.list(bookId, userId);
  }

  @PostMapping
  public BookRecurring create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody RecurringCommand command) {
    return recurringService.create(bookId, userId, command);
  }

  @DeleteMapping("/{recurringId}")
  public void delete(
      @PathVariable UUID bookId, @PathVariable UUID recurringId, @RequestParam UUID userId) {
    recurringService.delete(bookId, userId, recurringId);
  }

  /** 기간(최대 400 일) 안의 아직 안 넣은 회차. */
  @GetMapping("/due")
  public List<Due> due(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return recurringService.due(bookId, userId, from, to);
  }

  @PostMapping("/{recurringId}/confirm")
  public Entry confirm(
      @PathVariable UUID bookId,
      @PathVariable UUID recurringId,
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return recurringService.confirm(bookId, userId, recurringId, date);
  }
}
