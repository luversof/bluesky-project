package net.luversof.web.gate.bookkeeping.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.CsvExport;
import net.luversof.web.gate.stock.support.StockViewSupport;

/** 내역 CSV 내려받기. 기간은 필수(양 끝 포함), 검색 조건은 월 화면과 같다. 기간은 달 단위로 나눠 조회한다. */
@Controller
public class BookkeepingExportController {

  /** shortcut: 한 번에 최대 10년, 그보다 길면 400 - 필요해지면 스트리밍으로. */
  static final int MAX_YEARS = 10;

  private final AssetClient assetClient;
  private final CategoryClient categoryClient;
  private final EntryClient entryClient;

  public BookkeepingExportController(
      AssetClient assetClient, CategoryClient categoryClient, EntryClient entryClient) {
    this.assetClient = assetClient;
    this.categoryClient = categoryClient;
    this.entryClient = entryClient;
  }

  @GetMapping("/bookkeeping/{bookId}/export.csv")
  public ResponseEntity<byte[]> export(
      @PathVariable UUID bookId,
      @RequestParam String from,
      @RequestParam String to,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID assetId) {
    if (StockViewSupport.isNotAuthenticated()) {
      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    var start = BookkeepingView.parseDate(from);
    var end = BookkeepingView.parseDate(to);
    if (start == null
        || end == null
        || start.isAfter(end)
        || start.plusYears(MAX_YEARS).isBefore(end)) {
      return ResponseEntity.badRequest().build();
    }
    var userId = UserUtil.requireUserId();
    var query = q == null || q.isBlank() ? null : q.strip();
    try {
      var entries = new ArrayList<Entry>();
      for (LocalDate[] chunk : CsvExport.monthChunks(start, end)) {
        entries.addAll(
            entryClient.search(
                bookId,
                userId,
                chunk[0].toString(),
                chunk[1].toString(),
                query,
                categoryId,
                assetId));
      }
      var body =
          CsvExport.csv(
                  entries,
                  BookkeepingView.assetsById(assetClient.list(bookId, userId)),
                  BookkeepingView.categoriesById(categoryClient.list(bookId, userId)))
              .getBytes(StandardCharsets.UTF_8);
      return ResponseEntity.ok()
          .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              ContentDisposition.attachment()
                  .filename("bookkeeping-" + start + "_" + end + ".csv")
                  .build()
                  .toString())
          .body(body);
    } catch (BlueskyException e) {
      if (e.getStatus() >= 500) {
        throw e;
      }
      // 기간 · 인자는 위에서 걸렀다. 남은 실패는 멤버가 아니거나 없는 가계부 - 월 화면처럼 403(클라이언트 예외는 상태 0 으로 온다).
      return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }
  }
}
