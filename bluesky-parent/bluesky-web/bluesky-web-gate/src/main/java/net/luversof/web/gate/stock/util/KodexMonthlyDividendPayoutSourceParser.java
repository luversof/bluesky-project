package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import tools.jackson.databind.json.JsonMapper;

@Component
public class KodexMonthlyDividendPayoutSourceParser {

  private final JsonMapper objectMapper;

  public KodexMonthlyDividendPayoutSourceParser(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public KodexDividendResponse parseResponse(String json) {
    try {
      return objectMapper.readValue(json, KodexDividendResponse.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.unparseable", "KODEX"), ex);
    }
  }

  public String toBulkInput(List<KodexDividendRow> rows) {
    return PayoutSourceText.bulkInput(
        rows,
        "KODEX",
        // 과세표준이 비었다고 지급 행을 버리지 않는다(2026-10-08) - 과세표준만 모름("-")으로 싣는다.
        row ->
            !StringUtils.hasText(row.basicD())
                    || !StringUtils.hasText(row.payD())
                    || !StringUtils.hasText(row.dividA())
                ? null
                : new PayoutSourceText.BulkRow(
                    PayoutSourceText.isoDateOfCompact(row.basicD()),
                    PayoutSourceText.isoDateOfCompact(row.payD()),
                    row.dividA().trim(),
                    StringUtils.hasText(row.taxDividA()) ? row.taxDividA().trim() : "-"));
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KodexDividendResponse(List<KodexDividendRow> dividList) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record KodexDividendRow(String basicD, String payD, String dividA, String taxDividA) {}
}
