package net.luversof.web.gate.bookkeeping.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.board.service.BoardUserInfoService;
import net.luversof.web.gate.bookkeeping.constant.AssetKind;
import net.luversof.web.gate.bookkeeping.constant.BookRole;
import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetCommand;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.Book;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.BookCommand;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.Budget;
import net.luversof.web.gate.bookkeeping.httpexchange.BudgetClient.BudgetCommand;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.CategoryCommand;
import net.luversof.web.gate.bookkeeping.httpexchange.FavoriteClient;
import net.luversof.web.gate.bookkeeping.httpexchange.FavoriteClient.Favorite;
import net.luversof.web.gate.bookkeeping.httpexchange.MemberClient;
import net.luversof.web.gate.bookkeeping.httpexchange.RecurringClient;
import net.luversof.web.gate.bookkeeping.httpexchange.RecurringClient.Recurring;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.stock.support.StockViewSupport;

/**
 * 설정 화면: 가계부 정보 · 분류 · 자산 · 멤버.
 *
 * <p>쓰기는 성공하면 설정 화면으로 돌려보내고(PRG, 알릴 것은 ?notice=), 실패하면 그 자리에서 오류와 함께 다시 그린다(플래시를 쓰지 않는 이유는
 * BookkeepingViewController.monthView 주석). 권한(OWNER 전용 등)은 api-bookkeeping 이 판정하고, 화면은 역할에 맞는 단추만
 * 보인다.
 */
@Controller
@RequestMapping(value = "/bookkeeping/{bookId}/settings", produces = MediaType.TEXT_HTML_VALUE)
public class BookkeepingSettingsViewController {

  private static final Logger log =
      LoggerFactory.getLogger(BookkeepingSettingsViewController.class);

  /** 예산 입력 칸 이름: budget.total(전체) · budget.<지출 대분류 id>. */
  static final String BUDGET_PREFIX = "budget.";

  static final String BUDGET_TOTAL = BUDGET_PREFIX + "total";

  /** 멤버 한 줄. */
  public record MemberRow(UUID userId, String name, BookRole role, boolean me) {}

  /**
   * 설정 화면 한 장. editAsset · editCategory 는 그 줄을 수정 폼으로 그린다. inviteUrl 은 방금 만든 초대 링크. budgets 가 null
   * 이면 예산을 불러오지 못했다.
   */
  public record SettingsPage(
      Book book,
      BookRole myRole,
      List<Category> categories,
      List<AssetBalance> assets,
      List<MemberRow> members,
      UUID editAsset,
      UUID editCategory,
      String inviteUrl,
      String notice,
      List<Budget> budgets,
      List<Favorite> favorites,
      List<Recurring> recurrings) {

    public boolean owner() {
      return myRole == BookRole.OWNER;
    }
  }

  @FunctionalInterface
  interface Action {
    String run(UUID userId);
  }

  private final BookClient bookClient;
  private final MemberClient memberClient;
  private final AssetClient assetClient;
  private final CategoryClient categoryClient;
  private final BudgetClient budgetClient;
  private final FavoriteClient favoriteClient;
  private final RecurringClient recurringClient;
  private final BoardUserInfoService userInfoService;

  public BookkeepingSettingsViewController(
      BookClient bookClient,
      MemberClient memberClient,
      AssetClient assetClient,
      CategoryClient categoryClient,
      BudgetClient budgetClient,
      FavoriteClient favoriteClient,
      RecurringClient recurringClient,
      BoardUserInfoService userInfoService) {
    this.bookClient = bookClient;
    this.memberClient = memberClient;
    this.assetClient = assetClient;
    this.categoryClient = categoryClient;
    this.budgetClient = budgetClient;
    this.favoriteClient = favoriteClient;
    this.recurringClient = recurringClient;
    this.userInfoService = userInfoService;
  }

  @GetMapping
  public String settings(
      @PathVariable UUID bookId,
      @RequestParam(required = false) UUID editAsset,
      @RequestParam(required = false) UUID editCategory,
      @RequestParam(required = false) String notice,
      HttpServletRequest request,
      HttpServletResponse response,
      Model model) {
    if (StockViewSupport.isNotAuthenticated()) {
      return StockViewSupport.loginRedirectView(request);
    }
    return view(
        bookId,
        UserUtil.requireUserId(),
        editAsset,
        editCategory,
        null,
        notice(notice),
        response,
        model);
  }

  // ---- 가계부

  @PostMapping("/book")
  public String updateBook(
      @PathVariable UUID bookId,
      @RequestParam String name,
      @RequestParam String baseCurrency,
      @RequestParam(required = false) Integer monthStartDay,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          bookClient.update(bookId, userId, new BookCommand(name, baseCurrency, monthStartDay));
          return back(bookId, "saved");
        },
        response,
        model);
  }

  /** confirm 이 없으면(확인 칸을 안 고름) 지우지 않는다. */
  @PostMapping("/delete")
  public String deleteBook(
      @PathVariable UUID bookId,
      @RequestParam(required = false) String confirm,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          if (confirm == null) {
            return back(bookId, null);
          }
          bookClient.delete(bookId, userId);
          return "redirect:/bookkeeping?all=1";
        },
        response,
        model);
  }

  // ---- 분류

  @PostMapping("/categories")
  public String addCategory(
      @PathVariable UUID bookId,
      @RequestParam(required = false) UUID parentId,
      @RequestParam(required = false) CategoryKind kind,
      @RequestParam String name,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          categoryClient.create(bookId, userId, new CategoryCommand(parentId, kind, name, null));
          return back(bookId, "saved");
        },
        response,
        model);
  }

  @PostMapping("/categories/{categoryId}")
  public String updateCategory(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam String name,
      @RequestParam(required = false) String hidden,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          categoryClient.update(
              bookId, categoryId, userId, new CategoryCommand(null, null, name, hidden != null));
          return back(bookId, "saved");
        },
        response,
        model);
  }

  @PostMapping("/categories/{categoryId}/delete")
  public String deleteCategory(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId ->
            back(
                bookId,
                categoryClient.delete(bookId, categoryId, userId).deleted() ? "deleted" : "hidden"),
        response,
        model);
  }

  @PostMapping("/categories/{categoryId}/move")
  public String moveCategory(
      @PathVariable UUID bookId,
      @PathVariable UUID categoryId,
      @RequestParam String direction,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          categoryClient.move(bookId, categoryId, userId, direction);
          return back(bookId, null);
        },
        response,
        model);
  }

  // ---- 즐겨찾기

  @PostMapping("/favorites/{favoriteId}/delete")
  public String deleteFavorite(
      @PathVariable UUID bookId,
      @PathVariable UUID favoriteId,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          favoriteClient.delete(bookId, favoriteId, userId);
          return back(bookId, "deleted");
        },
        response,
        model);
  }

  // ---- 반복

  @PostMapping("/recurrings/{recurringId}/delete")
  public String deleteRecurring(
      @PathVariable UUID bookId,
      @PathVariable UUID recurringId,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          recurringClient.delete(bookId, recurringId, userId);
          return back(bookId, "deleted");
        },
        response,
        model);
  }

  // ---- 예산

  /** 바뀐 칸만 보낸다(빈 칸 = 지우기). 칸 이름이 틀리면(남의 id 등) API 가 거절한다. */
  @PostMapping("/budgets")
  public String saveBudgets(
      @PathVariable UUID bookId,
      @RequestParam Map<String, String> params,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          var current = budgetClient.list(bookId, userId);
          for (var p : params.entrySet()) {
            if (!p.getKey().startsWith(BUDGET_PREFIX)) {
              continue;
            }
            var total = BUDGET_TOTAL.equals(p.getKey());
            var categoryId = total ? null : uuid(p.getKey().substring(BUDGET_PREFIX.length()));
            if (!total && categoryId == null) {
              continue; // 화면이 만든 칸이 아니다
            }
            var amount = number(p.getValue());
            var before =
                current.stream()
                    .filter(b -> Objects.equals(b.categoryId(), categoryId))
                    .map(Budget::amount)
                    .findFirst()
                    .orElse(null);
            if (!sameAmount(before, amount)) {
              budgetClient.set(bookId, userId, new BudgetCommand(categoryId, amount));
            }
          }
          return back(bookId, "saved");
        },
        response,
        model);
  }

  private static UUID uuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  static boolean sameAmount(BigDecimal a, BigDecimal b) {
    return a == null || b == null ? a == b : a.compareTo(b) == 0;
  }

  // ---- 자산

  @PostMapping("/assets")
  public String addAsset(
      @PathVariable UUID bookId,
      @RequestParam AssetKind kind,
      @RequestParam String name,
      @RequestParam String currency,
      @RequestParam(required = false) String openingBalance,
      @RequestParam(required = false) Integer paymentDay,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          assetClient.create(
              bookId,
              userId,
              new AssetCommand(kind, name, currency, number(openingBalance), paymentDay, null));
          return back(bookId, "saved");
        },
        response,
        model);
  }

  @PostMapping("/assets/{assetId}")
  public String updateAsset(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam AssetKind kind,
      @RequestParam String name,
      @RequestParam String currency,
      @RequestParam(required = false) String openingBalance,
      @RequestParam(required = false) Integer paymentDay,
      @RequestParam(required = false) String hidden,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          assetClient.update(
              bookId,
              assetId,
              userId,
              new AssetCommand(
                  kind, name, currency, number(openingBalance), paymentDay, hidden != null));
          return back(bookId, "saved");
        },
        response,
        model);
  }

  @PostMapping("/assets/{assetId}/delete")
  public String deleteAsset(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId ->
            back(
                bookId,
                assetClient.delete(bookId, assetId, userId).deleted() ? "deleted" : "hidden"),
        response,
        model);
  }

  @PostMapping("/assets/{assetId}/move")
  public String moveAsset(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam String direction,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          assetClient.move(bookId, assetId, userId, direction);
          return back(bookId, null);
        },
        response,
        model);
  }

  // ---- 멤버

  /** 초대 링크는 한 번만 보여 준다(다시 볼 수 없음) - 리다이렉트하지 않고 바로 그린다. */
  @PostMapping("/invites")
  public String createInvite(@PathVariable UUID bookId, HttpServletResponse response, Model model) {
    var userId = UserUtil.requireUserId();
    String url;
    try {
      var invite = memberClient.createInvite(bookId, userId);
      url =
          ServletUriComponentsBuilder.fromCurrentContextPath()
              .path("/bookkeeping/invite/{token}")
              .buildAndExpand(invite.token())
              .toUriString();
    } catch (BlueskyException e) {
      BookkeepingViewController.failed(e, response, model);
      return view(bookId, userId, null, null, null, null, response, model);
    }
    return view(bookId, userId, null, null, url, null, response, model);
  }

  /** 본인이면 나가기(목록으로), 남이면 추방(OWNER). */
  @PostMapping("/members/{memberUserId}/remove")
  public String removeMember(
      @PathVariable UUID bookId,
      @PathVariable UUID memberUserId,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          memberClient.remove(bookId, memberUserId, userId);
          return userId.equals(memberUserId)
              ? "redirect:/bookkeeping?all=1"
              : back(bookId, "saved");
        },
        response,
        model);
  }

  /** OWNER 넘기기. confirm(확인 칸)이 없으면 아무것도 하지 않는다. */
  @PostMapping("/members/{memberUserId}/owner")
  public String transferOwner(
      @PathVariable UUID bookId,
      @PathVariable UUID memberUserId,
      @RequestParam(required = false) String confirm,
      HttpServletResponse response,
      Model model) {
    return act(
        bookId,
        userId -> {
          if (confirm == null) {
            return back(bookId, null);
          }
          memberClient.transferOwner(bookId, memberUserId, userId);
          return back(bookId, "saved");
        },
        response,
        model);
  }

  // ---- 도우미

  private String act(UUID bookId, Action action, HttpServletResponse response, Model model) {
    var userId = UserUtil.requireUserId();
    try {
      return action.run(userId);
    } catch (NumberFormatException e) {
      response.setStatus(HttpStatus.BAD_REQUEST.value());
      model.addAttribute(
          BookkeepingViewController.ERROR, BookkeepingView.msg("bookkeeping.error.input"));
    } catch (BlueskyException e) {
      BookkeepingViewController.failed(e, response, model);
    }
    return view(bookId, userId, null, null, null, null, response, model);
  }

  private String view(
      UUID bookId,
      UUID userId,
      UUID editAsset,
      UUID editCategory,
      String inviteUrl,
      String notice,
      HttpServletResponse response,
      Model model) {
    Book book;
    try {
      book = bookClient.get(bookId, userId);
    } catch (BlueskyException e) {
      return BookkeepingViewController.forbidden(e, response, model);
    }
    var members = memberClient.members(bookId, userId);
    var names = userInfoService.fetchUsernames(members.stream().map(m -> m.userId()).toList());
    var rows =
        members.stream()
            .map(
                m ->
                    new MemberRow(
                        m.userId(),
                        names.getOrDefault(
                            m.userId(), BookkeepingView.msg("bookkeeping.members.unknown")),
                        m.role(),
                        m.userId().equals(userId)))
            .toList();
    var myRole =
        rows.stream()
            .filter(MemberRow::me)
            .map(MemberRow::role)
            .findFirst()
            .orElse(BookRole.MEMBER);
    model.addAttribute(
        "page",
        new SettingsPage(
            book,
            myRole,
            categoryClient.list(bookId, userId),
            assetClient.list(bookId, userId),
            rows,
            editAsset,
            editCategory,
            inviteUrl,
            notice,
            budgets(bookId, userId),
            favoritesOrNull(bookId, userId),
            recurringsOrNull(bookId, userId)));
    model.addAttribute("currencies", BookkeepingView.CURRENCIES);
    return "bookkeeping/settings";
  }

  /** 예산은 곁가지 - 못 불러와도 설정 화면은 뜬다(null = 안내만). */
  private List<Budget> budgets(UUID bookId, UUID userId) {
    try {
      return budgetClient.list(bookId, userId);
    } catch (RuntimeException e) {
      log.warn("bookkeeping budgets unavailable", e);
      return null;
    }
  }

  /** null = 못 불러옴(안내만). */
  private List<Recurring> recurringsOrNull(UUID bookId, UUID userId) {
    try {
      return recurringClient.list(bookId, userId);
    } catch (RuntimeException e) {
      log.warn("bookkeeping recurrings unavailable", e);
      return null;
    }
  }

  /** null = 못 불러옴(안내만). */
  private List<Favorite> favoritesOrNull(UUID bookId, UUID userId) {
    try {
      return favoriteClient.list(bookId, userId);
    } catch (RuntimeException e) {
      log.warn("bookkeeping favorites unavailable", e);
      return null;
    }
  }

  private static String back(UUID bookId, String notice) {
    return "redirect:/bookkeeping/"
        + bookId
        + "/settings"
        + (notice == null ? "" : "?notice=" + notice);
  }

  /** 알림 키는 정해진 것만 받는다(주소로 아무 문구나 띄우지 못하게). */
  private static String notice(String notice) {
    return notice != null && List.of("saved", "deleted", "hidden").contains(notice)
        ? BookkeepingView.msg("bookkeeping.settings.notice." + notice)
        : null;
  }

  private static BigDecimal number(String value) {
    return BookkeepingViewController.number(value);
  }
}
