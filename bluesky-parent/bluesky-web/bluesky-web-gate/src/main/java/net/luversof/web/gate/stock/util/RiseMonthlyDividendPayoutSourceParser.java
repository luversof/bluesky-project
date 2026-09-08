package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RiseMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  public String toBulkInput(String html) {
    if (!StringUtils.hasText(html)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.empty", "RISE"));
    }

    Document document = Jsoup.parse(html);
    Element heading =
        document.select("h3.heading03").stream()
            .filter(element -> "분배금 지급현황".equals(normalizeText(element.text())))
            .findFirst()
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        msg("stock.monthly.reference.error.source.rise.section.missing")));

    Element wrapper = heading.closest("div.wrap_inner");
    Element table =
        findNextTable(wrapper != null ? wrapper.nextElementSibling() : heading.parent());
    if (table == null) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rise.table.missing"));
    }

    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (Element row : table.select("tbody > tr")) {
      Elements cells = row.select("td");
      if (cells.size() < 4) {
        continue;
      }

      String recordDate = cleanCell(cells.get(0));
      String payDate = cleanCell(cells.get(1));
      String dividendAmount = cleanCell(cells.get(2));
      String taxableBase = cleanCell(cells.get(3));
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
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "RISE"));
    }

    return bulkInput.toString();
  }

  private Element findNextTable(Element current) {
    Element sibling = current;
    while (sibling != null) {
      Element table = sibling.selectFirst("table");
      if (table != null) {
        return table;
      }
      sibling = sibling.nextElementSibling();
    }
    return null;
  }

  private String cleanCell(Element cell) {
    return normalizeText(cell.text());
  }

  private String normalizeText(String value) {
    return value != null ? value.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim() : "";
  }
}
