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
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.Asset;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.support.AssetGroups;

/** 자산 화면: 종류별 묶음 · 통화별 합계 · 숨긴 자산 · 렌더. */
class BookkeepingAssetScreenTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void groupsByKindInEnumOrderAndSumsPerCurrency() {
    var page =
        AssetGroups.of(
            List.of(
                balance(AssetKind.CREDIT_CARD, "카드", "KRW", "-120000", false, 14),
                balance(AssetKind.CASH, "현금", "KRW", "50000", false, null),
                balance(AssetKind.BANK, "통장", "KRW", "1000000", false, null),
                balance(AssetKind.BANK, "달러", "USD", "100.25", false, null),
                balance(AssetKind.BANK, "옛 통장", "KRW", "7", true, null)));
    assertThat(page.groups())
        .extracting(g -> g.kind())
        .containsExactly(AssetKind.CASH, AssetKind.BANK, AssetKind.CREDIT_CARD);
    assertThat(page.groups().get(1).subtotals())
        .extracting(s -> s.currency() + " " + s.amount().toPlainString())
        .containsExactly("KRW 1000000", "USD 100.25");
    assertThat(page.totals())
        .extracting(s -> s.currency() + " " + s.amount().toPlainString())
        .containsExactly("KRW 930000", "USD 100.25"); // 숨긴 자산(7) 제외, 카드 음수 포함
    assertThat(page.hidden()).extracting(a -> a.asset().name()).containsExactly("옛 통장");
  }

  @Test
  void assetsPageRenders() {
    var page =
        AssetGroups.of(
            List.of(
                balance(AssetKind.CASH, "현금", "KRW", "50000", false, null),
                balance(AssetKind.CREDIT_CARD, "카드", "KRW", "-120000", false, 14),
                balance(AssetKind.BANK, "옛 통장", "KRW", "7", true, null)));
    var html =
        BookkeepingScreenTest.render(
            "bookkeeping/assets.jte", Map.of("book", BookkeepingScreenTest.book(), "page", page));
    assertThat(html).contains("<title>우리집 자산 · Bluesky 가계부</title>");
    assertThat(html).contains("-70,000").contains("-120,000").contains("14일 결제");
    assertThat(html).contains(">신용카드<").contains(">현금<");
    assertThat(html).contains("숨긴 자산 1개").contains("옛 통장");
    assertThat(html).contains("/bookkeeping/" + BOOK + "?assetId=");
    assertThat(html).contains("aria-current=\"page\"");

    var empty =
        BookkeepingScreenTest.render(
            "bookkeeping/assets.jte",
            Map.of("book", BookkeepingScreenTest.book(), "page", AssetGroups.of(List.of())));
    assertThat(empty).contains("자산이 없습니다.").doesNotContain("숨긴 자산");
  }

  static AssetBalance balance(
      AssetKind kind,
      String name,
      String currency,
      String balance,
      boolean hidden,
      Integer paymentDay) {
    return new AssetBalance(
        new Asset(
            UUID.randomUUID(), BOOK, kind, name, currency, BigDecimal.ZERO, paymentDay, 0, hidden),
        new BigDecimal(balance));
  }
}
