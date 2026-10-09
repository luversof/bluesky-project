package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;

@Component
public class MonthlyDividendPayoutImportParser {

  public List<MonthlyDividendPayoutUpsertRequest> parse(String symbol, String bulkInput) {
    if (!StringUtils.hasText(symbol)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.symbol.required"));
    }
    if (!StringUtils.hasText(bulkInput)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.empty"));
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    List<MonthlyDividendPayoutUpsertRequest> requests = new ArrayList<>();
    ColumnMapping columnMapping = null;
    String[] lines = bulkInput.split("\\R");
    for (int index = 0; index < lines.length; index++) {
      String line = lines[index] != null ? lines[index].trim() : "";
      if (!StringUtils.hasText(line)) {
        continue;
      }

      String[] columns = splitColumns(lines[index]);
      if (columns.length == 0) {
        continue;
      }

      if (columnMapping == null) {
        columnMapping = resolveColumnMapping(columns);
        continue;
      }

      requests.add(parseRow(normalizedSymbol, columns, columnMapping, index + 1));
    }

    if (columnMapping == null) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.header.missing"));
    }
    if (requests.isEmpty()) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.nothing"));
    }

    return requests;
  }

  /**
   * 출처 사이트의 표 한 줄을 못 읽어 건너뛰었다.
   *
   * @param row 출처 표의 몇째 행인가(머리글 제외, 1 부터) &mdash; 사이트에서 같은 행을 찾을 수 있게
   * @param reason 사용자에게 보일 사유(행 번호와 문제 값을 담는다)
   */
  public record SkippedRow(int row, String reason) {}

  /** 가져올 행과 건너뛴 행. */
  public record LenientParseResult(
      List<MonthlyDividendPayoutUpsertRequest> requests, List<SkippedRow> skipped) {}

  /**
   * 출처 사이트 표를 가져올 때만 쓴다 &mdash; 잘못된 행은 건너뛰고 나머지를 가져오며, 건너뛴 행은 사유와 함께 돌려준다.
   *
   * <p>사람이 붙여넣는 가져오기({@link #parse})는 그대로 엄격하다. 붙여넣은 사람은 그 자리에서 고칠 수 있지만, 출처 사이트의 오타는 우리가 고칠 수 없고
   * 사이트가 정정할 때까지 가져올 때마다 난다 &mdash; 실측 2026-09-17: RISE 44J2(코리아밸류업위클리고정커버드콜) 표의 지급기준일 2026-05-15 행
   * 실지급일이 2025-05-19(연도 오타)라, 그 한 줄 때문에 가져오기가 <b>통째로</b> 실패했다. 사용자 결정(2026-09-17): 잘못된 행만 건너뛰고 결과에
   * 밝힌다. 건너뛴 행은 저장된 이력을 건드리지 않는다(저장은 행마다 덮어쓰기라 지우는 일이 없다).
   *
   * <p>저장 단계가 막는 값(음수, 분배금을 넘는 과세표준)도 여기서 건너뛴다 &mdash; 안 그러면 앞쪽 행만 저장된 채 가져오기가 중간에 멈춘다.
   *
   * <p>모든 행이 잘못됐으면 가져올 것이 없으므로 예전처럼 실패한다(사유를 담아).
   */
  public LenientParseResult parseLenient(String symbol, String bulkInput) {
    if (!StringUtils.hasText(symbol)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.symbol.required"));
    }
    if (!StringUtils.hasText(bulkInput)) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.empty"));
    }

    String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);
    List<MonthlyDividendPayoutUpsertRequest> requests = new ArrayList<>();
    List<SkippedRow> skipped = new ArrayList<>();
    ColumnMapping columnMapping = null;
    int dataRow = 0;
    for (String rawLine : bulkInput.split("\\R")) {
      if (!StringUtils.hasText(rawLine)) {
        continue;
      }
      String[] columns = splitColumns(rawLine);
      if (columns.length == 0) {
        continue;
      }
      if (columnMapping == null) {
        columnMapping = resolveColumnMapping(columns);
        continue;
      }

      dataRow++;
      try {
        MonthlyDividendPayoutUpsertRequest request =
            parseRow(normalizedSymbol, columns, columnMapping, dataRow);
        requireSaveable(request, dataRow);
        requests.add(request);
      } catch (IllegalArgumentException ex) {
        skipped.add(new SkippedRow(dataRow, ex.getMessage()));
      } catch (RuntimeException ex) {
        // 칸이 모자란 줄 등 - 읽지 못한 줄도 건너뛰되 그렇다고 밝힌다.
        skipped.add(
            new SkippedRow(
                dataRow, msg("stock.monthly.reference.import.skipped.row.unreadable", dataRow)));
      }
    }

    if (columnMapping == null) {
      throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.header.missing"));
    }
    if (requests.isEmpty()) {
      if (skipped.isEmpty()) {
        throw new IllegalArgumentException(msg("stock.monthly.reference.error.bulk.nothing"));
      }
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.import.skipped.nothing", joinReasons(skipped)));
    }

    return new LenientParseResult(List.copyOf(requests), List.copyOf(skipped));
  }

  /** 건너뛴 행들의 사유를 한 줄로. */
  public static String joinReasons(List<SkippedRow> skipped) {
    return String.join(" / ", skipped.stream().map(SkippedRow::reason).toList());
  }

  /** 줄 하나를 읽는다. {@code lineNumber} 는 사용자에게 보일 사유에 그대로 들어간다. */
  private MonthlyDividendPayoutUpsertRequest parseRow(
      String normalizedSymbol, String[] columns, ColumnMapping columnMapping, int lineNumber) {
    MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
    request.setSymbol(normalizedSymbol);
    request.setRecordDate(
        parseLocalDate(
            columns[columnMapping.recordDateIndex()],
            lineNumber,
            msg("stock.monthly.reference.field.record.date")));
    request.setPayDate(
        parseLocalDate(
            columns[columnMapping.payDateIndex()],
            lineNumber,
            msg("stock.monthly.reference.field.pay.date")));
    request.setDistributionRatePct(
        columnMapping.distributionRateIndex() >= 0
            ? parseOptionalBigDecimal(
                columns[columnMapping.distributionRateIndex()],
                lineNumber,
                msg("stock.monthly.reference.field.distribution.rate"))
            : null);
    request.setDividendAmountPerShare(
        parseRequiredBigDecimal(
            columns[columnMapping.dividendAmountIndex()],
            lineNumber,
            msg("stock.monthly.reference.field.dividend.per.share")));
    // 과세표준이 "-" · 빈 칸이면 모름(null)으로 둔다(2026-10-07) - 0 으로 두면 진짜 0 과 구분되지 않아 다음 갱신이 채우지 못한다.
    request.setTaxableBasePerShare(
        parseOptionalBigDecimal(
            columns[columnMapping.taxableBaseIndex()],
            lineNumber,
            msg("stock.monthly.reference.field.taxable.base")));

    if (request.getPayDate().isBefore(request.getRecordDate())) {
      throw new IllegalArgumentException(
          msg(
              "stock.monthly.reference.error.bulk.pay.date.before.record",
              lineNumber,
              request.getPayDate(),
              request.getRecordDate()));
    }
    return request;
  }

  /** 저장 단계(게이트 검증 · api-stock)가 거절할 값. 관대한 가져오기에서 미리 걸러 가져오기가 중간에 멈추지 않게 한다. */
  private void requireSaveable(MonthlyDividendPayoutUpsertRequest request, int lineNumber) {
    String problem = null;
    if (request.getDistributionRatePct() != null && request.getDistributionRatePct().signum() < 0) {
      problem = msg("stock.monthly.reference.error.distribution.rate.negative");
    } else if (request.getDividendAmountPerShare() != null
        && request.getDividendAmountPerShare().signum() < 0) {
      problem = msg("stock.monthly.reference.error.dividend.per.share.negative");
    } else if (request.getTaxableBasePerShare() != null
        && request.getTaxableBasePerShare().signum() < 0) {
      problem = msg("stock.monthly.reference.error.taxable.base.negative");
    } else if (request.getTaxableBasePerShare() != null
        && request.getDividendAmountPerShare() != null
        && request.getTaxableBasePerShare().compareTo(request.getDividendAmountPerShare()) > 0) {
      problem = msg("stock.monthly.reference.error.taxable.base.exceeds.dividend");
    }
    if (problem != null) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.import.skipped.row.reason", lineNumber, problem));
    }
  }

  private String[] splitColumns(String line) {
    if (!StringUtils.hasText(line)) {
      return new String[0];
    }

    String[] rawColumns;
    if (line.contains("\t")) {
      rawColumns = line.split("\t", -1);
    } else if (line.contains(",")) {
      rawColumns = line.split(",", -1);
    } else {
      rawColumns = line.trim().split("\\s{2,}", -1);
    }

    List<String> columns = new ArrayList<>();
    for (String rawColumn : rawColumns) {
      columns.add(rawColumn != null ? rawColumn.trim() : "");
    }
    return columns.toArray(String[]::new);
  }

  private ColumnMapping resolveColumnMapping(String[] headers) {
    int recordDateIndex = -1;
    int payDateIndex = -1;
    int distributionRateIndex = -1;
    int dividendAmountIndex = -1;
    int taxableBaseIndex = -1;

    for (int index = 0; index < headers.length; index++) {
      String normalizedHeader = normalizeHeader(headers[index]);
      if (isRecordDateHeader(normalizedHeader)) {
        recordDateIndex = index;
      } else if (isPayDateHeader(normalizedHeader)) {
        payDateIndex = index;
      } else if (isDistributionRateHeader(normalizedHeader)) {
        distributionRateIndex = index;
      } else if (isDividendAmountHeader(normalizedHeader)) {
        dividendAmountIndex = index;
      } else if (isTaxableBaseHeader(normalizedHeader)) {
        taxableBaseIndex = index;
      }
    }

    if (recordDateIndex < 0
        || payDateIndex < 0
        || dividendAmountIndex < 0
        || taxableBaseIndex < 0) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.header.unrecognized"));
    }

    return new ColumnMapping(
        recordDateIndex,
        payDateIndex,
        distributionRateIndex,
        dividendAmountIndex,
        taxableBaseIndex);
  }

  private String normalizeHeader(String value) {
    return safe(value)
        .toLowerCase(Locale.ROOT)
        .replace(" ", "")
        .replace("\u00A0", "")
        .replace("(", "")
        .replace(")", "")
        .replace("_", "")
        .replace("-", "")
        .replace("%", "");
  }

  // 아래 한글은 화면 문구가 아니라 붙여넣은 표의 헤더를 알아보는 데이터다. 메시지 키로 옮기면
  // 영어 로케일에서 한글 헤더를 못 알아보게 된다(실측 2026-09-08: 옮겼다가 파서 검사 4개가 깨졌다).
  private boolean isRecordDateHeader(String normalizedHeader) {
    return normalizedHeader.contains("지급기준일")
        || normalizedHeader.equals("기준일")
        || normalizedHeader.contains("recorddate");
  }

  private boolean isPayDateHeader(String normalizedHeader) {
    return normalizedHeader.contains("실지급일")
        || normalizedHeader.contains("실제지급일")
        || normalizedHeader.contains("paydate");
  }

  private boolean isDistributionRateHeader(String normalizedHeader) {
    return normalizedHeader.contains("분배율") || normalizedHeader.contains("distributionrate");
  }

  private boolean isDividendAmountHeader(String normalizedHeader) {
    return normalizedHeader.contains("분배금") || normalizedHeader.contains("dividendamount");
  }

  private boolean isTaxableBaseHeader(String normalizedHeader) {
    return normalizedHeader.contains("과세표준액") || normalizedHeader.contains("taxablebase");
  }

  private LocalDate parseLocalDate(String value, int lineNumber, String label) {
    try {
      String normalized = safe(value).trim().replace('/', '-').replace('.', '-');
      String[] parts = normalized.split("-");
      if (parts.length != 3) {
        throw new IllegalArgumentException();
      }

      int year = Integer.parseInt(parts[0]);
      if (parts[0].length() == 2) {
        year += 2000;
      }
      int month = Integer.parseInt(parts[1]);
      int day = Integer.parseInt(parts[2]);
      return LocalDate.of(year, month, day);
    } catch (RuntimeException ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.format.invalid", lineNumber, label));
    }
  }

  private BigDecimal parseRequiredBigDecimal(String value, int lineNumber, String label) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.value.empty", lineNumber, label));
    }
    return parseBigDecimal(value, lineNumber, label);
  }

  private BigDecimal parseOptionalBigDecimal(String value, int lineNumber, String label) {
    if (!StringUtils.hasText(value) || "-".equals(value.trim())) {
      return null;
    }
    return parseBigDecimal(value, lineNumber, label);
  }

  private BigDecimal parseBigDecimal(String value, int lineNumber, String label) {
    try {
      return new BigDecimal(
          safe(value)
              .trim()
              .replace(",", "")
              .replace("%", "")
              .replace("원", "")
              .replace("\u00A0", ""));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.bulk.value.invalid", lineNumber, label));
    }
  }

  private String safe(String value) {
    return value != null ? value : "";
  }

  private record ColumnMapping(
      int recordDateIndex,
      int payDateIndex,
      int distributionRateIndex,
      int dividendAmountIndex,
      int taxableBaseIndex) {}
}
