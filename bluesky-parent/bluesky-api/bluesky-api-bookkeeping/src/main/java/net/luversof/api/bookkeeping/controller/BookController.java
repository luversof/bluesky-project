package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Book;
import net.luversof.api.bookkeeping.service.BookService;
import net.luversof.api.bookkeeping.service.BookService.BookCommand;
import net.luversof.api.bookkeeping.service.BookService.BookSummary;

/** 가계부. 모든 요청의 userId 는 게이트가 넣는 세션 사용자다. */
@RestController
@RequestMapping(value = "/api/books", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  @GetMapping
  public List<BookSummary> list(@RequestParam UUID userId) {
    return bookService.list(userId);
  }

  @PostMapping
  public Book create(@RequestParam UUID userId, @RequestBody BookCommand command) {
    return bookService.create(userId, command);
  }

  @GetMapping("/{bookId}")
  public Book get(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return bookService.get(bookId, userId);
  }

  @PutMapping("/{bookId}")
  public Book update(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody BookCommand command) {
    return bookService.update(bookId, userId, command);
  }

  @DeleteMapping("/{bookId}")
  public void delete(@PathVariable UUID bookId, @RequestParam UUID userId) {
    bookService.delete(bookId, userId);
  }
}
