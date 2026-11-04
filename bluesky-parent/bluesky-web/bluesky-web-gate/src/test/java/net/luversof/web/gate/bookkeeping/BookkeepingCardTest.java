package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.AssetKind;
import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.EntryForm;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.CardBill;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;
import net.luversof.web.gate.bookkeeping.support.AssetGroups;
import net.luversof.web.gate.bookkeeping.support.CardBillView;

/** 카드 할부: 모달 선택(지출만) · 목록 표시 · 자산 화면 결제 예정액. */
class BookkeepingCardTest {

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void expenseModalHasInstallmentSelectKeepingValue() {
    var base =
        BookkeepingScreenTest.entry(
            EntryType.EXPENSE,
            LocalDate.of(2026, 11, 3),
            "300000",
            null,
            BookkeepingScreenTest.CASH.id(),
            null,
            BookkeepingScreenTest.COFFEE.id(),
            "TV");
    var tv = withInstallment(base, 3);
    var html =
        BookkeepingRecurringTest.render(BookkeepingRecurringTest.with(EntryForm.of(tv), List.of()));
    assertThat(html).contains("name=\"installmentMonths\"");
    assertThat(html).contains("<option value=\"3\" selected>3개월 할부</option>");
    // 목록 줄에도 할부 표시
    var listed =
        BookkeepingScreenTest.render(
            "bookkeeping/month.jte",
            Map.of("page", BookkeepingScreenTest.page(List.of(tv), List.of(), EntryForm.closed())));
    assertThat(listed).contains("TV · 현금 · 3개월 할부");

    var income =
        new EntryForm(
            true, null, EntryType.INCOME, "2026-11-03", "", "", "", null, null, null, "", "");
    assertThat(BookkeepingRecurringTest.render(BookkeepingRecurringTest.with(income, List.of())))
        .doesNotContain("name=\"installmentMonths\"");
  }

  @Test
  void assetsShowCardBillsOrNote() {
    var card =
        BookkeepingAssetScreenTest.balance(
            AssetKind.CREDIT_CARD, "카드", "KRW", "-105000", false, 15);
    var page = AssetGroups.of(List.of(card));
    var id = card.asset().id();
    var bills =
        CardBillView.of(
            List.of(
                new CardBill(
                    id,
                    "2026-11",
                    new BigDecimal("5000"),
                    new BigDecimal("33334"),
                    new BigDecimal("38334"))),
            List.of(
                new CardBill(id, "2026-12", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO)));
    var html =
        BookkeepingScreenTest.render(
            "bookkeeping/assets.jte",
            Map.of("book", BookkeepingScreenTest.book(), "page", page, "cardBills", bills));
    assertThat(html).contains("이번 달 결제 38,334 · 다음 달 0");
    assertThat(
            BookkeepingScreenTest.render(
                "bookkeeping/assets.jte",
                Map.of(
                    "book",
                    BookkeepingScreenTest.book(),
                    "page",
                    page,
                    "cardBills",
                    CardBillView.unavailable())))
        .contains("카드 결제 예정액을 불러오지 못했습니다.")
        .doesNotContain("이번 달 결제");
  }

  static Entry withInstallment(Entry e, int months) {
    return new Entry(
        e.id(),
        e.bookId(),
        e.type(),
        e.entryDate(),
        e.entryTime(),
        e.amount(),
        e.toAmount(),
        e.assetId(),
        e.toAssetId(),
        e.categoryId(),
        e.content(),
        e.memo(),
        months,
        e.createdBy(),
        e.createdDate());
  }
}
