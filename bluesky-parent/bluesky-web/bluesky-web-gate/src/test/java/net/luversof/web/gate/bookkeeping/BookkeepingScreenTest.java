package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.bookkeeping.constant.AssetKind;
import net.luversof.web.gate.bookkeeping.constant.BookRole;
import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.EntryForm;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.Asset;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.Book;
import net.luversof.web.gate.bookkeeping.httpexchange.BookClient.BookSummary;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;
import net.luversof.web.gate.bookkeeping.httpexchange.MemberClient.InvitePreview;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.DailyTotal;
import net.luversof.web.gate.bookkeeping.httpexchange.StatsClient.MonthRange;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;

/** 가계부 화면 렌더(미리 컴파일된 jte) · 표시 도우미. 원격 호출 없이 가짜 데이터로 그린다. */
class BookkeepingScreenTest {

  static final UUID BOOK = UUID.fromString("00000000-0000-0000-0000-000000000001");
  static final Asset CASH =
      new Asset(
          UUID.randomUUID(), BOOK, AssetKind.CASH, "현금", "KRW", BigDecimal.ZERO, null, 0, false);
  static final Asset USD =
      new Asset(
          UUID.randomUUID(), BOOK, AssetKind.BANK, "달러 통장", "USD", BigDecimal.ZERO, null, 1, false);
  static final Asset OLD =
      new Asset(
          UUID.randomUUID(), BOOK, AssetKind.BANK, "숨긴 통장", "KRW", BigDecimal.ZERO, null, 2, true);
  static final Category FOOD =
      new Category(UUID.randomUUID(), BOOK, null, CategoryKind.EXPENSE, "식비", 0, false);
  static final Category COFFEE =
      new Category(UUID.randomUUID(), BOOK, FOOD.id(), CategoryKind.EXPENSE, "커피", 0, false);
  static final Category SALARY =
      new Category(UUID.randomUUID(), BOOK, null, CategoryKind.INCOME, "급여", 0, false);
  static final MonthRange RANGE =
      new MonthRange(
          YearMonth.of(2026, 11), LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 24));

  @BeforeAll
  static void messages() {
    var source = new ReloadableResourceBundleMessageSource();
    source.setBasenames("classpath:bookkeepingUiMessage", "classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
    LocaleContextHolder.setLocale(Locale.KOREAN);
  }

  @AfterAll
  static void reset() {
    MessageUtil.setMessageSourceAccessor(null);
    LocaleContextHolder.resetLocaleContext();
  }

  // ---- 표시 도우미

  @Test
  void calendarStartsSundayAndBlanksOutsideRange() {
    var weeks = BookkeepingView.calendar(RANGE, List.of());
    assertThat(weeks).hasSize(5).allSatisfy(w -> assertThat(w).hasSize(7));
    assertThat(weeks.get(0).get(0).date()).isEqualTo(LocalDate.of(2026, 10, 25)); // 일요일
    assertThat(weeks.get(4).get(2).date()).isEqualTo(LocalDate.of(2026, 11, 24));
    assertThat(weeks.get(4).get(3).date()).isNull();
    var calendarMonth =
        new MonthRange(
            YearMonth.of(2026, 11), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
    var nov = BookkeepingView.calendar(calendarMonth, List.of());
    assertThat(nov.get(0).get(0).date()).isEqualTo(LocalDate.of(2026, 11, 1)); // 2026-11-01 은 일요일
    assertThat(nov.stream().flatMap(List::stream).filter(c -> c.date() != null)).hasSize(30);
  }

  @Test
  void totalsPerCurrencyAndAmountDigits() {
    var totals =
        BookkeepingView.totals(
            List.of(
                new DailyTotal(
                    LocalDate.of(2026, 11, 1), "KRW", new BigDecimal("3000000"), BigDecimal.ZERO),
                new DailyTotal(
                    LocalDate.of(2026, 11, 2), "KRW", BigDecimal.ZERO, new BigDecimal("12000")),
                new DailyTotal(
                    LocalDate.of(2026, 11, 2), "USD", BigDecimal.ZERO, new BigDecimal("3.5"))));
    assertThat(totals)
        .extracting(t -> t.currency() + " " + t.net().toPlainString())
        .containsExactly("KRW 2988000", "USD -3.5");
    assertThat(BookkeepingView.amount(new BigDecimal("2988000"), "KRW")).isEqualTo("2,988,000");
    assertThat(BookkeepingView.amount(new BigDecimal("3.5"), "USD")).isEqualTo("3.50");
    assertThat(BookkeepingView.plainAmount(new BigDecimal("12000.0000"))).isEqualTo("12000");
  }

  @Test
  void hrefSkipsEmptyValues() {
    assertThat(BookkeepingView.href(BOOK, YearMonth.of(2026, 11), "day", null, "q", "", "new", "1"))
        .isEqualTo("/bookkeeping/" + BOOK + "?month=2026-11&new=1");
    assertThat(BookkeepingView.href(BOOK, null, "q", "커피 & 빵"))
        .isEqualTo("/bookkeeping/" + BOOK + "?q=%EC%BB%A4%ED%94%BC%20%26%20%EB%B9%B5");
  }

  @Test
  void optionsHideHiddenButKeepSelected() {
    var assets = List.of(balance(CASH), balance(USD), balance(OLD));
    assertThat(BookkeepingView.assetOptions(assets, null))
        .extracting(o -> o.label())
        .containsExactly("현금 (KRW)", "달러 통장 (USD)");
    assertThat(BookkeepingView.assetOptions(assets, OLD.id())).hasSize(3);
    var categories = List.of(FOOD, SALARY, COFFEE);
    assertThat(BookkeepingView.categoryOptions(categories, CategoryKind.EXPENSE, null))
        .extracting(o -> o.label() + (o.child() ? "(child)" : ""))
        .containsExactly("식비", "커피(child)");
  }

  // ---- 화면 렌더

  @Test
  void monthPageRendersSummaryCalendarEntriesAndModal() {
    var lunch =
        entry(
            EntryType.EXPENSE,
            LocalDate.of(2026, 11, 3),
            "12000",
            null,
            CASH.id(),
            null,
            COFFEE.id(),
            "점심 <b>");
    var fx =
        entry(
            EntryType.TRANSFER,
            LocalDate.of(2026, 11, 2),
            "140000",
            "100.25",
            CASH.id(),
            USD.id(),
            null,
            null);
    var daily =
        List.of(
            new DailyTotal(
                LocalDate.of(2026, 11, 3), "KRW", BigDecimal.ZERO, new BigDecimal("12000")));
    var html =
        render(
            "bookkeeping/month.jte",
            Map.of("page", page(List.of(lunch, fx), daily, EntryForm.of(lunch))));

    assertThat(html).contains("<title>우리집 2026년 11월 · Bluesky 가계부</title>");
    assertThat(html).contains("10.25 ~ 11.24");
    assertThat(html)
        .contains("action=\"/bookkeeping/" + BOOK + "/export.csv\"")
        .contains("name=\"from\" value=\"2026-10-25\"")
        .contains("name=\"to\" value=\"2026-11-24\"");
    assertThat(html).contains("식비 &gt; 커피").contains("점심 &lt;b&gt;"); // 이스케이프
    assertThat(html).contains("현금 → 달러 통장").contains("→ 100.25 USD");
    assertThat(html).contains("-12,000");
    assertThat(html).contains("aria-labelledby=\"bk-entry-title\"").contains("내역 수정");
    assertThat(html).contains("name=\"entryId\" value=\"" + lunch.id() + "\"");
    assertThat(html).contains("name=\"expenseCategoryId\"").doesNotContain("name=\"toAssetId\"");
    assertThat(html).contains("/entries/" + lunch.id() + "/delete");
    assertThat(html).doesNotContain("숨긴 통장");
    assertThat(html).contains("aria-label=\"달력\"");
  }

  @Test
  void newTransferModalShowsTargetAssetAndToAmount() {
    var form =
        new EntryForm(
            true, null, EntryType.TRANSFER, "2026-11-03", "", "", "", null, null, null, "", "");
    var html = render("bookkeeping/month.jte", Map.of("page", page(List.of(), List.of(), form)));
    assertThat(html).contains("name=\"toAssetId\"").contains("name=\"toAmount\"");
    assertThat(html).doesNotContain("CategoryId\"").doesNotContain("name=\"entryId\"");
    assertThat(html).contains("value=\"continue\"").contains("내역이 없습니다.");
  }

  @Test
  void indexAndInviteRender() {
    var model = new HashMap<String, Object>();
    model.put("books", List.of(new BookSummary(book(), BookRole.MEMBER)));
    model.put("currencies", BookkeepingView.CURRENCIES);
    var index = render("bookkeeping/index.jte", model);
    assertThat(index)
        .contains("우리집")
        .contains("멤버 · KRW")
        .contains("action=\"/bookkeeping\"")
        .contains("25일");

    var invite =
        render(
            "bookkeeping/invite.jte",
            Map.of("token", "tok", "preview", new InvitePreview(BOOK, "우리집", false, false)));
    assertThat(invite).contains("/bookkeeping/invite/tok/accept").contains("참여하기");
    var used =
        render(
            "bookkeeping/invite.jte",
            Map.of("token", "tok", "preview", new InvitePreview(BOOK, "우리집", false, true)));
    assertThat(used).contains("이미 사용한 초대 링크입니다.").doesNotContain("/accept");
  }

  // ---- 도우미

  static String render(String template, Map<String, Object> model) {
    var output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(template, model, output);
    return output.toString();
  }

  static Book book() {
    return new Book(BOOK, "우리집", "KRW", 25, Instant.parse("2026-11-01T00:00:00Z"));
  }

  static AssetBalance balance(Asset a) {
    return new AssetBalance(a, BigDecimal.ZERO);
  }

  static MonthPage page(List<Entry> entries, List<DailyTotal> daily, EntryForm form) {
    var assets = List.of(balance(CASH), balance(USD), balance(OLD));
    var categories = List.of(FOOD, COFFEE, SALARY);
    return new MonthPage(
        book(),
        RANGE,
        BookkeepingView.totals(daily),
        BookkeepingView.calendar(RANGE, daily),
        entries,
        null,
        "",
        null,
        null,
        assets,
        BookkeepingView.assetsById(assets),
        categories,
        BookkeepingView.categoriesById(categories),
        form,
        net.luversof.web.gate.bookkeeping.support.BudgetView.Status.none(),
        List.of(),
        List.of(),
        null);
  }

  static Entry entry(
      EntryType type,
      LocalDate day,
      String amount,
      String toAmount,
      UUID asset,
      UUID toAsset,
      UUID category,
      String content) {
    return new Entry(
        UUID.randomUUID(),
        BOOK,
        type,
        day,
        null,
        new BigDecimal(amount),
        toAmount == null ? null : new BigDecimal(toAmount),
        asset,
        toAsset,
        category,
        content,
        null,
        null,
        UUID.randomUUID(),
        Instant.now());
  }
}
