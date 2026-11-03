package net.luversof.web.gate.bookkeeping.httpexchange;

import java.time.Instant;
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

import net.luversof.web.gate.bookkeeping.constant.BookRole;

/** api-bookkeeping /api/books. userId 는 컨트롤러가 세션 사용자(UserUtil.requireUserId())로 넣는다. */
@HttpExchange(url = "/api/books", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BookClient {

  record Book(UUID id, String name, String baseCurrency, int monthStartDay, Instant createdDate) {}

  record BookSummary(Book book, BookRole role) {}

  /** monthStartDay 가 null 이면 1. */
  record BookCommand(String name, String baseCurrency, Integer monthStartDay) {}

  @GetExchange
  List<BookSummary> list(@RequestParam UUID userId);

  @PostExchange
  Book create(@RequestParam UUID userId, @RequestBody BookCommand command);

  @GetExchange("/{bookId}")
  Book get(@PathVariable UUID bookId, @RequestParam UUID userId);

  @PutExchange("/{bookId}")
  Book update(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody BookCommand command);

  @DeleteExchange("/{bookId}")
  void delete(@PathVariable UUID bookId, @RequestParam UUID userId);
}
