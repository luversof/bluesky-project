package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingViewController.EntryForm;

/** 0원 내역(복지카드 등): 목록에 "-0" 처럼 부호를 붙이지 않는다. */
class BookkeepingZeroAmountTest {

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void zeroEntryHasNoSign() {
    var zero =
        BookkeepingScreenTest.entry(
            EntryType.EXPENSE,
            LocalDate.of(2026, 11, 4),
            "0",
            null,
            BookkeepingScreenTest.CASH.id(),
            null,
            BookkeepingScreenTest.COFFEE.id(),
            "복지카드 점심");
    var html =
        BookkeepingScreenTest.render(
            "bookkeeping/month.jte",
            Map.of(
                "page", BookkeepingScreenTest.page(List.of(zero), List.of(), EntryForm.closed())));
    assertThat(html).contains("복지카드 점심").doesNotContain("-0\n").doesNotContain(">-0<");
    assertThat(html).containsPattern("amount-value\">\\s*0\\s*</span>");
  }
}
