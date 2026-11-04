package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.AssetKind;
import net.luversof.web.gate.bookkeeping.constant.BookRole;
import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.MemberRow;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingSettingsViewController.SettingsPage;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.Asset;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;

/** 설정 화면 렌더: OWNER / MEMBER 보기 · 수정 폼 · 초대 링크 · 알림. */
class BookkeepingSettingsScreenTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;
  static final UUID ME = UUID.randomUUID();
  static final UUID OTHER = UUID.randomUUID();
  static final Category FOOD =
      new Category(UUID.randomUUID(), BOOK, null, CategoryKind.EXPENSE, "식비", 0, false);
  static final Category COFFEE =
      new Category(UUID.randomUUID(), BOOK, FOOD.id(), CategoryKind.EXPENSE, "커피", 0, true);
  static final Asset CARD =
      new Asset(
          UUID.randomUUID(),
          BOOK,
          AssetKind.CREDIT_CARD,
          "카드",
          "KRW",
          new BigDecimal("-5000.0000"),
          14,
          0,
          false);

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void ownerSeesEditDeleteInviteAndKick() {
    var html =
        render(page(BookRole.OWNER, null, null, "https://gate/bookkeeping/invite/tok", "저장했습니다."));
    assertThat(html).contains("<title>우리집 설정 · Bluesky 가계부</title>");
    assertThat(html)
        .contains("/settings/book")
        .contains("/settings/delete")
        .contains("name=\"confirm\"");
    assertThat(html)
        .contains("/settings/invites")
        .contains("value=\"https://gate/bookkeeping/invite/tok\"");
    assertThat(html)
        .contains("/members/" + OTHER + "/remove")
        .doesNotContain("/members/" + ME + "/remove");
    assertThat(html).contains("내보내기").doesNotContain(">나가기<");
    assertThat(html)
        .contains("/members/" + OTHER + "/owner")
        .doesNotContain("/members/" + ME + "/owner");
    assertThat(html).contains("친구님에게 소유자를 넘깁니다");
    assertThat(html).contains("role=\"status\"").contains("저장했습니다.");
    assertThat(html).contains("· 커피").contains("숨김"); // 소분류 · 숨긴 표시
    assertThat(html)
        .contains("/settings/categories/" + FOOD.id() + "/move")
        .contains("/settings/assets/" + CARD.id() + "/delete");
  }

  @Test
  void memberCannotEditBookOrInviteButCanLeave() {
    var html = render(page(BookRole.MEMBER, null, null, null, null));
    assertThat(html)
        .doesNotContain("/settings/book")
        .doesNotContain("/settings/delete")
        .doesNotContain("/settings/invites");
    assertThat(html).contains("/members/" + ME + "/remove").contains("나가기");
    assertThat(html).doesNotContain("/members/" + OTHER + "/remove").doesNotContain("/owner\"");
  }

  @Test
  void editFormsPrefillValues() {
    var html = render(page(BookRole.OWNER, CARD.id(), COFFEE.id(), null, null));
    assertThat(html)
        .contains("action=\"/bookkeeping/" + BOOK + "/settings/assets/" + CARD.id() + "\"");
    assertThat(html).contains("value=\"-5000\"").contains("value=\"14\"");
    assertThat(html)
        .contains("action=\"/bookkeeping/" + BOOK + "/settings/categories/" + COFFEE.id() + "\"");
    assertThat(html).contains("value=\"커피\"");
  }

  static String render(SettingsPage page) {
    return BookkeepingScreenTest.render(
        "bookkeeping/settings.jte", Map.of("page", page, "currencies", BookkeepingView.CURRENCIES));
  }

  static SettingsPage page(
      BookRole myRole, UUID editAsset, UUID editCategory, String inviteUrl, String notice) {
    var owner = myRole == BookRole.OWNER;
    var members =
        List.of(
            new MemberRow(owner ? ME : OTHER, owner ? "나" : "주인", BookRole.OWNER, owner),
            new MemberRow(owner ? OTHER : ME, owner ? "친구" : "나", BookRole.MEMBER, !owner));
    return new SettingsPage(
        BookkeepingScreenTest.book(),
        myRole,
        List.of(FOOD, COFFEE),
        List.of(new AssetBalance(CARD, new BigDecimal("-5000"))),
        members,
        editAsset,
        editCategory,
        inviteUrl,
        notice,
        List.of(),
        List.of(),
        List.of());
  }
}
