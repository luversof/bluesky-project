package net.luversof.web.gate.stock.util;

import static net.luversof.web.gate.stock.support.StockViewSupport.msg;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.json.JsonMapper;

/**
 * RISE(KB자산운용) 출처의 분배금 지급 이력.
 *
 * <p><b>2026-09-21 사이트가 바뀌었다.</b> 예전에는 상세 페이지 HTML 에 "분배금 지급현황" 제목과 표가 서버에서 그려져 있어 그걸 읽었는데, 지금은
 * kbam.co.kr 의 단일 페이지 앱이라 그 표식이 하나도 없다(실측: heading03 0 건 · "분배금 지급현황" 0 건 · 표 1 개뿐). 그래서 화면에서 "이
 * 출처에서 가져오기" 를 누르면 <i>"RISE ETF 출처에서 분배금 지급현황 영역을 찾지 못했습니다"</i> 로 끝났다 &mdash; 등록된 RISE 프로필 2 건의 지급
 * 이력이 그때부터 멈춰 있었다.
 *
 * <p>지금 화면이 쓰는 것은 {@code /api/products/etfs/{fund_cd}/dividend} 다. 응답의 {@code history} 가 지급기준일 ·
 * 실지급일 · 분배금액 · 주당과세표준액을 그대로 담고 있어 HTML 을 긁을 이유가 없다(실측 44J2: 12 건).
 */
@Component
public class RiseMonthlyDividendPayoutSourceParser {

  private static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  private final JsonMapper objectMapper;

  public RiseMonthlyDividendPayoutSourceParser(JsonMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public RiseDividendResponse parseResponse(String json) {
    try {
      return objectMapper.readValue(json, RiseDividendResponse.class);
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.response.unparseable", "RISE"), ex);
    }
  }

  public String toBulkInput(List<RiseDividendRow> rows) {
    if (rows == null || rows.isEmpty()) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "RISE"));
    }

    StringBuilder bulkInput = new StringBuilder(BULK_INPUT_HEADER);
    for (RiseDividendRow row : rows) {
      if (!StringUtils.hasText(row.baseDate())
          || !StringUtils.hasText(row.paymentDate())
          || row.amount() == null) {
        continue;
      }

      bulkInput
          .append('\n')
          .append(formatDate(row.baseDate()))
          .append('\t')
          .append(formatDate(row.paymentDate()))
          .append('\t')
          .append(plain(row.amount()))
          // 과세표준액이 0 인 달이 있다(전액 비과세). 빠진 값과 구분해 0 을 그대로 싣는다.
          .append('\t')
          .append(
              plain(row.taxStandardAmount() != null ? row.taxStandardAmount() : BigDecimal.ZERO));
    }

    if (bulkInput.toString().equals(BULK_INPUT_HEADER)) {
      throw new IllegalArgumentException(
          msg("stock.monthly.reference.error.source.rows.missing", "RISE"));
    }

    return bulkInput.toString();
  }

  /** 이 출처를 우리가 맡는가. 사이트가 kbam.co.kr 로 옮겨 가는 중이라 둘 다 같은 것으로 본다(2026-09-21). */
  public static boolean supportsHost(String host) {
    if (host == null) {
      return false;
    }

    String normalized = host.toLowerCase(java.util.Locale.ROOT);
    return normalized.contains("riseetf.co.kr") || normalized.contains("kbam.co.kr");
  }

  /**
   * 상세 주소에서 펀드 코드를 뽑는다(경로 마지막 조각). {@code /prod/finderDetail/44J2} · {@code /products/44J2} 둘 다 쓴다.
   */
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

  /** 2026-09-15T00:00:00 · 20260915 · 2026-09-15 을 모두 yyyy-MM-dd 로. */
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

  /** 540.0 을 540 으로(지수 표기도 막는다) - 가져오기 파서가 숫자 문자열을 그대로 읽는다. */
  private String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RiseDividendResponse(
      @JsonProperty("fund_cd") String fundCode, String name, List<RiseDividendRow> history) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record RiseDividendRow(
      @JsonProperty("base_date") String baseDate,
      @JsonProperty("payment_date") String paymentDate,
      BigDecimal amount,
      @JsonProperty("tax_standard_amount") BigDecimal taxStandardAmount) {}
}
