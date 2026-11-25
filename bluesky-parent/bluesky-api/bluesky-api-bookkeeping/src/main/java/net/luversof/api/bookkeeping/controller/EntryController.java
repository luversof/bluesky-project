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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.EntryCommand;
import net.luversof.api.bookkeeping.service.EntryService;
import net.luversof.api.bookkeeping.service.EntryService.EntrySearch;

/**
 * 가계부 내역
 *
 * <p>검색은 기간(양 끝 포함) 필수, 최근 날짜 먼저.
 */
@RestController
@RequestMapping(value = "/api/books/{bookId}/entries", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {

  private final EntryService entryService;

  public EntryController(EntryService entryService) {
    this.entryService = entryService;
  }

  /**
   * 내역 검색
   *
   * <p>기간(양 끝 포함) 안의 내역. 최근 날짜 먼저.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @param from 시작일
   * @param to 종료일
   * @param q 검색어(선택)
   * @param categoryId 분류(선택)
   * @param assetId 자산(선택)
   * @return 내역 목록
   */
  @GetMapping
  public List<Entry> search(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID assetId) {
    return entryService.search(bookId, userId, new EntrySearch(from, to, q, categoryId, assetId));
  }

  /**
   * 내역 등록
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자(기록자가 된다)
   * @param command 날짜 · 종류 · 금액 · 자산 · 분류 · 메모
   * @return 넣은 내역
   */
  @PostMapping
  public Entry create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody EntryCommand command) {
    return entryService.create(bookId, userId, command);
  }

  /** 가져오기 결과. */
  public record ImportResult(int count) {}

  /**
   * 내역 일괄 등록
   *
   * <p>최대 2000건. 하나라도 실패하면 아무것도 넣지 않는다.
   */
  @PostMapping("/import")
  public ImportResult importAll(
      @PathVariable UUID bookId,
      @RequestParam UUID userId,
      @RequestBody List<EntryCommand> commands) {
    return new ImportResult(entryService.importAll(bookId, userId, commands));
  }

  /**
   * 내역 수정
   *
   * <p>기록자와 기록 시각은 처음 값을 유지한다.
   *
   * @param bookId 가계부 id
   * @param entryId 내역 id
   * @param userId 요청한 사용자
   * @param command 바꿀 값
   * @return 고친 내역
   */
  @PutMapping("/{entryId}")
  public Entry update(
      @PathVariable UUID bookId,
      @PathVariable UUID entryId,
      @RequestParam UUID userId,
      @RequestBody EntryCommand command) {
    return entryService.update(bookId, userId, entryId, command);
  }

  /**
   * 내역 삭제
   *
   * @param bookId 가계부 id
   * @param entryId 내역 id
   * @param userId 요청한 사용자
   */
  @DeleteMapping("/{entryId}")
  public void delete(
      @PathVariable UUID bookId, @PathVariable UUID entryId, @RequestParam UUID userId) {
    entryService.delete(bookId, userId, entryId);
  }
}
