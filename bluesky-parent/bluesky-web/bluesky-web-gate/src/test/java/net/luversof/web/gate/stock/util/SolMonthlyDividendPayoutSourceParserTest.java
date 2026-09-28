package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.util.MonthlyDividendSourceMetaParser.SourceMeta;
import net.luversof.web.gate.stock.util.SolMonthlyDividendPayoutSourceParser.SolDividendResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * SOL ETF(신한자산운용) 출처 &mdash; 사용자 요청 2026-09-21("sol etf도 가져오기를 지원하도록 추가해줘").
 *
 * <p>상세 화면의 "분배금 현황" 팝업이 부르는 {@code /api/etf/pds/dividend/{fundCd}} 를 그대로 읽는다. 실측(211097 · SOL
 * 코리아고배당, 11 건)한 응답 모양을 여기 고정한다.
 */
class SolMonthlyDividendPayoutSourceParserTest {

  private final SolMonthlyDividendPayoutSourceParser parser =
      new SolMonthlyDividendPayoutSourceParser(JsonMapper.builder().build());

  private final MonthlyDividendSourceMetaParser metaParser =
      new MonthlyDividendSourceMetaParser(JsonMapper.builder().build());

  private final MonthlyDividendPayoutImportParser importParser =
      new MonthlyDividendPayoutImportParser();

  private static final String SAMPLE_JSON =
      """
			{
			  "workDt": "20260915",
			  "fundName": "SOL 코리아고배당",
			  "totalCount": 11,
			  "items": [
			    {"FUND_CD":"211097","WORK_DT":"20260915","DIVIDEND_DT":"20260916","DIVIDEND_PRI":60,"WEEK_PRI":17,"TAX_PRI":10083.82,"BFAS_STAS_STPR":10100.95},
			    {"FUND_CD":"211097","WORK_DT":"20260814","DIVIDEND_DT":"20260818","DIVIDEND_PRI":60,"WEEK_PRI":29,"TAX_PRI":9900.10,"BFAS_STAS_STPR":9930.20}
			  ]
			}
			""";

  @Test
  void 팝업이_부르는_JSON_을_가져오기_입력으로_바꾼다() {
    SolDividendResponse response = parser.parseResponse(SAMPLE_JSON);
    assertThat(response.fundName()).isEqualTo("SOL 코리아고배당");

    List<MonthlyDividendPayoutUpsertRequest> requests =
        importParser.parse("0105E0", parser.toBulkInput(response.items()));

    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 9, 15));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 9, 16));
    assertThat(requests.get(0).getDividendAmountPerShare()).isEqualByComparingTo("60");
    assertThat(requests.get(0).getTaxableBasePerShare())
        .as("주당과세표준액은 WEEK_PRI 다 - TAX_PRI(과표기준가 10,083.82)를 쓰면 분배금보다 커져 검사에 걸린다")
        .isEqualByComparingTo("17");
    assertThat(requests.get(1).getTaxableBasePerShare()).isEqualByComparingTo("29");
  }

  @Test
  void 상세_HTML_에서_종목코드와_이름을_읽는다() {
    String html =
        """
				<h1 data-addtitle="this" class="fv-name">
				  <span>SOL 코리아고배당</span>
				  <small class="fd-code">(0105E0)</small>
				</h1>
				<ul class="fv-des"><li>매월 중순에 분배금을 지급하며, 국내 고배당 우량주 30종목에 투자하는 ETF</li></ul>
				""";

    SourceMeta meta = metaParser.fromSolHtml(html);
    assertThat(meta.symbol()).as("주소의 211097 은 펀드 코드라 종목코드가 아니다 - 본문에서 읽어야 한다").isEqualTo("0105E0");
    assertThat(meta.name()).isEqualTo("SOL 코리아고배당");
  }

  @Test
  void SOL_출처_주소만_맡고_펀드_코드는_마지막_조각이다() {
    assertThat(SolMonthlyDividendPayoutSourceParser.supportsHost("www.soletf.co.kr")).isTrue();
    assertThat(SolMonthlyDividendPayoutSourceParser.supportsHost("WWW.SOLETF.CO.KR")).isTrue();
    assertThat(SolMonthlyDividendPayoutSourceParser.supportsHost("www.samsungfund.com")).isFalse();
    assertThat(SolMonthlyDividendPayoutSourceParser.supportsHost(null)).isFalse();

    assertThat(SolMonthlyDividendPayoutSourceParser.fundCodeFrom("/ko/fund/etf/211097"))
        .isEqualTo("211097");
    assertThat(SolMonthlyDividendPayoutSourceParser.fundCodeFrom("/ko/fund/etf/211097/"))
        .isEqualTo("211097");
    assertThat(SolMonthlyDividendPayoutSourceParser.fundCodeFrom("")).isEmpty();
  }

  @Test
  void 칸에_적히는_글자까지_정해_둔다() {
    String bulkInput =
        parser.toBulkInput(
            List.of(
                new SolMonthlyDividendPayoutSourceParser.SolDividendRow(
                    "2026.08.14", "20260818", new BigDecimal("1200.0"), null)));

    String row = bulkInput.substring(bulkInput.indexOf((char) 10) + 1);
    assertThat(row)
        .as("점 찍힌 날짜도 같은 날로, 과세표준이 없으면 0 으로, 지수 표기 없이")
        .isEqualTo("2026-08-14\t2026-08-18\t1200\t0");
  }

  @Test
  void 이력이_비면_사유를_남긴다() {
    assertThatThrownBy(() -> parser.toBulkInput(List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parser.parseResponse("{"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  /** 판정만 있고 쓰이지 않으면 소용없다 - 가져오기가 그 판정을 쓰는지 본다. */
  @Test
  void 가져오기가_SOL_분기를_쓴다() throws java.io.IOException {
    String service =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/util/"
                    + "MonthlyDividendPayoutSourceImportService.java"),
            java.nio.charset.StandardCharsets.UTF_8);

    // 판정과 부르는 곳을 짝으로 본다 - 따로 보면 메타 분기만 끊어도 통과한다(변이 실험 2026-09-21).
    String squashed = service.replaceAll("\\s+", " ");
    assertThat(squashed)
        .as("메타는 SOL 판정 뒤에 SOL 파서를 불러야 한다")
        // 2026-09-28 총보수 · 상장일을 같은 HTML 에서 읽게 뒤에 withFacts 를 이었다 - 짝(SOL 판정 → SOL 파서)은 그대로다.
        .contains(
            "if (SolMonthlyDividendPayoutSourceParser.supportsHost(host)) {"
                + " return monthlyDividendSourceMetaParser .fromSolHtml(html)"
                + " .withFacts(monthlyDividendSourceFactsParser.fromDetailHtml(html)); }");
    assertThat(squashed)
        .as("이력도 마찬가지")
        .contains(
            "} else if (SolMonthlyDividendPayoutSourceParser.supportsHost(host)) {"
                + " bulkInput = solMonthlyDividendPayoutSourceParser.toBulkInput(fetchSolRows(sourceUri));");
    assertThat(service).contains("/api/etf/pds/dividend/{fundCode}");
  }
}
