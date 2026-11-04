package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.CsvExport;

/** CSV 내보내기: BOM · 머리글 · 따옴표 · 수식 주입 막기 · 정렬 · 달 나누기. */
class BookkeepingCsvExportTest {

  static final UUID BOOK = BookkeepingScreenTest.BOOK;

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  static Entry entry(
      EntryType type,
      String date,
      String time,
      String amount,
      String toAmount,
      UUID asset,
      UUID to,
      UUID category,
      String content,
      String memo) {
    return entry(type, date, time, amount, toAmount, asset, to, category, content, memo, null);
  }

  static Entry entry(
      EntryType type,
      String date,
      String time,
      String amount,
      String toAmount,
      UUID asset,
      UUID to,
      UUID category,
      String content,
      String memo,
      Integer installment) {
    return new Entry(
        UUID.randomUUID(),
        BOOK,
        type,
        LocalDate.parse(date),
        time == null ? null : LocalTime.parse(time),
        new BigDecimal(amount),
        toAmount == null ? null : new BigDecimal(toAmount),
        asset,
        to,
        category,
        content,
        memo,
        installment,
        UUID.randomUUID(),
        Instant.now());
  }

  @Test
  void writesBomHeaderAndEscapedRowsOldestFirst() {
    var cash = BookkeepingScreenTest.CASH;
    var usd = BookkeepingScreenTest.USD;
    var assets =
        BookkeepingView.assetsById(
            List.of(
                new AssetBalance(cash, BigDecimal.ZERO), new AssetBalance(usd, BigDecimal.ZERO)));
    var categories =
        BookkeepingView.categoriesById(
            List.of(
                BookkeepingScreenTest.FOOD,
                BookkeepingScreenTest.COFFEE,
                BookkeepingScreenTest.SALARY));
    var csv =
        CsvExport.csv(
            List.of(
                entry(
                    EntryType.TRANSFER,
                    "2026-11-02",
                    null,
                    "130000",
                    "100.25",
                    cash.id(),
                    usd.id(),
                    null,
                    "환전",
                    null),
                entry(
                    EntryType.EXPENSE,
                    "2026-11-01",
                    "12:30",
                    "12000.00",
                    null,
                    cash.id(),
                    null,
                    BookkeepingScreenTest.COFFEE.id(),
                    "점심, \"김밥\"",
                    "줄1\n줄2",
                    3),
                entry(
                    EntryType.INCOME,
                    "2026-11-01",
                    null,
                    "3000000",
                    null,
                    cash.id(),
                    null,
                    BookkeepingScreenTest.SALARY.id(),
                    "=HYPERLINK(1)",
                    "-메모")),
            assets,
            categories);
    assertThat(csv).startsWith("\uFEFF날짜,시간,종류,대분류,소분류,자산,받는 자산,금액,받는 금액,통화,내용,메모,할부\r\n");
    var lines = csv.substring(1).split("\r\n", -1);
    // 날짜 오래된 것부터, 같은 날은 시간 없는 것 먼저
    assertThat(lines[1]).isEqualTo("2026-11-01,,수입,급여,,현금,,3000000,,KRW,'=HYPERLINK(1),'-메모,");
    assertThat(lines[2])
        .isEqualTo("2026-11-01,12:30,지출,식비,커피,현금,,12000,,KRW,\"점심, \"\"김밥\"\"\",\"줄1\n줄2\",3");
    assertThat(lines[3]).isEqualTo("2026-11-02,,이체,,,현금,달러 통장,130000,100.25,KRW,환전,,");
    assertThat(lines).hasSize(5).endsWith("");
  }

  @Test
  void splitsPeriodByCalendarMonth() {
    assertThat(CsvExport.monthChunks(LocalDate.parse("2026-10-25"), LocalDate.parse("2026-12-03")))
        .extracting(c -> c[0] + "~" + c[1])
        .containsExactly("2026-10-25~2026-10-31", "2026-11-01~2026-11-30", "2026-12-01~2026-12-03");
    assertThat(CsvExport.monthChunks(LocalDate.parse("2026-11-05"), LocalDate.parse("2026-11-05")))
        .hasSize(1);
  }
}
