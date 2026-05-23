package net.luversof.web.gate.stock.util;

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
      throw new IllegalArgumentException("종목코드는 필수입니다.");
    }
    if (!StringUtils.hasText(bulkInput)) {
      throw new IllegalArgumentException("붙여넣기 데이터가 비어 있습니다.");
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

      MonthlyDividendPayoutUpsertRequest request = new MonthlyDividendPayoutUpsertRequest();
      request.setSymbol(normalizedSymbol);
      request.setRecordDate(
          parseLocalDate(columns[columnMapping.recordDateIndex()], index + 1, "지급기준일"));
      request.setPayDate(parseLocalDate(columns[columnMapping.payDateIndex()], index + 1, "실지급일"));
      request.setDistributionRatePct(
          columnMapping.distributionRateIndex() >= 0
              ? parseOptionalBigDecimal(
                  columns[columnMapping.distributionRateIndex()], index + 1, "분배율")
              : null);
      request.setDividendAmountPerShare(
          parseRequiredBigDecimal(
              columns[columnMapping.dividendAmountIndex()], index + 1, "주당 분배금"));
      request.setTaxableBasePerShare(
          parseZeroAllowedBigDecimal(
              columns[columnMapping.taxableBaseIndex()], index + 1, "주당 과세표준액"));

      if (request.getPayDate().isBefore(request.getRecordDate())) {
        throw new IllegalArgumentException(
            (index + 1)
                + "번째 줄의 실지급일("
                + request.getPayDate()
                + ")은 지급기준일("
                + request.getRecordDate()
                + ")보다 빠를 수 없습니다.");
      }

      requests.add(request);
    }

    if (columnMapping == null) {
      throw new IllegalArgumentException("헤더를 찾지 못했습니다.");
    }
    if (requests.isEmpty()) {
      throw new IllegalArgumentException("등록할 데이터가 없습니다.");
    }

    return requests;
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
      throw new IllegalArgumentException("붙여넣기 헤더 형식을 인식하지 못했습니다.");
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
      throw new IllegalArgumentException(lineNumber + "번째 줄의 " + label + " 형식이 올바르지 않습니다.");
    }
  }

  private BigDecimal parseRequiredBigDecimal(String value, int lineNumber, String label) {
    if (!StringUtils.hasText(value)) {
      throw new IllegalArgumentException(lineNumber + "번째 줄의 " + label + " 값이 비어 있습니다.");
    }
    return parseBigDecimal(value, lineNumber, label);
  }

  private BigDecimal parseOptionalBigDecimal(String value, int lineNumber, String label) {
    if (!StringUtils.hasText(value) || "-".equals(value.trim())) {
      return null;
    }
    return parseBigDecimal(value, lineNumber, label);
  }

  private BigDecimal parseZeroAllowedBigDecimal(String value, int lineNumber, String label) {
    if (!StringUtils.hasText(value) || "-".equals(value.trim())) {
      return BigDecimal.ZERO;
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
      throw new IllegalArgumentException(lineNumber + "번째 줄의 " + label + " 값이 올바르지 않습니다.");
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
