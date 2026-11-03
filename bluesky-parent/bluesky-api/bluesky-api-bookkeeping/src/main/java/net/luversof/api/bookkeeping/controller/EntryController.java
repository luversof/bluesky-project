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

/** 내역. 검색은 기간(양 끝 포함) 필수, 최근 날짜 먼저. */
@RestController
@RequestMapping(value = "/api/books/{bookId}/entries", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {

  private final EntryService entryService;

  public EntryController(EntryService entryService) {
    this.entryService = entryService;
  }

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

  @PostMapping
  public Entry create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody EntryCommand command) {
    return entryService.create(bookId, userId, command);
  }

  @PutMapping("/{entryId}")
  public Entry update(
      @PathVariable UUID bookId,
      @PathVariable UUID entryId,
      @RequestParam UUID userId,
      @RequestBody EntryCommand command) {
    return entryService.update(bookId, userId, entryId, command);
  }

  @DeleteMapping("/{entryId}")
  public void delete(
      @PathVariable UUID bookId, @PathVariable UUID entryId, @RequestParam UUID userId) {
    entryService.delete(bookId, userId, entryId);
  }
}
