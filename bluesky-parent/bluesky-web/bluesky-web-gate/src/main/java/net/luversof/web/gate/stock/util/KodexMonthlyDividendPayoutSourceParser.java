package net.luversof.web.gate.stock.util;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class KodexMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  private final ObjectMapper objectMapper;

  public KodexMonthlyDividendPayoutSourceParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public KodexDividendResponse parseResponse(String json) {
    try {
      return objectMapper.readValue(json, KodexDividendResponse.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("KODEX ETF 출처 응답을 해석하지 못했습니다.", ex);
    }
  }

  public String toBulkInput(List<KodexDividendRow> rows) {
    if (rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException("KODEX ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
    }

    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (KodexDividendRow row : rows) {
      if (!StringUtils.hasText(row.basicD())
          || !StringUtils.hasText(row.payD())
          || !StringUtils.hasText(row.dividA())
          || !StringUtils.hasText(row.taxDividA())) {
        continue;
      }

      bulkInput
          .append('\n')
          .append(formatCompactDate(row.basicD()))
          .append('\t')
          .append(formatCompactDate(row.payD()))
          .append('\t')
          .append(row.dividA().trim())
          .append('\t')
          .append(row.taxDividA().trim());
    }

    if (bulkInput.toString().equals(BULK_INPUT_HEADER)) {
      throw new IllegalArgumentException("KODEX ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
    }

    return bulkInput.toString();
  }

  private String formatCompactDate(String value) {
    String normalized = value != null ? value.replaceAll("[^0-9]", "") : "";
    if (normalized.length() != 8) {
      return value != null ? value.trim() : "";
    }

    return normalized.substring(0, 4)
        + "-"
        + normalized.substring(4, 6)
        + "-"
        + normalized.substring(6, 8);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KodexDividendResponse(List<KodexDividendRow> dividList) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KodexDividendRow(String basicD, String payD, String dividA, String taxDividA) {}
}
