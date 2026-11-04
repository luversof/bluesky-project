package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
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
import net.luversof.web.gate.bookkeeping.httpexchange.FavoriteClient.Favorite;

/** 즐겨찾기: 모달 채우기 · 단추 · 즐겨찾기로 저장 · 설정 목록. */
class BookkeepingFavoriteTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;
  static final Favorite LUNCH =
      new Favorite(
          UUID.randomUUID(),
          BOOK,
          "점심 <김밥>",
          EntryType.EXPENSE,
          new BigDecimal("9000.0000"),
          null,
          BookkeepingScreenTest.CASH.id(),
          null,
          BookkeepingScreenTest.COFFEE.id(),
          "김밥",
          null,
          0,
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
  void favoriteFillsNewEntryForm() {
    var form = EntryForm.of(LUNCH, "2026-11-03");
    assertThat(form.open()).isTrue();
    assertThat(form.entryId()).isNull();
    assertThat(form.amount()).isEqualTo("9000");
    assertThat(form.toAmount()).isEmpty();
    assertThat(form.categoryId()).isEqualTo(BookkeepingScreenTest.COFFEE.id());
    assertThat(form.content()).isEqualTo("김밥");
    assertThat(form.memo()).isEmpty();
    assertThat(form.date()).isEqualTo("2026-11-03");
  }

  @Test
  void newEntryModalShowsFavoriteButtonsAndSaveAs() {
    var newForm = EntryForm.of(LUNCH, "2026-11-03");
    var html = render(with(newForm, List.of(LUNCH)));
    assertThat(html).contains("fav=" + LUNCH.id()).contains("점심 &lt;김밥&gt;");
    assertThat(html).contains("formaction=\"/bookkeeping/" + BOOK + "/favorites\"");
    assertThat(html).contains("name=\"favoriteName\"").contains("formnovalidate");
    assertThat(html).contains("value=\"9000\"");

    // 수정 모달에는 단추가 없고 "즐겨찾기로 저장"은 있다
    var lunchEntry =
        BookkeepingScreenTest.entry(
            EntryType.EXPENSE,
            java.time.LocalDate.of(2026, 11, 3),
            "12000",
            null,
            BookkeepingScreenTest.CASH.id(),
            null,
            BookkeepingScreenTest.COFFEE.id(),
            "점심");
    var editHtml = render(with(EntryForm.of(lunchEntry), List.of(LUNCH)));
    assertThat(editHtml).doesNotContain("fav=" + LUNCH.id()).contains("name=\"favoriteName\"");
  }

  @Test
  void settingsListsFavoritesWithDelete() {
    var html = BookkeepingSettingsScreenTest.render(settings(List.of(LUNCH)));
    assertThat(html)
        .contains("/settings/favorites/" + LUNCH.id() + "/delete")
        .contains("점심 &lt;김밥&gt;");
    assertThat(BookkeepingSettingsScreenTest.render(settings(List.of()))).contains("아직 없습니다.");
    assertThat(BookkeepingSettingsScreenTest.render(settings(null))).contains("즐겨찾기를 불러오지 못했습니다.");
  }

  static SettingsPage settings(List<Favorite> favorites) {
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
        favorites,
        List.of());
  }

  static MonthPage with(EntryForm form, List<Favorite> favorites) {
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
        favorites,
        p.recurringDue(),
        p.fx());
  }

  static String render(MonthPage page) {
    return BookkeepingScreenTest.render("bookkeeping/month.jte", Map.of("page", page));
  }
}
