package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import io.github.luversof.boot.context.support.MessageUtil;
import net.luversof.web.gate.stock.dto.response.YearlyCostSummary;

/**
 * 자산 성장의 <b>연도별 세금·비용</b> 표를 렌더해서 본다.
 *
 * <p>이 값들은 원장에 다 있는데 <b>합계를 내는 화면이 없었다</b> &mdash; 그 해 수수료·증권거래세가 얼마였는지, 배당소득세가 얼마 나갔는지를 알려면 매매 내역과
 * 배당 내역을 각각 열어 눈으로 더해야 했다. 연말정산·금투세 때 필요한 수가 전부 그렇다.
 *
 * <p>매매는 거래일, 배당은 <b>지급일</b> 기준으로 해에 넣는다 &mdash; 배당소득세가 그때 원천징수되므로 세금 관점에서는 지급일이 맞다. 그 기준을 화면에 밝힌다.
 */
class YearlyCostSummaryRenderTest {

  private static final String TEMPLATE = "stock/htmx/fragments/yearlyCostSummary.jte";

  @BeforeAll
  static void primeMessages() {
    ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
    source.setBasename("classpath:uiMessage");
    source.setDefaultEncoding("UTF-8");
    source.setUseCodeAsDefaultMessage(true);
    MessageUtil.setMessageSourceAccessor(new MessageSourceAccessor(source));
  }

  @AfterAll
  static void clearMessages() {
    MessageUtil.setMessageSourceAccessor(null);
  }

  private static BigDecimal bd(String value) {
    return new BigDecimal(value);
  }

  private String render(List<YearlyCostSummary> rows) {
    Map<String, Object> model = new HashMap<>();
    model.put("yearlyCosts", rows);
    StringOutput output = new StringOutput();
    TemplateEngine.createPrecompiled(ContentType.Html).render(TEMPLATE, model, output);
    return output.toString();
  }

  /** 실데이터와 같은 모양: 실현손익이 있는 해와 없는 해, 거래세가 0 인 해. */
  private List<YearlyCostSummary> rows() {
    return List.of(
        new YearlyCostSummary(
            2026,
            bd("20933"),
            bd("616963"),
            bd("131261409"),
            bd("26164415"),
            // 과세금액은 세전보다 훨씬 작다 - 계좌별 분리과세 혜택 뒤 남은 몫이다.
            bd("9172513"),
            bd("1412730"),
            bd("24751685")),
        new YearlyCostSummary(
            2024,
            bd("3007"),
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            bd("11219044"),
            bd("10578571"),
            bd("1629100"),
            bd("9589944")));
  }

  @Test
  void 해마다_수수료와_세금과_배당을_한_줄로_모은다() {
    String html = render(rows());

    assertThat(html)
        .as("합계를 내는 화면이 없어 매매·배당 내역을 열어 눈으로 더해야 했다")
        .contains(MessageUtil.getMessage("stock.asset.growth.cost.title"))
        .contains("2026")
        .contains("20,933")
        .contains("616,963")
        .contains("26,164,415")
        .contains("9,172,513")
        .contains("1,412,730")
        .contains("24,751,685");
  }

  /** 실현손익은 부호가 뜻을 바꾸므로 부호를 붙인다. 수수료·세금은 나간 돈이라 금액만 적는다. */
  @Test
  void 실현손익에만_부호를_붙인다() {
    String html = render(rows());

    assertThat(html).contains("+&#8361;131,261,409");
    assertThat(html)
        .as("나간 돈에 + 를 붙이면 번 돈처럼 읽힌다")
        .doesNotContain("+&#8361;20,933")
        .doesNotContain("+&#8361;616,963");
  }

  /** 실현손익이 0 인 해는 빈 자리로 둔다. 0 원과 '그 해엔 판 게 없다' 는 다르게 읽힌다. */
  @Test
  void 실현손익이_0_인_해는_금액을_적지_않는다() {
    String html = render(rows());

    int year2024 = html.indexOf("2024");
    assertThat(year2024).as("2024 줄을 찾지 못했다").isGreaterThan(0);
    assertThat(html.substring(year2024)).contains("11,219,044");
  }

  /**
   * 과세금액은 세전과 <b>다른 칸</b>이어야 한다.
   *
   * <p>세전에 세율을 곱한 값이 아니다 &mdash; 계좌·종목에 따라 분리과세 혜택이 있어 혜택 뒤 남은 몫만 과세된다(실측 2026-08-24: KB증권 x KODEX
   * 한국부동산리츠인프라 8 건은 과세금액이 세전의 0.75% 수준). 금융소득종합과세 판정은 이 값으로 하므로 세전만 보면 판단을 그르친다.
   */
  @Test
  void 과세금액을_세전과_따로_적는다() {
    String html = render(rows());

    assertThat(html)
        .as("세전만 있으면 실제 과세대상이 얼마인지 알 수 없다")
        .contains(MessageUtil.getMessage("stock.asset.growth.cost.col.dividend.taxable"))
        .contains("9,172,513")
        .contains("10,578,571");
  }

  /** 어느 날짜 기준으로 해를 갈랐는지 밝힌다. 밝히지 않으면 배당을 기준일로 넣었는지 알 수 없다. */
  @Test
  void 해를_가르는_기준을_밝힌다() {
    assertThat(render(rows())).contains(MessageUtil.getMessage("stock.asset.growth.cost.desc"));
  }

  @Test
  void 자료가_없으면_아무것도_그리지_않는다() {
    assertThat(render(List.of()).trim()).isEmpty();
  }

  /** 자산 성장 화면이 두 표를 실제로 부르는지. 조각만 만들고 붙이지 않으면 화면에는 없는 것과 같다. */
  @Test
  void 자산_성장_화면이_두_표를_모두_부른다() throws IOException {
    String page =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/asset-growth.jte"), StandardCharsets.UTF_8);

    assertThat(page)
        .as("조각만 만들고 화면에 붙이지 않으면 없는 것과 같다")
        .contains("periodBreakdownTable")
        .contains("yearlyCostSummary");
  }

  /**
   * 화면 쪽 호출이 조각의 파라미터를 <b>빠뜨리지 않는지</b>.
   *
   * <p>{@code periodBreakdownTable} 은 컨트롤러가 뷰로 반환하지 않는 조각이라 {@code FragmentParameterCoverageTest} 의
   * 그물에 걸리지 않는다. 그런데 배선은 똑같이 두 벌이다 &mdash; 컨트롤러가 모델에 넣고, 페이지가 파라미터를 하나씩 적어 넘긴다. 새로 늘린 값을 페이지에 적는 것을
   * 잊으면 JTE 가 조용히 기본값으로 채우고 화면에서는 그냥 안 보인다(2026-08-27 기간 요약에서 실제로 그랬다).
   */
  @Test
  void 자산_성장_화면이_조각의_파라미터를_다_넘긴다() throws IOException {
    String page =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/asset-growth.jte"), StandardCharsets.UTF_8);
    String fragment =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/periodBreakdownTable.jte"),
            StandardCharsets.UTF_8);

    int call = page.indexOf("@template.stock.htmx.fragments.periodBreakdownTable(");
    assertThat(call).as("자산 성장이 기간별 손익 조각을 부르지 않는다").isGreaterThan(0);
    String args = page.substring(call, page.indexOf(')', call));

    List<String> missing = new ArrayList<>();
    for (String line : fragment.split("\n")) {
      if (!line.startsWith("@param ")) {
        continue;
      }
      String rest = line.substring("@param ".length()).trim();
      String name = rest.split("=")[0].trim();
      name = name.substring(name.lastIndexOf(' ') + 1);
      if (!args.contains(name + " =") && !args.contains(name + "=")) {
        missing.add(name);
      }
    }

    assertThat(missing).as("페이지가 넘기지 않은 파라미터는 화면에서 조용히 사라진다").isEmpty();
  }

  /**
   * 합계 줄. 일곱 열이 <b>모두</b> 더한 값이다.
   *
   * <p>연말정산·금투세 때 필요한 수는 대개 "그 해" 가 아니라 "다 합쳐서 얼마" 인데, 표가 해마다 한 줄씩만 적고 있어 눈으로 더해야 했다. 성과 표와 달리 여기는
   * 곱해서 이어야 하는 수익률도, 합계라는 것이 없는 기말 평가액도 없다 &mdash; 그래서 줄 이름이 '전체 기간' 이 아니라 '합계' 다.
   */
  @Test
  void 합계_줄에_일곱_열을_모두_더한다() {
    String html = render(rows());

    assertThat(html)
        .as("다 합쳐서 얼마인지가 없어 눈으로 더해야 했다")
        .contains("data-yearly-cost-total")
        .contains(MessageUtil.getMessage("stock.asset.growth.cost.sum.row"));

    String foot = html.substring(html.indexOf("data-yearly-cost-total"));
    assertThat(foot)
        .as("수수료 20,933 + 3,007 = 23,940 / 거래세 616,963 + 0 = 616,963")
        .contains("23,940")
        .contains("616,963")
        .as("배당 세전 26,164,415 + 11,219,044 = 37,383,459")
        .contains("37,383,459")
        .as("과세금액 9,172,513 + 10,578,571 = 19,751,084")
        .contains("19,751,084")
        .as("배당소득세 1,412,730 + 1,629,100 = 3,041,830")
        .contains("3,041,830")
        .as("배당 세후 24,751,685 + 9,589,944 = 34,341,629")
        .contains("34,341,629");
  }

  /** 합계 줄도 줄과 같은 규칙을 쓴다 - 실현손익에만 부호를 붙이고, 나간 돈에는 붙이지 않는다. */
  @Test
  void 합계_줄도_실현손익에만_부호를_붙인다() {
    String foot = render(rows()).substring(render(rows()).indexOf("data-yearly-cost-total"));

    assertThat(foot).contains("+&#8361;131,261,409");
    assertThat(foot)
        .as("나간 돈에 + 를 붙이면 번 돈처럼 읽힌다")
        .doesNotContain("+&#8361;23,940")
        .doesNotContain("+&#8361;3,041,830");
  }

  /**
   * 줄이 하나뿐이면 합계를 적지 않는다.
   *
   * <p>합계가 그 줄을 <b>그대로 되풀이할 뿐</b>이라 아무것도 더해 주지 않는다. 실측 2026-09-03: '올해' 를 고르면 이 표가 2026 한 줄인데 그 아래에
   * 같은 수를 한 번 더 적고 있었다.
   */
  @Test
  void 줄이_하나뿐이면_합계를_적지_않는다() {
    String html = render(List.of(rows().get(0)));

    assertThat(html).as("한 줄짜리 표를 그리지 못했다 - 검사가 무력해진다").contains("2026");
    assertThat(html)
        .as("합계가 그 줄을 그대로 되풀이할 뿐이다")
        .doesNotContain("data-yearly-cost-total")
        .doesNotContain(MessageUtil.getMessage("stock.asset.growth.cost.sum.row"));
  }
}
