package net.luversof.web.gate.stock.util;

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
      throw new IllegalArgumentException("TIGER ETF 출처 응답이 비어 있습니다.");
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
      if (!StringUtils.hasText(recordDate)
          || !StringUtils.hasText(payDate)
          || !StringUtils.hasText(dividendAmount)
          || !StringUtils.hasText(taxableBase)) {
        continue;
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
      throw new IllegalArgumentException("TIGER ETF 출처에서 분배금 지급 이력을 찾지 못했습니다.");
    }

    return bulkInput.toString();
  }

  private String normalizeText(String value) {
    return value != null ? value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim() : "";
  }
}
