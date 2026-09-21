package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.json.JsonMapper;

/**
 * SOL ETF(신한자산운용) 출처의 분배금 지급 이력(사용자 요청 2026-09-21).
 *
 * <p>상세 화면은 "분배금 현황" 을 팝업으로 띄우는데, 그 팝업이 부르는 것이 {@code /api/etf/pds/dividend/{fundCd}} 다. 응답의 {@code
 * items} 가 팝업 표와 같은 순서로 온다 &mdash; 실측 2026-09-21(211097 · SOL 코리아고배당, 11 건):
 *
 * <pre>
 *   표    지급기준일   실제지급일   분배금액(원)  주당과세표준액(원)  과표기준가    배당과표기준가
 *   값    2026.09.15  2026.09.16  60           17                10,083.82    10,100.95
 *   JSON  WORK_DT     DIVIDEND_DT DIVIDEND_PRI WEEK_PRI          TAX_PRI      BFAS_STAS_STPR
 * </pre>
 *
 * <p><b>주당과세표준액은 {@code WEEK_PRI} 다</b> &mdash; 이름만 보면 주간 값 같지만 팝업 표의 네 번째 칸과 값이 같다. {@code
 * TAX_PRI}(과표기준가, 10,083.82)를 쓰면 분배금보다 커져 "과세표준이 분배금을 넘을 수 없다" 는 검사에 걸린다.
 *
 * <p>주소의 마지막 조각({@code 211097})은 <b>펀드 코드</b>라 종목코드(0105E0)와 다르다 &mdash; RISE 와 같은 구조다.
 */
@Component
public class SolMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  private final JsonMapper objectMapper;

  public SolMonthlyDividendPayoutSourceParser(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** 이 출처를 우리가 맡는가. */
  public static boolean supportsHost(String host) {
    return host != null && host.toLowerCase(Locale.ROOT).contains("soletf.co.kr");
  }

  /** 상세 주소에서 펀드 코드를 뽑는다(경로 마지막 조각). {@code /ko/fund/etf/211097} */
  public static String fundCodeFrom(String path) {
    if (!StringUtils.hasText(path)) {
      return "";
    }

    String[] parts = path.split("/");
    for (int index = parts.length - 1; index >= 0; index--) {
      String candidate = parts[index].trim();
      if (StringUtils.hasText(candidate)
          && candidate.chars().allMatch(Character::isLetterOrDigit)) {
        return candidate;
      }
    }
    return "";
  }

  public SolDividendResponse parseResponse(String json) {
    try {
      return objectMapper.readValue(json, SolDividendResponse.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.unparseable", "SOL"), ex);
    }
  }

  public String toBulkInput(List<SolDividendRow> rows) {
    if (rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "SOL"));
    }

    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (SolDividendRow row : rows) {
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
          msg("stock.monthly.reference.error.source.rows.missing", "SOL"));
    }

    return bulkInput.toString();
  }

  /** 20260915 · 2026.09.15 · 2026-09-15 를 모두 yyyy-MM-dd 로. */
  private String formatDate(String value) {
    String normalized = value != null ? value.replaceAll("[^0-9]", "") : "";
    if (normalized.length() < 8) {
      return value != null ? value.trim() : "";
    }

    return normalized.substring(0, 4)
        + "-"
        + normalized.substring(4, 6)
        + "-"
        + normalized.substring(6, 8);
  }

  /** 60.0 을 60 으로(지수 표기도 막는다). */
  private String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SolDividendResponse(
      @JsonProperty("fundName") String fundName, List<SolDividendRow> items) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record SolDividendRow(
      @JsonProperty("WORK_DT") String recordDate,
      @JsonProperty("DIVIDEND_DT") String payDate,
      @JsonProperty("DIVIDEND_PRI") BigDecimal dividendAmount,
      @JsonProperty("WEEK_PRI") BigDecimal taxableBase) {}
}
