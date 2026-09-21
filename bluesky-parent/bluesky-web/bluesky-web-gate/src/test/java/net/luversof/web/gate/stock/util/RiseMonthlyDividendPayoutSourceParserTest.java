package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.util.RiseMonthlyDividendPayoutSourceParser.RiseDividendResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * RISE(KB자산운용) 출처 파서.
 *
 * <p>2026-09-21 사이트가 단일 페이지 앱으로 바뀌어 상세 HTML 에 "분배금 지급현황" 표가 사라졌다 &mdash; 그때부터 화면의 "이 출처에서 가져오기" 가
 * <i>"RISE ETF 출처에서 분배금 지급현황 영역을 찾지 못했습니다"</i> 로 끝났다(실측). 지금은 화면이 쓰는 JSON 을 그대로 읽는다.
 */
class RiseMonthlyDividendPayoutSourceParserTest {

  private final RiseMonthlyDividendPayoutSourceParser parser =
      new RiseMonthlyDividendPayoutSourceParser(JsonMapper.builder().build());

  private final MonthlyDividendPayoutImportParser importParser =
      new MonthlyDividendPayoutImportParser();

  /** 실제 응답 모양(44J2, 2026-09-21 실측)에서 필요한 칸만 남긴 것. */
  private static final String SAMPLE_JSON =
      """
			{
			  "fund_cd": "44J2",
			  "name": "RISE 코리아밸류업위클리고정커버드콜",
			  "info": {"payment_schedule": "매월 15일(다만, 매월 15일이 영업일이 아닌 경우 그 직전 영업일)"},
			  "history": [
			    {"base_date":"2026-09-15","payment_date":"2026-09-17","amount":540.0,"tax_standard_amount":9.0,"dividend_ratio":2.98},
			    {"base_date":"2026-08-14","payment_date":"2026-08-19","amount":420.0,"tax_standard_amount":0.0,"dividend_ratio":2.4}
			  ]
			}
			""";

  @Test
  void JSON_지급이력을_가져오기_입력으로_바꾼다() {
    RiseDividendResponse response = parser.parseResponse(SAMPLE_JSON);
    assertThat(response.fundCode()).isEqualTo("44J2");
    assertThat(response.name()).isEqualTo("RISE 코리아밸류업위클리고정커버드콜");

    List<MonthlyDividendPayoutUpsertRequest> requests =
        importParser.parse("0094M0", parser.toBulkInput(response.history()));

    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 9, 17));
    // 540.0 을 그대로 실으면 숫자 파싱이 소수로 읽는다 - 540 으로 적는다.
    assertThat(requests.get(0).getDividendAmountPerShare()).isEqualByComparingTo("540");
    assertThat(requests.get(0).getTaxableBasePerShare()).isEqualByComparingTo("9");
    // 전액 비과세인 달은 과세표준 0 이다(빠진 값이 아니라 0).
    assertThat(requests.get(1).getTaxableBasePerShare()).isEqualByComparingTo("0");
  }

  /** 가져오기 파서는 칸을 글자로 읽는다 - 빈 칸과 0, 지수 표기가 섞이면 값이 달라진다. */
  @Test
  void 칸에_적히는_글자까지_정해_둔다() {
    String bulkInput =
        parser.toBulkInput(
            List.of(
                new RiseMonthlyDividendPayoutSourceParser.RiseDividendRow(
                    "2026-08-14", "2026-08-19", new BigDecimal("1200.0"), null)));

    String row = bulkInput.substring(bulkInput.indexOf(chr10()) + 1);
    assertThat(row).as("과세표준이 없으면 0 이라고 적는다(빈 칸이 아니라)").isEqualTo("2026-08-14	2026-08-19	1200	0");
    assertThat(row).as("지수 표기가 섞이면 안 된다").doesNotContain("E");
  }

  private static String chr10() {
    return String.valueOf((char) 10);
  }

  /** 판정만 있고 쓰이지 않으면 소용없다 - 부르는 줄까지 본다(변이 실험 2026-09-21). */
  @Test
  void 가져오기가_그_판정을_쓴다() throws java.io.IOException {
    String service =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/util/"
                    + "MonthlyDividendPayoutSourceImportService.java"),
            java.nio.charset.StandardCharsets.UTF_8);

    assertThat(service.replaceAll("\s+", " "))
        .contains("if (RiseMonthlyDividendPayoutSourceParser.supportsHost(host)) {");
  }

  @Test
  void RISE_출처_주소만_맡는다() {
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost("www.riseetf.co.kr")).isTrue();
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost("kbam.co.kr"))
        .as("사이트가 옮겨 간 주소도 같은 곳이다")
        .isTrue();
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost("WWW.KBAM.CO.KR")).isTrue();
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost("www.samsungfund.com")).isFalse();
    assertThat(RiseMonthlyDividendPayoutSourceParser.supportsHost(null)).isFalse();
  }

  @Test
  void 주소_마지막_조각이_펀드_코드다() {
    assertThat(RiseMonthlyDividendPayoutSourceParser.fundCodeFrom("/prod/finderDetail/44J2"))
        .isEqualTo("44J2");
    assertThat(RiseMonthlyDividendPayoutSourceParser.fundCodeFrom("/products/44J2"))
        .as("kbam.co.kr 로 옮겨 간 주소도 같은 코드다")
        .isEqualTo("44J2");
    assertThat(RiseMonthlyDividendPayoutSourceParser.fundCodeFrom("/products/44J2/"))
        .as("끝 슬래시는 무시한다")
        .isEqualTo("44J2");
    assertThat(RiseMonthlyDividendPayoutSourceParser.fundCodeFrom("")).isEmpty();
    assertThat(RiseMonthlyDividendPayoutSourceParser.fundCodeFrom(null)).isEmpty();
  }

  @Test
  void 이력이_비면_사유를_남긴다() {
    assertThatThrownBy(() -> parser.toBulkInput(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parser.parseResponse("{"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 날짜가 20260915 · 2026-09-15T00:00:00 로 와도 같은 날로 읽어야 한다. */
  @Test
  void 날짜_표기가_달라도_같은_날이다() {
    String bulkInput =
        parser.toBulkInput(
            List.of(
                new RiseMonthlyDividendPayoutSourceParser.RiseDividendRow(
                    "20260915",
                    "2026-09-17T00:00:00",
                    new BigDecimal("540.0"),
                    new BigDecimal("9")),
                new RiseMonthlyDividendPayoutSourceParser.RiseDividendRow(
                    "2026-08-14", "20260819", new BigDecimal("420"), null)));

    List<MonthlyDividendPayoutUpsertRequest> requests = importParser.parse("0094M0", bulkInput);
    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 9, 17));
    assertThat(requests.get(1).getPayDate()).isEqualTo(LocalDate.of(2026, 8, 19));
    assertThat(requests.get(1).getTaxableBasePerShare())
        .as("과세표준이 없으면 0 으로 싣는다(가져오기 파서가 빈 칸을 거부한다)")
        .isEqualByComparingTo("0");
  }
}
