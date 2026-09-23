package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * TIME ETF(타임폴리오자산운용) 출처의 분배금 지급 이력(사용자 요청 2026-09-22).
 *
 * <p>다른 운용사와 달리 <b>따로 부를 것이 없다</b> &mdash; "최근 3년 분배금 지급현황" 팝업이 상세 HTML 안에 이미 들어 있고(숨겨진 {@code
 * div.pop3year}), 단추를 누르면 보여 주기만 한다. 그래서 상세 주소 하나로 종목 정보와 지급 이력을 다 얻는다.
 *
 * <p>실측 2026-09-22 (idx=12 · TIME Korea플러스배당액티브 441800):
 *
 * <pre>
 *   &lt;table class="moreList3"&gt;
 *     &lt;tr&gt;&lt;th&gt;지급기준일&lt;/th&gt;&lt;th&gt;지급일&lt;/th&gt;&lt;th&gt;분배금액 (원)&lt;/th&gt;&lt;th&gt;주당과세표준액 (원)&lt;/th&gt;&lt;/tr&gt;
 *     &lt;tr&gt;&lt;td&gt;2026.08.31&lt;/td&gt;&lt;td&gt;2026.09.02&lt;/td&gt;&lt;td&gt;145&lt;/td&gt;&lt;td&gt;3&lt;/td&gt;&lt;/tr&gt;
 * </pre>
 *
 * <p><b>머리글 줄은 건너뛴다.</b> 이 표는 {@code <thead>} 없이 {@code <th>} 줄을 그냥 섞어 놓아, 칸 개수로만 세면 머리글이 한 건으로
 * 들어온다.
 */
@Component
public class TimeMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  /** 지급 이력 표. 이 클래스 이름은 이 사이트에서 이 표에만 쓰인다(실측 2026-09-22). */
  private static final Pattern PAYOUT_TABLE =
      Pattern.compile(
          "<table[^>]*class=\"[^\"]*moreList3[^\"]*\"[^>]*>(.*?)</table>",
          Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  private static final Pattern ROW =
      Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  private static final Pattern CELL =
      Pattern.compile("<td[^>]*>(.*?)</td>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

  private static final Pattern TAG = Pattern.compile("<[^>]+>");

  /** 이 출처를 우리가 맡는가. */
  public static boolean supportsHost(String host) {
    return host != null && host.toLowerCase(Locale.ROOT).contains("timeetf.co.kr");
  }

  /** 상세 HTML 에서 지급 이력 줄을 뽑는다. */
  public List<TimeDividendRow> parseRows(String html) {
    if (!StringUtils.hasText(html)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "TIME"));
    }

    Matcher table = PAYOUT_TABLE.matcher(html);
    if (!table.find()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "TIME"));
    }

    List<TimeDividendRow> rows = new ArrayList<>();
    Matcher row = ROW.matcher(table.group(1));
    while (row.find()) {
      List<String> cells = new ArrayList<>();
      Matcher cell = CELL.matcher(row.group(1));
      while (cell.find()) {
        cells.add(text(cell.group(1)));
      }
      // 머리글 줄은 td 가 없다 - 여기서 저절로 빠진다.
      if (cells.size() < 3) {
        continue;
      }

      rows.add(
          new TimeDividendRow(
              cells.get(0),
              cells.get(1),
              number(cells.get(2)),
              cells.size() > 3 ? number(cells.get(3)) : null));
    }
    return rows;
  }

  public String toBulkInput(List<TimeDividendRow> rows) {
    if (rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "TIME"));
    }

    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (TimeDividendRow row : rows) {
      if (!StringUtils.hasText(row.recordDate())
          || !StringUtils.hasText(row.payDate())
          || row.dividendAmount() == null) {
        continue;
      }

      bulkInput
          .append('\n')
          .append(formatDate(row.recordDate()))
          .append('\t')
          .append(formatDate(row.payDate()))
          .append('\t')
          .append(plain(row.dividendAmount()))
          // 과세표준이 없는 달은 빈 칸이 아니라 0 으로 적는다(가져오기 파서가 빈 칸을 거부한다).
          .append('\t')
          .append(plain(row.taxableBase() != null ? row.taxableBase() : BigDecimal.ZERO));
    }

    if (bulkInput.toString().equals(BULK_INPUT_HEADER)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "TIME"));
    }

    return bulkInput.toString();
  }

  /** 2026.08.31 · 2026-08-31 · 20260831 을 모두 yyyy-MM-dd 로. */
  private String formatDate(String value) {
    String digits = value != null ? value.replaceAll("[^0-9]", "") : "";
    if (digits.length() < 8) {
      return value != null ? value.trim() : "";
    }

    return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
  }

  private BigDecimal number(String value) {
    String cleaned = value != null ? value.replaceAll("[^0-9.-]", "") : "";
    if (!StringUtils.hasText(cleaned)) {
      return null;
    }

    try {
      return new BigDecimal(cleaned);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  private String text(String html) {
    return TAG.matcher(html)
        .replaceAll(" ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replaceAll("[\\s]+", " ")
        .trim();
  }

  /** 표 한 줄(지급기준일 · 지급일 · 분배금액 · 주당과세표준액). */
  public record TimeDividendRow(
      String recordDate, String payDate, BigDecimal dividendAmount, BigDecimal taxableBase) {}
}
