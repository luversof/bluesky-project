package net.luversof.web.gate.bookkeeping.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.Book;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.CsvImport;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * CSV 가져오기: 파일 올리기 → 미리보기(줄마다 오류) → 넣기. 서버에 상태를 두지 않는다 - 미리보기 화면이 CSV 글자를 숨은 칸에 실어 넣기 때 다시 읽는다. 넣기는
 * API 가 전부 아니면 무로 한다.
 */
@Controller
@RequestMapping(value = "/bookkeeping/{bookId}/import", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingImportController {

  /** 화면 한 장. result 가 null 이면 올리기 단계, done 은 방금 넣은 건수. */
  public record ImportPage(Book book, CsvImport.Result result, String csv, Integer done) {}

  private final BookClient bookClient;
  private final AssetClient assetClient;
  private final CategoryClient categoryClient;
  private final EntryClient entryClient;

  public BookkeepingImportController(
      BookClient bookClient,
      AssetClient assetClient,
      CategoryClient categoryClient,
      EntryClient entryClient) {
    this.bookClient = bookClient;
    this.assetClient = assetClient;
    this.categoryClient = categoryClient;
    this.entryClient = entryClient;
  }

  @GetMapping
  public String form(
      @PathVariable UUID bookId,
      @RequestParam(required = false) Integer done,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return view(bookId, UserUtil.requireUserId(), null, null, done, response, model);
  }

  @PostMapping
  public String preview(
      @PathVariable UUID bookId,
      @RequestParam("file") MultipartFile file,
      HttpServletResponse response,
      Model model)
      throws IOException {
    var userId = UserUtil.requireUserId();
    var csv = new String(file.getBytes(), StandardCharsets.UTF_8);
    if (csv.indexOf('�') >= 0) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute(
          BookkeepingViewController.ERROR,
          BookkeepingView.msg("bookkeeping.import.error.encoding"));
      return view(bookId, userId, null, null, null, response, model);
    }
    return view(bookId, userId, csv, null, null, response, model);
  }

  /** 같은 CSV 를 다시 읽어 오류가 없을 때만 넣는다. */
  @PostMapping("/confirm")
  public String confirm(
      @PathVariable UUID bookId,
      @RequestParam String csv,
      HttpServletResponse response,
      Model model) {
    var userId = UserUtil.requireUserId();
    CsvImport.Result result;
    try {
      result =
          CsvImport.read(
              csv, assetClient.list(bookId, userId), categoryClient.list(bookId, userId));
    } catch (BlueskyException e) {
      return BookkeepingViewController.forbidden(e, response, model);
    }
    if (!result.importable()) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      return view(bookId, userId, csv, result, null, response, model);
    }
    try {
      var imported =
          entryClient.importAll(
              bookId, userId, result.rows().stream().map(CsvImport.Row::command).toList());
      return "redirect:/bookkeeping/" + bookId + "/import?done=" + imported.count();
    } catch (BlueskyException e) {
      BookkeepingViewController.failed(e, response, model);
      return view(bookId, userId, csv, result, null, response, model);
    }
  }

  private String view(
      UUID bookId,
      UUID userId,
      String csv,
      CsvImport.Result read,
      Integer done,
      HttpServletResponse response,
      Model model) {
    Book book;
    CsvImport.Result result = read;
    try {
      book = bookClient.get(bookId, userId);
      if (csv != null && result == null) {
        result =
            CsvImport.read(
                csv, assetClient.list(bookId, userId), categoryClient.list(bookId, userId));
      }
    } catch (BlueskyException e) {
      return BookkeepingViewController.forbidden(e, response, model);
    }
    if (result != null && !result.importable() && response.getStatus() < 400) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
    }
    model.addAttribute("page", new ImportPage(book, result, csv, done));
    return "bookkeeping/import";
  }
}
