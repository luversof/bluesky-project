package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.BookRole;
import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.EntryForm;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.MonthPage;
import net.luversof.web.gate.bookkeeping.httpexchange.RecurringClient.Due;
import net.luversof.web.gate.bookkeeping.httpexchange.RecurringClient.Recurring;
import net.luversof.web.gate.bookkeeping.support.RecurringView;

/** 반복: 주기 문구 · 월 화면 예정 반복(넣기) · 모달 반복으로 저장 · 설정 목록. */
class BookkeepingRecurringTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;
  static final Recurring RENT =
      new Recurring(
          UUID.randomUUID(),
          BOOK,
          EntryType.EXPENSE,
          new BigDecimal("500000"),
          null,
          BookkeepingScreenTest.CASH.id(),
          null,
          BookkeepingScreenTest.FOOD.id(),
          "월세 <집>",
          null,
          "MONTHLY",
          31,
          null,
          null,
          LocalDate.of(2026, 1, 31),
          null,
          UUID.randomUUID(),
          Instant.now());

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void cycleLabels() {
    assertThat(RecurringView.cycleLabel(RENT)).isEqualTo("매월 31일");
    var date = LocalDate.of(2026, 11, 3); // 화요일
    assertThat(RecurringView.cycleOption("WEEKLY", date)).isEqualTo("매주 화요일");
    assertThat(RecurringView.cycleOption("YEARLY", date)).isEqualTo("매년 11월 3일");
    assertThat(RecurringView.cycleOption("MONTHLY", date)).isEqualTo("매월 3일");
  }

  @Test
  void monthShowsDueWithConfirmFormOrNote() {
    var html = render(with(EntryForm.closed(), List.of(new Due(RENT, LocalDate.of(2026, 11, 24)))));
    assertThat(html).contains("예정 반복 1건").contains("식비 · 월세 &lt;집&gt;");
    assertThat(html)
        .contains("action=\"/bookkeeping/" + BOOK + "/recurrings/" + RENT.id() + "/confirm\"")
        .contains("name=\"date\" value=\"2026-11-24\"")
        .contains("-500,000");
    assertThat(render(with(EntryForm.closed(), List.of()))).doesNotContain("bk-recurring-title");
    assertThat(render(with(EntryForm.closed(), null)))
        .contains("반복 내역을 불러오지 못했습니다.")
        .doesNotContain("bk-recurring-title");
  }

  @Test
  void entryModalOffersSaveAsRecurringFromItsDate() {
    var form =
        new EntryForm(
            true, null, EntryType.EXPENSE, "2026-11-03", "", "", "", null, null, null, "", "");
    var html = render(with(form, List.of()));
    assertThat(html)
        .contains("formaction=\"/bookkeeping/" + BOOK + "/recurrings\"")
        .contains("<option value=\"WEEKLY\">매주 화요일</option>")
        .contains("name=\"endDate\"");
  }

  @Test
  void settingsListsRecurringsWithDelete() {
    var html = BookkeepingSettingsScreenTest.render(settings(List.of(RENT)));
    assertThat(html)
        .contains("/settings/recurrings/" + RENT.id() + "/delete")
        .contains("매월 31일 · 2026-01-31 ~ ");
    assertThat(BookkeepingSettingsScreenTest.render(settings(null))).contains("반복 내역을 불러오지 못했습니다.");
  }

  static SettingsPage settings(List<Recurring> recurrings) {
    var p = BookkeepingSettingsScreenTest.page(BookRole.MEMBER, null, null, null, null);
    return new SettingsPage(
        p.book(),
        p.myRole(),
        p.categories(),
        p.assets(),
        p.members(),
        null,
        null,
        null,
        null,
        List.of(),
        List.of(),
        recurrings);
  }

  static MonthPage with(EntryForm form, List<Due> due) {
    var p = BookkeepingScreenTest.page(List.of(), List.of(), form);
    return new MonthPage(
        p.book(),
        p.range(),
        p.totals(),
        p.weeks(),
        p.entries(),
        p.day(),
        p.q(),
        p.categoryId(),
        p.assetId(),
        p.assets(),
        p.assetsById(),
        p.categories(),
        p.categoriesById(),
        p.form(),
        p.budget(),
        p.favorites(),
        due,
        p.fx());
  }

  static String render(MonthPage page) {
    return BookkeepingScreenTest.render("bookkeeping/month.jte", Map.of("page", page));
  }
}
