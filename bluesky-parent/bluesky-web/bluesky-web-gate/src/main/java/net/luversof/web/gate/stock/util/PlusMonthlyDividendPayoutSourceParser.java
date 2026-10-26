package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.json.JsonMapper;

@Component
public class PlusMonthlyDividendPayoutSourceParser {

  private final JsonMapper objectMapper;

  public PlusMonthlyDividendPayoutSourceParser(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public PlusDividendPage parsePage(String json) {
    try {
      return objectMapper.readValue(json, PlusDividendPage.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.unparseable", "PLUS"), ex);
    }
  }

  public String toBulkInput(List<PlusDividendRow> rows) {
    return PayoutSourceText.bulkInput(
        rows,
        "PLUS",
        // 과세표준이 비었다고 지급 행을 버리지 않는다(2026-10-08) - 과세표준만 모름("-")으로 싣는다.
        row ->
            !StringUtils.hasText(row.wkdate())
                    || !StringUtils.hasText(row.dkdate())
                    || !StringUtils.hasText(row.dividend())
                ? null
                : new PayoutSourceText.BulkRow(
                    PayoutSourceText.isoDateOfCompact(row.wkdate()),
                    PayoutSourceText.isoDateOfCompact(row.dkdate()),
                    row.dividend().trim(),
                    StringUtils.hasText(row.taxBase()) ? row.taxBase().trim() : "-"));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlusDividendPage(List<PlusDividendRow> content, boolean last) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record PlusDividendRow(String wkdate, String dkdate, String dividend, String taxBase) {}
}
