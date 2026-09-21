package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.StockItem;
import net.luversof.web.gate.stock.service.MonthlyDividendLinkRegisterService;
import net.luversof.web.gate.stock.service.MonthlyDividendLinkRegisterService.StockItemAction;
import net.luversof.web.gate.stock.util.MonthlyDividendSourceMetaParser.SourceMeta;
import tools.jackson.databind.json.JsonMapper;

/**
 * 링크만으로 월배당 기준 데이터를 등록한다(사용자 요청 2026-09-21).
 *
 * <p>네 운용사 모두 링크에서 종목코드 · 이름을 얻을 수 있음을 실측하고(2026-09-21) 그 모양을 여기 고정한다. 바깥 사이트가 바뀌면 여기가 아니라 라이브 탐침이
 * 먼저 알려 주지만, 적어도 <b>우리가 무엇을 기대하고 있는지</b>는 남는다.
 */
class MonthlyDividendLinkRegisterTest {

  private final MonthlyDividendSourceMetaParser parser =
      new MonthlyDividendSourceMetaParser(JsonMapper.builder().build());

  @Test
  void KODEX_는_JSON_LD_에서_코드와_이름을_읽는다() {
    String html =
        """
				<script type="application/ld+json">
				{"@type":"Product","name":"KODEX 한국부동산리츠인프라","alternateName":"KODEX KOREA REITs Infra",
				 "identifier":"476800","url":"https://www.samsungfund.com/etf/product/view.do?id=2ETFM4"}
				</script>
				""";

    SourceMeta meta = parser.fromKodexHtml(html);
    assertThat(meta.symbol()).isEqualTo("476800");
    assertThat(meta.name()).isEqualTo("KODEX 한국부동산리츠인프라");
  }

  @Test
  void PLUS_는_상품코드_칸과_제목에서_읽는다() {
    String html =
        """
				<title>PLUS 고배당주위클리고정커버드콜 | PLUS ETF</title>
				<h2 class="summary__title">PLUS 고배당주위클리고정커버드콜</h2>
				<div class="summary__product-code">0018C0</div>
				""";

    SourceMeta meta = parser.fromPlusHtml(html);
    assertThat(meta.symbol()).isEqualTo("0018C0");
    assertThat(meta.name()).as("제목의 사이트 이름은 떼고 종목 이름만").isEqualTo("PLUS 고배당주위클리고정커버드콜");
  }

  /** TIGER 는 주소에 표준코드(ISIN)가 실려 있다: KR7 <b>329200</b> 000. */
  @Test
  void TIGER_는_주소의_표준코드에서_종목코드를_얻는다() {
    URI uri =
        URI.create(
            "https://investments.miraeasset.com/tigeretf/ko/product/search/detail/index.do"
                + "?ksdFund=KR7329200000");
    String html = "<title> TIGER 리츠부동산인프라 | ETF 상품 | 미래에셋 TIGER ETF</title>";

    SourceMeta meta = parser.fromTiger(uri, html);
    assertThat(meta.symbol()).isEqualTo("329200");
    assertThat(meta.name()).isEqualTo("TIGER 리츠부동산인프라");
  }

  @Test
  void RISE_는_header_API_에서_읽는다() {
    String json =
        """
				{"fund_cd":"44J2","name":"RISE 코리아밸류업위클리고정커버드콜","krx_cd":"0094M0",
				 "category1":"국내주식","seo_title":"RISE 코리아밸류업 커버드콜 ETF | 월배당 고배당 투자"}
				""";

    SourceMeta meta = parser.fromRiseHeaderJson(json);
    assertThat(meta.symbol())
        .as("seo_title 이 아니라 krx_cd · name 을 쓴다(제목은 마케팅 문구라 종목명과 다르다)")
        .isEqualTo("0094M0");
    assertThat(meta.name()).isEqualTo("RISE 코리아밸류업위클리고정커버드콜");
  }

  @Test
  void 코드나_이름을_못_읽으면_등록하지_않는다() {
    assertThatThrownBy(() -> parser.fromKodexHtml("<html>아무것도 없음</html>"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parser.fromPlusHtml("<title>PLUS ETF</title>"))
        .as("코드 칸이 없으면 이름만으로 등록하지 않는다")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void 여러_줄에서_빈_줄과_겹치는_주소를_걷어낸다() {
    String raw =
        "https://a.test/1\n" + "  \n" + "https://a.test/2\r\n" + "  https://a.test/1  \n" + "\n";

    assertThat(MonthlyDividendLinkRegisterService.splitLinks(raw))
        .containsExactly("https://a.test/1", "https://a.test/2");
    assertThat(MonthlyDividendLinkRegisterService.splitLinks(null)).isEmpty();
    assertThat(MonthlyDividendLinkRegisterService.splitLinks("   ")).isEmpty();
  }

  @Test
  void 종목이_없으면_만들고_태그만_없으면_태그를_더한다() {
    assertThat(MonthlyDividendLinkRegisterService.decideStockItemAction(null))
        .isEqualTo(StockItemAction.CREATE);
    assertThat(
            MonthlyDividendLinkRegisterService.decideStockItemAction(
                new StockItem(UUID.randomUUID(), "476800", "KODEX", "KRX", List.of("리츠"))))
        .as("이미 있는 종목의 이름 · 시장은 건드리지 않고 태그만 더한다")
        .isEqualTo(StockItemAction.ADD_TAG);
    assertThat(
            MonthlyDividendLinkRegisterService.decideStockItemAction(
                new StockItem(UUID.randomUUID(), "476800", "KODEX", "KRX", List.of("월배당"))))
        .isEqualTo(StockItemAction.KEEP);
  }

  /**
   * 출처를 부르는 클라이언트는 앱이 만들어 준 것을 그대로 쓴다.
   *
   * <p>타임아웃을 주겠다고 {@code requestFactory(...)} 로 갈아 끼웠더니 localdev 의 "모든 인증서 신뢰" 팩터리가 사라져 삼성자산운용 인증서를
   * 이 JVM 이 못 믿었다 &mdash; 사용자가 링크를 넣자 PKIX path building failed 로 등록이 실패했다 (실측 2026-09-21, 사용자 보고).
   * 타임아웃은 spring.http.clients.* 로 이미 걸린다.
   */
  @Test
  void 출처_호출은_앱이_준_클라이언트를_그대로_쓴다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
            java.nio.file.Path.of(
                "src/main/java/net/luversof/web/gate/stock/util/"
                    + "MonthlyDividendPayoutSourceImportService.java"),
            java.nio.charset.StandardCharsets.UTF_8);
    // 주석은 이 판단에 넣지 않는다 - 까닭을 적어 둔 주석에 requestFactory 가 나온다.
    // 클라이언트를 만드는 생성자 구간만 본다.
    int from = source.indexOf("this.restClient =");
    assertThat(from).as("전제: 생성자에서 클라이언트를 만든다").isGreaterThanOrEqualTo(0);
    String building = source.substring(from, source.indexOf(".build();", from));

    assertThat(building)
        .as("요청 팩터리를 갈아 끼우면 localdev 의 인증서 신뢰 설정이 사라진다")
        .doesNotContain("requestFactory(");
    assertThat(building)
        .as("타임아웃을 여기서 따로 만들지 않는다 - spring.http.clients.* 가 건다")
        .doesNotContain("HttpClientSettings");

    String properties =
        java.nio.file.Files.readString(
            java.nio.file.Path.of("src/main/resources/application.properties"),
            java.nio.charset.StandardCharsets.UTF_8);
    assertThat(properties.lines().map(String::trim).toList())
        .as("그 타임아웃이 실제로 있어야 이 판단이 성립한다")
        .contains("spring.http.clients.connect-timeout=3s", "spring.http.clients.read-timeout=10s");
  }

  /** 지급 시기 자동 판정 - 등록된 12 종목 258 건에 대 보니 12/12 맞았다(2026-09-21). */
  @Test
  void 지급_시기를_기준일로_가린다() {
    assertThat(
            MonthlyDividendPayoutWindowGuesser.guess(
                List.of(
                    LocalDate.of(2026, 9, 15),
                    LocalDate.of(2026, 8, 14),
                    LocalDate.of(2026, 7, 15))))
        .isEqualTo(MonthlyDividendPayoutWindowGuesser.MID_MONTH);
    assertThat(
            MonthlyDividendPayoutWindowGuesser.guess(
                List.of(
                    LocalDate.of(2026, 8, 31),
                    LocalDate.of(2026, 7, 31),
                    LocalDate.of(2026, 2, 27))))
        .as("달마다 말일이 다르니 '말일에서 며칠 전' 으로 센다")
        .isEqualTo(MonthlyDividendPayoutWindowGuesser.MONTH_END);
    assertThat(
            MonthlyDividendPayoutWindowGuesser.guess(
                List.of(LocalDate.of(2026, 2, 24), LocalDate.of(2027, 2, 24))))
        .as("2 월 24 일은 말일에서 나흘 전이라 월말이다 - '26 일 이후' 같은 달력일 기준이면 놓친다")
        .isEqualTo(MonthlyDividendPayoutWindowGuesser.MONTH_END);
    assertThat(
            MonthlyDividendPayoutWindowGuesser.guess(
                List.of(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 8, 31))))
        .as("반반이면 억지로 고르지 않는다")
        .isEqualTo(MonthlyDividendPayoutWindowGuesser.UNKNOWN);
    assertThat(MonthlyDividendPayoutWindowGuesser.guess(List.of()))
        .isEqualTo(MonthlyDividendPayoutWindowGuesser.UNKNOWN);
    assertThat(
            MonthlyDividendPayoutWindowGuesser.guess(
                List.of(LocalDate.of(2026, 9, 2), LocalDate.of(2026, 8, 2))))
        .as("이력이 2 건뿐이어도 한 방향이면 정한다(실측: 신규 2 종목)")
        .isEqualTo(MonthlyDividendPayoutWindowGuesser.MID_MONTH);
  }
}
