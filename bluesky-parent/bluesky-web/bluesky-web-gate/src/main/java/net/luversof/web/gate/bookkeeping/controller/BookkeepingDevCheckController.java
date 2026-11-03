package net.luversof.web.gate.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.devcheck.annotation.DevCheckController;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.BookSummary;

/** 개발 점검: 게이트 → api-bookkeeping 연결 확인(devcheck 는 live 외 환경에서만 켜진다). */
@DevCheckController
@RequestMapping(value = "/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingDevCheckController {

  private final BookClient bookClient;

  public BookkeepingDevCheckController(BookClient bookClient) {
    this.bookClient = bookClient;
  }

  @GetMapping("/books")
  public List<BookSummary> books(@RequestParam UUID userId) {
    return bookClient.list(userId);
  }
}
