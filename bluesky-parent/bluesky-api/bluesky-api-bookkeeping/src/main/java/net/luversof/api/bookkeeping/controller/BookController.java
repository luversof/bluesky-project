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

/**
 * 가계부
 *
 * <p>모든 요청의 userId 는 게이트가 넣는 세션 사용자다.
 */
@RestController
@RequestMapping(value = "/api/books", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookController {

  private final BookService bookService;

  public BookController(BookService bookService) {
    this.bookService = bookService;
  }

  /**
   * 가계부 목록 조회
   *
   * <p>사용자가 멤버인 가계부만.
   *
   * @param userId 요청한 사용자
   * @return 가계부 요약
   */
  @GetMapping
  public List<BookSummary> list(@RequestParam UUID userId) {
    return bookService.list(userId);
  }

  /**
   * 가계부 생성
   *
   * <p>요청한 사용자가 OWNER 가 되고, 기본 분류 12개와 "현금" 자산이 함께 생긴다.
   *
   * @param userId 요청한 사용자
   * @param command 이름 · 기준통화 · 월 시작일
   * @return 만든 가계부
   */
  @PostMapping
  public Book create(@RequestParam UUID userId, @RequestBody BookCommand command) {
    return bookService.create(userId, command);
  }

  /**
   * 가계부 조회
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자(멤버여야 한다)
   * @return 가계부
   */
  @GetMapping("/{bookId}")
  public Book get(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return bookService.get(bookId, userId);
  }

  /**
   * 가계부 수정
   *
   * <p>OWNER 만 할 수 있다.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @param command 바꿀 값
   * @return 고친 가계부
   */
  @PutMapping("/{bookId}")
  public Book update(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody BookCommand command) {
    return bookService.update(bookId, userId, command);
  }

  /**
   * 가계부 삭제
   *
   * <p>가계부와 딸린 데이터를 모두 지운다. OWNER 만 할 수 있다.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   */
  @DeleteMapping("/{bookId}")
  public void delete(@PathVariable UUID bookId, @RequestParam UUID userId) {
    bookService.delete(bookId, userId);
  }
}
