package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

/**
 * 운용사 상세에서 총보수 · 상장일 읽기(2026-09-28). 조각은 그날 받은 실제 페이지에서 해당 칸 주변만 잘랐다 &mdash; 헷갈리게 하는 부분(세부 보수, 탭
 * 이름의 "상장일", 스크립트 주석, {@code &#37;}, 칸이 태그로 쪼개진 값)을 일부러 남겼다.
 */
class MonthlyDividendSourceFactsParserTest {

  private final MonthlyDividendSourceFactsParser parser =
      new MonthlyDividendSourceFactsParser(JsonMapper.builder().build());

  @Test
  void TIGER_는_퍼센트를_엔티티로_적는다() {
    String html =
        """
        <div class="lead-closer"><div class="each"><div class="title">상장일</div>
        <div class="desc">2019-07-19</div></div>
        <div class="each"><div class="title">벤치마크</div><div class="desc">FnGuide 리츠부동산인프라</div></div></div>
        <h3>운용 세부 정보</h3>
        <div class="c-card" data-type="definition"><div class="c-card-header">총보수</div>
        <div class="c-card-content"><p>연 0.08&#37; </p>
        운용: 0.054&#37;<br/> 지정참가: 0.001&#37;<br/> 신탁: 0.015&#37;<br/> 일반사무: 0.01&#37;</div></div>
        """;

    var facts = parser.fromDetailHtml(html);

    assertThat(facts.totalExpenseRatioPct()).isEqualByComparingTo("0.08");
    assertThat(facts.listingDate()).isEqualTo(LocalDate.of(2019, 7, 19));
  }

  @Test
  void SOL_은_총보수_뒤에_세부_보수를_괄호로_적는다() {
    String html =
        """
        <dl class="def"><dt>상장일</dt><dd>2025.09.23</dd></dl>
        <dl class="def"><dt>설정단위</dt><dd>50,000좌</dd></dl>
        <dl class="def"><dt>총보수</dt>
        <dd>0.150000%(집합투자: 0.109000%,<br/>AP/LP: 0.001000%, 신탁업자: 0.020000%, 일반사무: 0.020000%)</dd></dl>
        """;

    var facts = parser.fromDetailHtml(html);

    assertThat(facts.totalExpenseRatioPct()).isEqualByComparingTo("0.15");
    assertThat(facts.listingDate()).isEqualTo(LocalDate.of(2025, 9, 23));
  }

  /** PLUS 는 칸 이름이 "보수 (연)" 이고 값이 태그로 쪼개져 있다. 스크립트 주석의 "상장일" 은 칸이 아니다. */
  @Test
  void PLUS_는_보수_연_칸과_쪼개진_값() {
    String html =
        """
        <script>
          // 1달 탭을 눌렀는데 상장일과 wkdate 차이가 30일도 안 될때 상장일로
          var fee = "보수 (연) 9.99%";
        </script>
        <span class="sub-pages__base-index-title">상장일</span></div>
        <div class="sub-pages__base-index-num-wrap"><span class="sub-pages__base-index-num">2025.03.05</span></div>
        <h3 class="c-accordion__title">보수 (연)</h3></div></div>
        <div class="c-accordion__conts"><div class="c-accordion__desc">
        <span>0.3</span>%
        (집합투자 : <span>0.259</span>%
        , 지정참가회사 : <span>0.001</span>%
        """;

    var facts = parser.fromDetailHtml(html);

    assertThat(facts.totalExpenseRatioPct()).isEqualByComparingTo("0.3");
    assertThat(facts.listingDate()).isEqualTo(LocalDate.of(2025, 3, 5));
  }

  /** TIME 은 세부 보수 넷을 먼저 적고 끝에 "총 0.80%" - 첫 % 가 아니라 합계다. 탭 이름 "상장일" 뒤에는 날짜가 없다. */
  @Test
  void TIME_은_합계를_끝에_적는다() {
    String html =
        """
        <input type="radio" name="top10Tab" id="top10Tab5" class="top10tab" data-pastperiod="listingDate">
        <label for="top10Tab5">상장일</label></div><div>종목명</div><div>비중(%)</div><div>1</div><div>SK하이닉스</div>
        <dl><dt>순자산총액</dt><dd>8,085 억</dd></dl><dl><dt>상장일</dt><dd>2022.09.27</dd></dl>
        <table><tr><th>운용</th><th>AP/LP</th><th>수탁</th><th>사무수탁</th><th class="total">총보수(연%)</th></tr>
        <tr><td>0.69%</td><td>0.05%</td><td>0.03%</td><td>0.03%</td><td class="total">총 0.80%</td></tr></table>
        """;

    var facts = parser.fromDetailHtml(html);

    assertThat(facts.totalExpenseRatioPct()).isEqualByComparingTo("0.80");
    assertThat(facts.listingDate()).isEqualTo(LocalDate.of(2022, 9, 27));
  }

  @Test
  void RISE_basic_info() {
    String json =
        """
        {"fund_cd":"44J2","basic_info":{"base_dt":"20260927","net_asset_total":347532660743.0,
         "listing_dt":"2025-09-02T00:00:00","shares_per_cu":50000.0},
         "fees":{"bosu_total":0.3,"bosu_panmae":0.001,"bosu_sutak":0.02,"bosu_witak":0.259}}
        """;

    var facts = parser.fromRiseBasicInfoJson(json);

    assertThat(facts.totalExpenseRatioPct()).isEqualByComparingTo("0.3");
    assertThat(facts.listingDate()).isEqualTo(LocalDate.of(2025, 9, 2));
  }

  @Test
  void KODEX_상품_API() {
    String json =
        """
        {"info":{"product":{"gijunYMD":"20260923","listD":"20240305",
         "bosuInfo":"0.090% (지정참가회사 : 0.001%, 집합투자 : 0.069%, 신탁 : 0.010%, 일반사무 : 0.010%)"},
         "divideList":[{"BASIC_D":"20260915","PAY_D":"20260917"}]}}
        """;

    var facts = parser.fromKodexProductJson(json);

    assertThat(facts.totalExpenseRatioPct()).isEqualByComparingTo("0.09");
    assertThat(facts.listingDate()).isEqualTo(LocalDate.of(2024, 3, 5));
  }

  /** 못 읽으면 예외가 아니라 null - 이 값 때문에 등록 · 가져오기가 실패하면 안 된다. 0 으로 채우지도 않는다. */
  @Test
  void 못_읽으면_null_이고_예외가_없다() {
    assertThat(parser.fromDetailHtml("<html><body>총보수 정보 없음</body></html>"))
        .isEqualTo(MonthlyDividendSourceFactsParser.SourceFacts.NONE);
    assertThat(parser.fromDetailHtml(null))
        .isEqualTo(MonthlyDividendSourceFactsParser.SourceFacts.NONE);
    assertThat(parser.fromRiseBasicInfoJson("not json"))
        .isEqualTo(MonthlyDividendSourceFactsParser.SourceFacts.NONE);
    assertThat(parser.fromKodexProductJson("{}"))
        .isEqualTo(MonthlyDividendSourceFactsParser.SourceFacts.NONE);
  }

  /** 100% 이상은 다른 칸을 집은 것이다(예: 비중 %) - 버린다. */
  @Test
  void 말이_안_되는_보수는_버린다() {
    assertThat(MonthlyDividendSourceFactsParser.sane(new BigDecimal("120"))).isNull();
    assertThat(MonthlyDividendSourceFactsParser.sane(new BigDecimal("-0.1"))).isNull();
    assertThat(MonthlyDividendSourceFactsParser.sane(new BigDecimal("0.0795")))
        .isEqualByComparingTo("0.0795");
  }
}
