package net.luversof.web.gate.bookkeeping.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.Book;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.BookCommand;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.EntryCommand;
import net.luversof.web.gate.bookkeeping.httpexchange.MemberClient;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView.CalendarDay;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView.CurrencyTotal;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 가계부 화면: 목록 · 만들기 · 초대 수락 · 월 화면(달력 · 내역 · 입력 모달).
 *
 * <p>화면은 서버가 다 그리고, 쓰기는 일반 폼 POST 뒤 다시 월 화면으로 보낸다(PRG). 입력 모달은 {@code ?new=1} · {@code ?edit=} 로 열려
 * JS 가 필요 없다. 권한은 api-bookkeeping 이 판정하고(멤버가 아니면 403), 게이트는 세션 사용자만 넘긴다.
 */
@Controller
@RequestMapping(value = "/bookkeeping", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingViewController {

  private static final Logger log = LoggerFactory.getLogger(BookkeepingViewController.class);

  static final String ERROR = "error";
  static final String FORBIDDEN_VIEW = "bookkeeping/error";

  /** 월 화면 한 장에 필요한 것. */
  public record MonthPage(
      Book book,
      MonthRange range,
      List<CurrencyTotal> totals,
      List<List<CalendarDay>> weeks,
      List<Entry> entries,
      LocalDate day,
      String q,
      UUID categoryId,
      UUID assetId,
      List<AssetBalance> assets,
      Map<UUID, AssetBalance> assetsById,
      List<Category> categories,
      Map<UUID, Category> categoriesById,
      EntryForm form) {}

  /** 입력 모달의 값. open 이 false 면 모달을 그리지 않는다. 저장이 실패하면 받은 값 그대로 다시 그린다. */
  public record EntryForm(
      boolean open,
      UUID entryId,
      EntryType type,
      String date,
      String time,
      String amount,
      String toAmount,
      UUID assetId,
      UUID toAssetId,
      UUID categoryId,
      String content,
      String memo) {

    public static EntryForm closed() {
      return new EntryForm(
          false, null, EntryType.EXPENSE, "", "", "", "", null, null, null, "", "");
    }

    public static EntryForm of(Entry e) {
      return new EntryForm(
          true,
          e.id(),
          e.type(),
          e.entryDate().toString(),
          e.entryTime() == null ? "" : e.entryTime().toString(),
          BookkeepingView.plainAmount(e.amount()),
          BookkeepingView.plainAmount(e.toAmount()),
          e.assetId(),
          e.toAssetId(),
          e.categoryId(),
          e.content() == null ? "" : e.content(),
          e.memo() == null ? "" : e.memo());
    }
  }

  private final BookClient bookClient;
  private final MemberClient memberClient;
  private final AssetClient assetClient;
  private final CategoryClient categoryClient;
  private final EntryClient entryClient;
  private final StatsClient statsClient;

  public BookkeepingViewController(
      BookClient bookClient,
      MemberClient memberClient,
      AssetClient assetClient,
      CategoryClient categoryClient,
      EntryClient entryClient,
      StatsClient statsClient) {
    this.bookClient = bookClient;
    this.memberClient = memberClient;
    this.assetClient = assetClient;
    this.categoryClient = categoryClient;
    this.entryClient = entryClient;
    this.statsClient = statsClient;
  }

  // ---- 목록 · 만들기

  /** 1권이면 바로 그 가계부로(목록을 보려면 ?all=1). */
  @GetMapping
  public String index(
      @RequestParam(required = false) String all, HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    var books = bookClient.list(UserUtil.requireUserId());
    if (books.size() == 1 && all == null) {
      return "redirect:/bookkeeping/" + books.get(0).book().id();
    }
    model.addAttribute("books", books);
    model.addAttribute("currencies", BookkeepingView.CURRENCIES);
    return "bookkeeping/index";
  }

  @PostMapping
  public String create(
      @RequestParam String name,
      @RequestParam String baseCurrency,
      @RequestParam(required = false) Integer monthStartDay,
      HttpServletResponse response,
      Model model) {
    var userId = UserUtil.requireUserId();
    try {
      var book = bookClient.create(userId, new BookCommand(name, baseCurrency, monthStartDay));
      return "redirect:/bookkeeping/" + book.id();
    } catch (BlueskyException e) {
      failed(e, response, model);
      model.addAttribute("books", bookClient.list(userId));
      model.addAttribute("currencies", BookkeepingView.CURRENCIES);
      return "bookkeeping/index";
    }
  }

  // ---- 초대

  @GetMapping("/invite/{token}")
  public String invite(@PathVariable String token, HttpServletRequest request, Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    model.addAttribute("token", token);
    try {
      model.addAttribute("preview", memberClient.preview(token));
    } catch (BlueskyException e) {
      log.warn("bookkeeping invite preview failed", e);
      model.addAttribute(ERROR, failure(e));
    }
    return "bookkeeping/invite";
  }

  @PostMapping("/invite/{token}/accept")
  public String accept(@PathVariable String token, HttpServletResponse response, Model model) {
    try {
      var member = memberClient.accept(token, UserUtil.requireUserId());
      return "redirect:/bookkeeping/" + member.bookId();
    } catch (BlueskyException e) {
      failed(e, response, model);
      model.addAttribute("token", token);
      try {
        model.addAttribute("preview", memberClient.preview(token));
      } catch (BlueskyException ignored) {
        // 미리보기도 안 되면(없는 링크) 오류 문구만 보인다
      }
      return "bookkeeping/invite";
    }
  }

  // ---- 월 화면

  @GetMapping("/{bookId}")
  public String month(
      @PathVariable UUID bookId,
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String day,
      @RequestParam(required = false) String q,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) UUID assetId,
      @RequestParam(name = "new", required = false) String newEntry,
      @RequestParam(required = false) UUID edit,
      @RequestParam(required = false) EntryType type,
      @RequestParam(required = false) String date,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return monthView(
        bookId,
        UserUtil.requireUserId(),
        month,
        day,
        q,
        categoryId,
        assetId,
        newEntry,
        edit,
        type,
        date,
        null,
        response,
        model);
  }

  /**
   * 월 화면을 그린다. forced 가 있으면(저장 실패) 그 입력값으로 모달을 연다.
   *
   * <p>실패를 리다이렉트 + 플래시로 넘기지 않는다 — 게이트 세션 저장소(api-user createSession)가 지운 속성을 지우지 않아 플래시가 다음 요청들에도
   * 계속 남았다(2026-11-03 실측: 한 번 실패한 입력 모달이 3분 동안 매 화면에 다시 열림).
   */
  private String monthView(
      UUID bookId,
      UUID userId,
      String month,
      String day,
      String q,
      UUID categoryId,
      UUID assetId,
      String newEntry,
      UUID edit,
      EntryType type,
      String date,
      EntryForm forced,
      HttpServletResponse response,
      Model model) {
    Book book;
    try {
      book = bookClient.get(bookId, userId);
    } catch (BlueskyException e) {
      return forbidden(e, response, model);
    }
    var parsedMonth = BookkeepingView.parseMonth(month);
    var stats =
        statsClient.month(bookId, userId, parsedMonth == null ? null : parsedMonth.toString());
    var range = stats.range();
    var selectedDay = BookkeepingView.parseDate(day);
    if (selectedDay != null
        && (selectedDay.isBefore(range.from()) || selectedDay.isAfter(range.to()))) {
      selectedDay = null;
    }
    var from = selectedDay == null ? range.from() : selectedDay;
    var to = selectedDay == null ? range.to() : selectedDay;
    var query = q == null || q.isBlank() ? null : q.strip();
    var entries =
        entryClient.search(
            bookId, userId, from.toString(), to.toString(), query, categoryId, assetId);
    var assets = assetClient.list(bookId, userId);
    var categories = categoryClient.list(bookId, userId);

    var form = forced;
    if (form == null) {
      form = EntryForm.closed();
      if (edit != null) {
        form =
            entries.stream()
                .filter(e -> e.id().equals(edit))
                .findFirst()
                .map(EntryForm::of)
                .orElse(form);
      } else if (newEntry != null) {
        var formDate = BookkeepingView.parseDate(date);
        if (formDate == null) {
          formDate = selectedDay != null ? selectedDay : defaultDate(range);
        }
        form =
            new EntryForm(
                true,
                null,
                type == null ? EntryType.EXPENSE : type,
                formDate.toString(),
                "",
                "",
                "",
                null,
                null,
                null,
                "",
                "");
      }
    }
    if (form.open() && type != null && form.entryId() == null) {
      form = withType(form, type);
    }

    model.addAttribute(
        "page",
        new MonthPage(
            book,
            range,
            BookkeepingView.totals(stats.daily()),
            BookkeepingView.calendar(range, stats.daily()),
            entries,
            selectedDay,
            query == null ? "" : query,
            categoryId,
            assetId,
            assets,
            BookkeepingView.assetsById(assets),
            categories,
            BookkeepingView.categoriesById(categories),
            form));
    return "bookkeeping/month";
  }

  /**
   * 내역 추가(entryId 없음) · 수정. next=continue 면 같은 날짜 · 종류로 모달을 다시 연다.
   *
   * <p>분류는 종류마다 다른 칸(incomeCategoryId · expenseCategoryId)으로 받는다 — 한 칸이면 종류를 바꿨을 때 다른 종류의 분류가 남는다.
   */
  @PostMapping("/{bookId}/entries")
  public String saveEntry(
      @PathVariable UUID bookId,
      @RequestParam(required = false) UUID entryId,
      @RequestParam EntryType type,
      @RequestParam String entryDate,
      @RequestParam(required = false) String entryTime,
      @RequestParam String amount,
      @RequestParam(required = false) String toAmount,
      @RequestParam UUID assetId,
      @RequestParam(required = false) UUID toAssetId,
      @RequestParam(required = false) UUID incomeCategoryId,
      @RequestParam(required = false) UUID expenseCategoryId,
      @RequestParam(required = false) String content,
      @RequestParam(required = false) String memo,
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String next,
      HttpServletResponse response,
      Model model) {
    var userId = UserUtil.requireUserId();
    var categoryId =
        switch (type) {
          case INCOME -> incomeCategoryId;
          case EXPENSE -> expenseCategoryId;
          case TRANSFER -> null;
        };
    var form =
        new EntryForm(
            true,
            entryId,
            type,
            entryDate,
            nullToEmpty(entryTime),
            amount,
            nullToEmpty(toAmount),
            assetId,
            type == EntryType.TRANSFER ? toAssetId : null,
            categoryId,
            nullToEmpty(content),
            nullToEmpty(memo));
    var back = monthUrl(bookId, month);
    try {
      var command =
          new EntryCommand(
              type,
              LocalDate.parse(entryDate),
              entryTime == null || entryTime.isBlank() ? null : LocalTime.parse(entryTime),
              number(amount),
              type == EntryType.TRANSFER ? number(toAmount) : null,
              assetId,
              type == EntryType.TRANSFER ? toAssetId : null,
              categoryId,
              content,
              memo);
      if (entryId == null) {
        entryClient.create(bookId, userId, command);
      } else {
        entryClient.update(bookId, entryId, userId, command);
      }
    } catch (DateTimeParseException | NumberFormatException e) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute(ERROR, BookkeepingView.msg("bookkeeping.error.input"));
      return monthView(
          bookId, userId, month, null, null, null, null, null, null, null, null, form, response,
          model);
    } catch (BlueskyException e) {
      failed(e, response, model);
      return monthView(
          bookId, userId, month, null, null, null, null, null, null, null, null, form, response,
          model);
    }
    if ("continue".equals(next)) {
      return "redirect:"
          + monthUrl(bookId, month, "new", "1", "type", type.name(), "date", entryDate);
    }
    return "redirect:" + back;
  }

  @PostMapping("/{bookId}/entries/{entryId}/delete")
  public String deleteEntry(
      @PathVariable UUID bookId,
      @PathVariable UUID entryId,
      @RequestParam(required = false) String month,
      HttpServletResponse response,
      Model model) {
    var userId = UserUtil.requireUserId();
    try {
      entryClient.delete(bookId, entryId, userId);
    } catch (BlueskyException e) {
      failed(e, response, model);
      return monthView(
          bookId, userId, month, null, null, null, null, null, null, null, null, null, response,
          model);
    }
    return "redirect:" + monthUrl(bookId, month);
  }

  // ---- 도우미

  /** 오늘이 기간 안이면 오늘, 아니면 기간 마지막 날. */
  static LocalDate defaultDate(MonthRange range) {
    var today = LocalDate.now();
    return today.isBefore(range.from()) || today.isAfter(range.to()) ? range.to() : today;
  }

  static String monthUrl(UUID bookId, String month, String... pairs) {
    return BookkeepingView.href(bookId, BookkeepingView.parseMonth(month), pairs);
  }

  /** "12,000" 처럼 쓴 금액도 받는다. 비우면 null(API 가 INVALID_AMOUNT 로 거절). */
  static BigDecimal number(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return new BigDecimal(value.replace(",", "").strip());
  }

  private static EntryForm withType(EntryForm f, EntryType type) {
    return new EntryForm(
        f.open(),
        f.entryId(),
        type,
        f.date(),
        f.time(),
        f.amount(),
        f.toAmount(),
        f.assetId(),
        f.toAssetId(),
        f.categoryId(),
        f.content(),
        f.memo());
  }

  private static String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  private static String failure(BlueskyException e) {
    return StockViewSupport.failureMessage(e, BookkeepingView.msg("bookkeeping.error.action"));
  }

  /** 실패: 로그를 남기고 백엔드 사유(없으면 일반 문구)를 화면에 싣는다. 5xx 는 그대로 던진다. */
  static void failed(BlueskyException e, HttpServletResponse response, Model model) {
    if (e.getStatus() >= 500) {
      throw e;
    }
    log.warn("bookkeeping action failed", e);
    response.setStatus(e.getStatus() > 0 ? e.getStatus() : HttpStatus.BAD_REQUEST.value());
    model.addAttribute(ERROR, failure(e));
  }

  /** 멤버가 아니거나 없는 가계부. 있는지 여부는 알리지 않는다. */
  static String forbidden(BlueskyException e, HttpServletResponse response, Model model) {
    if (e.getStatus() >= 500) {
      throw e;
    }
    log.warn("bookkeeping book not accessible: {}", e.getErrorCode());
    response.setStatus(HttpStatus.FORBIDDEN.value());
    model.addAttribute(ERROR, BookkeepingView.msg("bookkeeping.error.forbidden"));
    return FORBIDDEN_VIEW;
  }
}
