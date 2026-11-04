package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.controller.BookkeepingImportController.ImportPage;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.support.BookkeepingView;
import net.luversof.web.gate.bookkeeping.support.CsvExport;
import net.luversof.web.gate.bookkeeping.support.CsvImport;

/** CSV 가져오기: 내보낸 파일을 그대로 다시 읽는다(왕복) · 줄 오류 · 파일 오류 · 미리보기 렌더. */
class BookkeepingCsvImportTest {

  static final List<AssetBalance> ASSETS =
      List.of(
          BookkeepingScreenTest.balance(BookkeepingScreenTest.CASH),
          BookkeepingScreenTest.balance(BookkeepingScreenTest.USD));
  static final List<net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category> CATS =
      List.of(
          BookkeepingScreenTest.FOOD, BookkeepingScreenTest.COFFEE, BookkeepingScreenTest.SALARY);

  @BeforeAll
  static void messages() {
    BookkeepingScreenTest.messages();
  }

  @AfterAll
  static void reset() {
    BookkeepingScreenTest.reset();
  }

  @Test
  void exportedFileReadsBackToTheSameCommands() {
    var cash = BookkeepingScreenTest.CASH.id();
    var usd = BookkeepingScreenTest.USD.id();
    var entries =
        List.of(
            BookkeepingCsvExportTest.entry(
                EntryType.EXPENSE,
                "2026-11-01",
                "12:30",
                "12000",
                null,
                cash,
                null,
                BookkeepingScreenTest.COFFEE.id(),
                "점심, \"김밥\"",
                "줄1\n줄2",
                3),
            BookkeepingCsvExportTest.entry(
                EntryType.INCOME,
                "2026-11-01",
                null,
                "3000000",
                null,
                cash,
                null,
                BookkeepingScreenTest.SALARY.id(),
                "=HYPERLINK(1)",
                null),
            BookkeepingCsvExportTest.entry(
                EntryType.TRANSFER,
                "2026-11-02",
                null,
                "130000",
                "100.25",
                cash,
                usd,
                null,
                null,
                null));
    var csv =
        CsvExport.csv(
            entries, BookkeepingView.assetsById(ASSETS), BookkeepingView.categoriesById(CATS));
    var result = CsvImport.read(csv, ASSETS, CATS);
    assertThat(result.errors()).isEmpty();
    assertThat(result.importable()).isTrue();
    var commands = result.rows().stream().map(CsvImport.Row::command).toList();
    // 내보내기는 날짜순(같은 날 시간 없는 것 먼저)
    assertThat(commands.get(0).type()).isEqualTo(EntryType.INCOME);
    assertThat(commands.get(0).content()).isEqualTo("=HYPERLINK(1)"); // ' 를 걷었다
    assertThat(commands.get(0).categoryId()).isEqualTo(BookkeepingScreenTest.SALARY.id());
    var lunch = commands.get(1);
    assertThat(lunch.categoryId()).isEqualTo(BookkeepingScreenTest.COFFEE.id());
    assertThat(lunch.content()).isEqualTo("점심, \"김밥\"");
    assertThat(lunch.memo()).isEqualTo("줄1\n줄2");
    assertThat(lunch.installmentMonths()).isEqualTo(3);
    assertThat(lunch.entryTime()).hasToString("12:30");
    var fx = commands.get(2);
    assertThat(fx.toAssetId()).isEqualTo(usd);
    assertThat(fx.toAmount()).isEqualByComparingTo("100.25");
    assertThat(fx.categoryId()).isNull();
    assertThat(result.rows().get(1).line()).isEqualTo(3);
  }

  @Test
  void rowErrorsNameTheLineAndBlockImport() {
    var csv =
        "날짜,시간,종류,대분류,소분류,자산,받는 자산,금액,받는 금액,통화,내용,메모\r\n"
            + "2026-11-01,,지출,식비,커피,현금,,1000,,KRW,,\r\n"
            + "2026-11-02,,지출,없는분류,,현금,,1000,,KRW,,\r\n"
            + "2026-13-01,,지출,식비,,현금,,1000,,KRW,,\r\n"
            + "2026-11-03,,지출,식비,,없는자산,,1000,,KRW,,\r\n"
            + "2026-11-03,,지출,식비,,현금,,abc,,KRW,,\r\n"
            + "2026-11-03,,지출,식비,,현금,,1000,,USD,,\r\n"
            + "2026-11-03,,뭐지,식비,,현금,,1000,,KRW,,\r\n"
            + "2026-11-03,,지출\r\n";
    var result = CsvImport.read(csv, ASSETS, CATS);
    assertThat(result.rows()).hasSize(1);
    assertThat(result.importable()).isFalse();
    assertThat(result.errors())
        .extracting(e -> e.line() + ":" + e.message())
        .containsExactly(
            "3:분류를 찾지 못했습니다: 없는분류",
            "4:날짜가 올바르지 않습니다: 2026-13-01",
            "5:자산을 찾지 못했습니다: 없는자산",
            "6:금액이 올바르지 않습니다: abc",
            "7:통화(USD)가 자산의 통화(KRW)와 다릅니다.",
            "8:종류를 알 수 없습니다: 뭐지",
            "9:열이 12개여야 합니다(3개).");
  }

  @Test
  void fileErrors() {
    assertThat(CsvImport.read("", ASSETS, CATS).fileError()).contains("비어");
    assertThat(CsvImport.read("a,b\r\n", ASSETS, CATS).fileError()).contains("머리글");
    assertThat(CsvImport.read("a,\"b\r\n", ASSETS, CATS).fileError()).contains("따옴표");
    var many = new StringBuilder("날짜,시간,종류,대분류,소분류,자산,받는 자산,금액,받는 금액,통화,내용,메모\r\n");
    for (int i = 0; i <= CsvImport.MAX_ROWS; i++) {
      many.append("2026-11-01,,지출,식비,,현금,,1,,KRW,,\r\n");
    }
    assertThat(CsvImport.read(many.toString(), ASSETS, CATS).fileError()).contains("2,000");
  }

  @Test
  void previewRendersRowsErrorsAndConfirmOnlyWhenClean() {
    var good =
        "날짜,시간,종류,대분류,소분류,자산,받는 자산,금액,받는 금액,통화,내용,메모\r\n"
            + "2026-11-01,,지출,식비,커피,현금,,1000,,KRW,<b>,\r\n";
    var html =
        render(
            new ImportPage(
                BookkeepingScreenTest.book(), CsvImport.read(good, ASSETS, CATS), good, null));
    assertThat(html).contains("enctype=\"multipart/form-data\"");
    assertThat(html).contains("2줄").contains("&lt;b&gt;");
    assertThat(html)
        .contains("action=\"/bookkeeping/" + BookkeepingScreenTest.BOOK + "/import/confirm\"");
    assertThat(html).contains("1건 넣기");

    var bad = good + "2026-11-02,,지출,없는분류,,현금,,1000,,KRW,,\r\n";
    var badHtml =
        render(
            new ImportPage(
                BookkeepingScreenTest.book(), CsvImport.read(bad, ASSETS, CATS), bad, null));
    assertThat(badHtml).contains("분류를 찾지 못했습니다: 없는분류").doesNotContain("/import/confirm");

    assertThat(render(new ImportPage(BookkeepingScreenTest.book(), null, null, 5)))
        .contains("5건을 넣었습니다.")
        .doesNotContain("bk-import-preview");
  }

  static String render(ImportPage page) {
    return BookkeepingScreenTest.render("bookkeeping/import.jte", Map.of("page", page));
  }
}
