package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.MonthlyDividendSourceMetaParser.SourceMeta;
import net.luversof.web.gate.stock.util.TimeMonthlyDividendPayoutSourceParser.TimeDividendRow;
import tools.jackson.databind.json.JsonMapper;

/**
 * TIME ETF(타임폴리오자산운용) 출처 &mdash; 사용자 요청 2026-09-22("time etf도 프로필 등록 가능하도록 처리해줘").
 *
 * <p>여기 담은 HTML 조각은 실측이다(2026-09-22, {@code timeetf.co.kr/m11_view.php?idx=12} · TIME Korea플러스배당액티브
 * 441800). 이 사이트는 지급 이력을 상세 HTML 안에 숨겨 두고 단추로 보여 주기만 한다 &mdash; 따로 부를 API 가 없다.
 */
class TimeMonthlyDividendPayoutSourceParserTest {

  private final TimeMonthlyDividendPayoutSourceParser parser =
      new TimeMonthlyDividendPayoutSourceParser();

  private final MonthlyDividendSourceMetaParser metaParser =
      new MonthlyDividendSourceMetaParser(JsonMapper.builder().build());

  /** 실측 HTML 의 모양을 줄인 것 - 머리글 줄에 td 가 없는 것까지 그대로다. */
  private static final String HTML =
      """
      <html><head><title>TIME Korea플러스배당액티브(441800)</title></head><body>
      <div class="table">
       <table cellpadding="0" cellspacing="0" class="table3 moreList1">
        <thead><tr><th>종목코드</th><th>종목명</th></tr></thead>
        <tbody><tr><td>000660</td><td>SK하이닉스</td></tr></tbody>
       </table>
      </div>
      <div class="layerPop pop3year">
       <div class="title">최근 3년 분배금 지급현황</div>
       <table cellpadding="0" cellspacing="0" class="moreList3">
        <tr><th>지급기준일</th><th>지급일</th><th>분배금액 (원)</th><th>주당과세표준액 (원)</th></tr>
        <tbody>
         <tr><td>2026.08.31</td><td>2026.09.02</td><td>145</td><td>3</td></tr>
         <tr><td>2026.07.31</td><td>2026.08.04</td><td>134</td><td>0</td></tr>
         <tr><td>2026.06.30</td><td>2026.07.02</td><td>368</td><td>0</td></tr>
        </tbody>
       </table>
      </div>
      </body></html>
      """;

  @Test
  void 우리가_맡는_주소인가() {
    assertThat(TimeMonthlyDividendPayoutSourceParser.supportsHost("timeetf.co.kr")).isTrue();
    assertThat(TimeMonthlyDividendPayoutSourceParser.supportsHost("www.TimeETF.co.kr")).isTrue();
    assertThat(TimeMonthlyDividendPayoutSourceParser.supportsHost("soletf.co.kr")).isFalse();
    assertThat(TimeMonthlyDividendPayoutSourceParser.supportsHost(null)).isFalse();
  }

  @Test
  void 지급_이력을_읽는다() {
    List<TimeDividendRow> rows = parser.parseRows(HTML);

    assertThat(rows).hasSize(3);
    assertThat(rows.get(0).recordDate()).isEqualTo("2026.08.31");
    assertThat(rows.get(0).payDate()).isEqualTo("2026.09.02");
    assertThat(rows.get(0).dividendAmount()).isEqualByComparingTo("145");
    assertThat(rows.get(0).taxableBase()).isEqualByComparingTo("3");
  }

  @Test
  void 구성_종목_표를_지급_이력으로_읽지_않는다() {
    // 이 페이지에는 구성 종목 표(moreList1)가 먼저 나온다. 아무 표나 집으면 SK하이닉스 한 줄이 분배금이 된다.
    List<TimeDividendRow> rows = parser.parseRows(HTML);

    assertThat(rows).noneMatch(row -> "000660".equals(row.recordDate()));
  }

  @Test
  void 머리글_줄은_한_건으로_세지_않는다() {
    // 이 표는 thead 없이 th 줄을 섞어 둔다 - 칸 개수로만 세면 "지급기준일" 이 한 건이 된다.
    List<TimeDividendRow> rows = parser.parseRows(HTML);

    assertThat(rows).noneMatch(row -> row.recordDate().contains("지급기준일"));
  }

  @Test
  void 벌크_입력으로_바꾼다() {
    String bulkInput = parser.toBulkInput(parser.parseRows(HTML));
    String[] lines = bulkInput.split("\n");

    assertThat(lines[0]).isEqualTo("지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)");
    assertThat(lines[1]).isEqualTo("2026-08-31\t2026-09-02\t145\t3");
    // 과세표준 0 은 빈 칸이 아니라 0 으로 적는다 - 가져오기 파서가 빈 칸을 거부한다.
    assertThat(lines[2]).isEqualTo("2026-07-31\t2026-08-04\t134\t0");
    assertThat(lines).hasSize(4);
  }

  @Test
  void 종목코드와_이름은_제목에서_읽는다() {
    SourceMeta meta = metaParser.fromTimeHtml(HTML);

    // 본문에는 구성 종목 코드(000660 등)가 수십 개 있다 - 아무 여섯 자리나 집으면 엉뚱한 종목이 걸린다.
    assertThat(meta.symbol()).isEqualTo("441800");
    assertThat(meta.name()).isEqualTo("TIME Korea플러스배당액티브");
  }

  @Test
  void 표가_없으면_까닭을_말한다() {
    assertThatThrownBy(() -> parser.parseRows("<html><body>없다</body></html>"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parser.parseRows("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 제목에_코드가_없으면_까닭을_말한다() {
    assertThatThrownBy(() -> metaParser.fromTimeHtml("<html><title>이름만 있다</title></html>"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
