package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TigerMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  public String toBulkInput(String html) {
    if (!StringUtils.hasText(html)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.empty", "TIGER"));
    }

    Document document = Jsoup.parseBodyFragment("<table><tbody>" + html + "</tbody></table>");
    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (Element row : document.select("tr")) {
      Elements cells = row.select("td");
      if (cells.size() < 4) {
        continue;
      }

      String recordDate = normalizeText(cells.get(0).text());
      String payDate = normalizeText(cells.get(1).text());
      String dividendAmount = normalizeText(cells.get(2).text());
      String taxableBase = normalizeText(cells.get(3).text());
      // 과세표준이 비었다고 지급 행을 버리지 않는다(2026-10-08) - 분배금 이력이 빠진다. 과세표준만 모름("-")으로 싣는다.
      if (!StringUtils.hasText(recordDate)
          || !StringUtils.hasText(payDate)
          || !StringUtils.hasText(dividendAmount)) {
        continue;
      }
      if (!StringUtils.hasText(taxableBase)) {
        taxableBase = "-";
      }

      bulkInput
          .append('\n')
          .append(recordDate)
          .append('\t')
          .append(payDate)
          .append('\t')
          .append(dividendAmount)
          .append('\t')
          .append(taxableBase);
    }

    if (bulkInput.toString().equals(BULK_INPUT_HEADER)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "TIGER"));
    }

    return bulkInput.toString();
  }

  private String normalizeText(String value) {
    return value != null ? value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim() : "";
  }
}
