package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.Entry;

/**
 * 내역 CSV(UTF-8 BOM, 엑셀 호환). 열: 날짜 · 시간 · 종류 · 대분류 · 소분류 · 자산 · 받는 자산 · 금액 · 받는 금액 · 통화 · 내용 · 메모 ·
 * 할부(개월, 비면 일시불).
 *
 * <p>통화는 금액(나가는 자산)의 통화. 받는 금액의 통화는 받는 자산의 통화다. 글자 칸이 = + - @ 등으로 시작하면 앞에 ' 를 붙여 엑셀이 수식으로 읽지 않게
 * 한다(CSV 수식 주입). 가져오기(2차)는 이 형식을 읽는다.
 */
public final class CsvExport {

  /** 머리글 메시지 키(열 순서). */
  public static final List<String> HEADER_KEYS =
      List.of(
          "bookkeeping.csv.date",
          "bookkeeping.csv.time",
          "bookkeeping.csv.type",
          "bookkeeping.csv.category",
          "bookkeeping.csv.subcategory",
          "bookkeeping.csv.asset",
          "bookkeeping.csv.toAsset",
          "bookkeeping.csv.amount",
          "bookkeeping.csv.toAmount",
          "bookkeeping.csv.currency",
          "bookkeeping.csv.content",
          "bookkeeping.csv.memo",
          "bookkeeping.csv.installment");

  private CsvExport() {}

  /** BOM + 머리글 + 내역(날짜 오래된 것부터). */
  public static String csv(
      List<Entry> entries, Map<UUID, AssetBalance> assets, Map<UUID, Category> categories) {
    var b = new StringBuilder("\uFEFF");
    line(b, HEADER_KEYS.stream().map(BookkeepingView::msg).toList());
    var sorted = new ArrayList<>(entries);
    sorted.sort(
        (x, y) -> {
          int c = x.entryDate().compareTo(y.entryDate());
          if (c != 0) {
            return c;
          }
          if (x.entryTime() == null || y.entryTime() == null) {
            return x.entryTime() == null ? (y.entryTime() == null ? 0 : -1) : 1;
          }
          return x.entryTime().compareTo(y.entryTime());
        });
    for (var e : sorted) {
      var category = e.categoryId() == null ? null : categories.get(e.categoryId());
      var parent =
          category == null || category.parentId() == null
              ? null
              : categories.get(category.parentId());
      line(
          b,
          List.of(
              e.entryDate().toString(),
              e.entryTime() == null ? "" : e.entryTime().toString(),
              BookkeepingView.msg("bookkeeping.type." + e.type().name()),
              text(parent != null ? parent.name() : category != null ? category.name() : ""),
              text(parent != null ? category.name() : ""),
              text(BookkeepingView.assetName(assets, e.assetId())),
              text(BookkeepingView.assetName(assets, e.toAssetId())),
              number(e.amount()),
              number(e.toAmount()),
              BookkeepingView.assetCurrency(assets, e.assetId()),
              text(e.content()),
              text(e.memo()),
              e.installmentMonths() == null ? "" : e.installmentMonths().toString()));
    }
    return b.toString();
  }

  /** from ~ to 를 달력 월 단위로 나눈다(한 번 조회 상한을 넘지 않게). */
  public static List<LocalDate[]> monthChunks(LocalDate from, LocalDate to) {
    var chunks = new ArrayList<LocalDate[]>();
    var start = from;
    while (!start.isAfter(to)) {
      var end = YearMonth.from(start).atEndOfMonth();
      if (end.isAfter(to)) {
        end = to;
      }
      chunks.add(new LocalDate[] {start, end});
      start = end.plusDays(1);
    }
    return chunks;
  }

  static String number(BigDecimal value) {
    return value == null ? "" : value.stripTrailingZeros().toPlainString();
  }

  /** 수식으로 읽힐 글자 앞에 ' 를 붙인다. */
  static String text(String value) {
    if (value == null || value.isEmpty()) {
      return "";
    }
    char first = value.charAt(0);
    return first == '='
            || first == '+'
            || first == '-'
            || first == '@'
            || first == '\t'
            || first == '\r'
        ? "'" + value
        : value;
  }

  private static void line(StringBuilder b, List<String> fields) {
    for (int i = 0; i < fields.size(); i++) {
      if (i > 0) {
        b.append(',');
      }
      b.append(quote(fields.get(i)));
    }
    b.append("\r\n");
  }

  static String quote(String value) {
    if (value.indexOf(',') < 0
        && value.indexOf('"') < 0
        && value.indexOf('\n') < 0
        && value.indexOf('\r') < 0) {
      return value;
    }
    return '"' + value.replace("\"", "\"\"") + '"';
  }
}
