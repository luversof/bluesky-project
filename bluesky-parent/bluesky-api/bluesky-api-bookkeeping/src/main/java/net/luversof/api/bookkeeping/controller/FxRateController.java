package net.luversof.api.bookkeeping.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.BookFxRate;
import net.luversof.api.bookkeeping.service.FxRateService;
import net.luversof.api.bookkeeping.service.FxRateService.FxRateCommand;

/** 월 환율(외화 1 단위 = 기준통화 rate). month 는 "2026-11". */
@RestController
@RequestMapping(value = "/api/books/{bookId}/fx-rates", produces = MediaType.APPLICATION_JSON_VALUE)
public class FxRateController {

  private final FxRateService fxRateService;

  public FxRateController(FxRateService fxRateService) {
    this.fxRateService = fxRateService;
  }

  @GetMapping
  public List<BookFxRate> list(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestParam YearMonth month) {
    return fxRateService.list(bookId, userId, month);
  }

  /** rate 를 비우거나 0 이면 지운다. 바뀐 뒤 그 달 목록. */
  @PutMapping
  public List<BookFxRate> set(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody FxRateCommand command) {
    return fxRateService.set(bookId, userId, command);
  }
}
