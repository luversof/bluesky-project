package net.luversof.web.gate.bookkeeping.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import net.luversof.web.gate.bookkeeping.constant.CategoryKind;
import net.luversof.web.gate.bookkeeping.constant.EntryType;
import net.luversof.web.gate.bookkeeping.httpexchange.AssetClient.AssetBalance;
import net.luversof.web.gate.bookkeeping.httpexchange.CategoryClient.Category;
import net.luversof.web.gate.bookkeeping.httpexchange.EntryClient.EntryCommand;

/**
 * CSV 가져오기: 내보내기({@link CsvExport})와 같은 형식만 읽는다. 첫 줄은 머리글, 12 열(할부 없음) 또는 13 열.
 *
 * <p>자산 · 분류는 이름으로 찾고(숨긴 것 포함), 못 찾으면 그 줄을 오류로 남긴다 - 새로 만들지 않는다. 오류가 한 줄이라도 있으면 화면이 넣기를 막는다. 내보내기가
 * 수식 주입을 막으려 붙인 ' 는 걷어 낸다.
 */
public final class CsvImport {

  /** 한 번에 넣는 최대 건수(API 와 같다). */
  public static final int MAX_ROWS = 2000;

  private static final int COLUMNS = CsvExport.HEADER_KEYS.size();

  /** line 은 파일의 줄 번호(머리글 = 1). command 는 오류가 없을 때만. */
  public record Row(int line, EntryCommand command, String summary) {}

  public record RowError(int line, String message) {}

  /** fileError 가 있으면 줄 단위 결과 없이 파일 자체가 틀렸다. */
  public record Result(List<Row> rows, List<RowError> errors, String fileError) {

    public boolean importable() {
      return fileError == null && errors.isEmpty() && !rows.isEmpty();
    }
  }

  private CsvImport() {}

  public static Result read(String csv, List<AssetBalance> assets, List<Category> categories) {
    List<List<String>> records;
    try {
      records = parse(csv == null ? "" : csv);
    } catch (IllegalArgumentException e) {
      return fileError("bookkeeping.import.error.quote");
    }
    if (records.isEmpty()) {
      return fileError("bookkeeping.import.error.empty");
    }
    var header = records.get(0);
    if (header.size() != COLUMNS && header.size() != COLUMNS - 1) {
      return fileError("bookkeeping.import.error.header");
    }
    if (records.size() - 1 > MAX_ROWS) {
      return fileError("bookkeeping.import.error.tooMany");
    }
    var rows = new ArrayList<Row>();
    var errors = new ArrayList<RowError>();
    for (int i = 1; i < records.size(); i++) {
      var r = records.get(i);
      int line = i + 1;
      if (r.size() == 1 && r.get(0).isBlank()) {
        continue; // 빈 줄
      }
      try {
        rows.add(row(line, r, header.size(), assets, categories));
      } catch (RowException e) {
        errors.add(new RowError(line, e.getMessage()));
      }
    }
    if (rows.isEmpty() && errors.isEmpty()) {
      return fileError("bookkeeping.import.error.empty");
    }
    return new Result(rows, errors, null);
  }

  private static Row row(
      int line, List<String> r, int width, List<AssetBalance> assets, List<Category> categories) {
    if (r.size() != width) {
      throw new RowException("bookkeeping.import.error.columns", width, r.size());
    }
    var type = type(r.get(2));
    LocalDate date;
    LocalTime time;
    try {
      date = LocalDate.parse(r.get(0).strip());
      time = r.get(1).isBlank() ? null : LocalTime.parse(r.get(1).strip());
    } catch (DateTimeParseException e) {
      throw new RowException("bookkeeping.import.error.date", (r.get(0) + " " + r.get(1)).strip());
    }
    var asset = asset(assets, unguard(r.get(5)), "bookkeeping.import.error.asset");
    var currency = r.get(9).strip();
    if (!currency.isEmpty() && !currency.equals(asset.asset().currency())) {
      throw new RowException(
          "bookkeeping.import.error.currency", currency, asset.asset().currency());
    }
    var amount = amount(r.get(7));
    if (amount == null) {
      throw new RowException("bookkeeping.import.error.amount", r.get(7));
    }
    UUID toAssetId = null;
    BigDecimal toAmount = null;
    UUID categoryId = null;
    if (type == EntryType.TRANSFER) {
      toAssetId = asset(assets, unguard(r.get(6)), "bookkeeping.import.error.toAsset").asset().id();
      toAmount = r.get(8).isBlank() ? null : amount(r.get(8));
    } else {
      categoryId = category(categories, type, unguard(r.get(3)), unguard(r.get(4))).id();
    }
    Integer installment = null;
    if (width == COLUMNS && !r.get(12).isBlank()) {
      try {
        installment = Integer.valueOf(r.get(12).strip());
      } catch (NumberFormatException e) {
        throw new RowException("bookkeeping.import.error.installment", r.get(12));
      }
    }
    var content = unguard(r.get(10));
    var command =
        new EntryCommand(
            type,
            date,
            time,
            amount,
            toAmount,
            asset.asset().id(),
            toAssetId,
            categoryId,
            content.isEmpty() ? null : content,
            emptyToNull(unguard(r.get(11))),
            installment);
    return new Row(
        line,
        command,
        date
            + " · "
            + BookkeepingView.msg("bookkeeping.type." + type.name())
            + " · "
            + BookkeepingView.amount(amount, asset.asset().currency())
            + " "
            + asset.asset().currency()
            + (content.isEmpty() ? "" : " · " + content));
  }

  private static EntryType type(String value) {
    var v = value.strip();
    for (var t : EntryType.values()) {
      if (t.name().equalsIgnoreCase(v)
          || BookkeepingView.msg("bookkeeping.type." + t.name()).equals(v)) {
        return t;
      }
    }
    throw new RowException("bookkeeping.import.error.type", value);
  }

  private static AssetBalance asset(List<AssetBalance> assets, String name, String errorKey) {
    return assets.stream()
        .filter(a -> a.asset().name().equals(name))
        .findFirst()
        .orElseThrow(() -> new RowException(errorKey, name));
  }

  /** 소분류가 비면 대분류 자체. 종류(수입 · 지출)가 맞는 분류만. */
  private static Category category(
      List<Category> categories, EntryType type, String parentName, String childName) {
    var kind = type == EntryType.INCOME ? CategoryKind.INCOME : CategoryKind.EXPENSE;
    var parent =
        categories.stream()
            .filter(c -> c.parentId() == null && c.kind() == kind && c.name().equals(parentName))
            .findFirst()
            .orElseThrow(() -> new RowException("bookkeeping.import.error.category", parentName));
    if (childName.isEmpty()) {
      return parent;
    }
    return categories.stream()
        .filter(c -> Objects.equals(c.parentId(), parent.id()) && c.name().equals(childName))
        .findFirst()
        .orElseThrow(
            () ->
                new RowException(
                    "bookkeeping.import.error.category", parentName + " > " + childName));
  }

  private static BigDecimal amount(String value) {
    try {
      return value.isBlank() ? null : new BigDecimal(value.replace(",", "").strip());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  /** 내보내기가 수식 앞에 붙인 ' 를 걷는다. */
  static String unguard(String value) {
    var v = value.strip();
    if (v.length() > 1 && v.charAt(0) == '\'') {
      char next = v.charAt(1);
      if (next == '=' || next == '+' || next == '-' || next == '@') {
        return v.substring(1);
      }
    }
    return v;
  }

  private static String emptyToNull(String value) {
    return value.isEmpty() ? null : value;
  }

  /**
   * RFC 4180: 쉼표 구분, 큰따옴표로 감싼 칸 안의 쉼표 · 줄바꿈 · "" 허용. BOM 은 버린다. 닫히지 않은 따옴표는
   * IllegalArgumentException.
   */
  static List<List<String>> parse(String csv) {
    var text = csv.startsWith("\uFEFF") ? csv.substring(1) : csv;
    var records = new ArrayList<List<String>>();
    var record = new ArrayList<String>();
    var field = new StringBuilder();
    boolean quoted = false;
    boolean any = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      any = true;
      if (quoted) {
        if (c == '"') {
          if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
            field.append('"');
            i++;
          } else {
            quoted = false;
          }
        } else {
          field.append(c);
        }
      } else if (c == '"' && field.isEmpty()) {
        quoted = true;
      } else if (c == ',') {
        record.add(field.toString());
        field.setLength(0);
      } else if (c == '\r' || c == '\n') {
        if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
          i++;
        }
        record.add(field.toString());
        field.setLength(0);
        records.add(record);
        record = new ArrayList<>();
        any = false;
      } else {
        field.append(c);
      }
    }
    if (quoted) {
      throw new IllegalArgumentException("unclosed quote");
    }
    if (any) {
      record.add(field.toString());
      records.add(record);
    }
    return records;
  }

  private static Result fileError(String key) {
    return new Result(List.of(), List.of(), BookkeepingView.msg(key, MAX_ROWS));
  }

  /** 줄 하나의 사유(메시지로 바꿔 둔다). */
  private static final class RowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    RowException(String key, Object... args) {
      super(BookkeepingView.msg(key, args), null, false, false);
    }
  }
}
