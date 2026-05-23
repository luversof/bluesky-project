package net.luversof.web.gate.stock.util;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PlusMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  private final ObjectMapper objectMapper;

  public PlusMonthlyDividendPayoutSourceParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public PlusDividendPage parsePage(String json) {
    try {
      return objectMapper.readValue(json, PlusDividendPage.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException("PLUS ETF 출처 응답을 해석하지 못했습니다.", ex);
    }
  }

  public String toBulkInput(List<PlusDividendRow> rows) {
    if (rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException("PLUS ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
    }

    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (PlusDividendRow row : rows) {
      if (!StringUtils.hasText(row.wkdate())
          || !StringUtils.hasText(row.dkdate())
          || !StringUtils.hasText(row.dividend())
          || !StringUtils.hasText(row.taxBase())) {
        continue;
      }

      bulkInput
          .append('\n')
          .append(formatCompactDate(row.wkdate()))
          .append('\t')
          .append(formatCompactDate(row.dkdate()))
          .append('\t')
          .append(row.dividend().trim())
          .append('\t')
          .append(row.taxBase().trim());
    }

    if (bulkInput.toString().equals(BULK_INPUT_HEADER)) {
      throw new IllegalArgumentException("PLUS ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
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
  public record PlusDividendPage(List<PlusDividendRow> content, boolean last) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlusDividendRow(String wkdate, String dkdate, String dividend, String taxBase) {}
}
