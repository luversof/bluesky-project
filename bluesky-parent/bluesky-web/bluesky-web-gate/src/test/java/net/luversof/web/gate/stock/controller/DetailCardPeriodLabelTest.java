package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

/**
 * 상세 화면의 지표 카드는 자기가 어느 축의 값인지 이름으로 말해야 한다.
 *
 * <p>카드가 한 줄에 섞여 있다. 종목 상세 여덟 장 중 셋(실현 손익·기간 매수·기간 배당)은 <b>고른 기간</b>의 값이고, 나머지 다섯(수량·평균 단가·현재가·평가
 * 금액·평가 손익)은 <b>지금 보유분</b>이다. 그런데 두 라벨이 축을 잘못 말했다.
 *
 * <p>실측 2026-09-11(삼성전자, 전체 기간): "매수 원가 466,248,078" 옆의 평가 손익이 996,563,421 인데 1,359,088,500 −
 * 466,248,078 = 892,840,422 이라 검산이 안 됐다 &mdash; 평가 손익은 보유원가 362,525,079(= 5,043주 × 71,887) 기준이고, 그
 * 카드는 사실 '기간 내 매수 금액' 이었다. 기간을 올해로 좁히면 "매수 원가 0원" 옆에 평가 손익 +996,563,421 이 그대로 섰다. "누적 배당" 도 1개월
 * 기간에서 1,595,632 를 누적이라고 적었다(전 기간은 35,340,449).
 *
 * <p>값이 아니라 이름을 고쳤다 &mdash; 기간 값이라는 정보를 잃지 않는다. 대시보드의 '누적 배당' 은 실제로 전 기간이라 그대로 둔다(실측: 전체와 올해가 같은
 * 65,652,134).
 */
class DetailCardPeriodLabelTest {

  private static final String ITEM = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String ACCOUNT = "src/main/jte/stock/htmx/accountDetailContent.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  private Properties messages(String path) throws IOException {
    Properties props = new Properties();
    try (var reader = Files.newBufferedReader(Path.of(path), StandardCharsets.UTF_8)) {
      props.load(reader);
    }
    return props;
  }

  @Test
  void 기간_배당은_누적이라고_적지_않는다() throws IOException {
    assertThat(read(ITEM))
        .as("기간이 걸리는 값에 '누적' 라벨을 쓰면 1개월 배당이 누적으로 읽힌다")
        .doesNotContain("stock.summary.label.cumulative.dividend");
    assertThat(read(ACCOUNT)).doesNotContain("stock.summary.label.cumulative.dividend");

    assertThat(read(ITEM)).contains("stock.item.detail.period.dividend");
    assertThat(read(ACCOUNT)).contains("stock.item.detail.period.dividend");
  }

  @Test
  void 기간_매수는_보유원가로_읽히지_않는다() throws IOException {
    Properties ko = messages("src/main/resources/uiMessage_ko.properties");
    Properties en = messages("src/main/resources/uiMessage.properties");

    // 이 값은 api-stock 의 totalBuyCost = '기간 내 매수원가' 다(StockDetailViewController 주석).
    assertThat(ko.getProperty("stock.item.detail.principal"))
        .as("'원가' 라고 적으면 옆의 평가 손익과 같은 기준으로 읽힌다")
        .doesNotContain("원가")
        .isEqualTo("기간 매수");
    assertThat(en.getProperty("stock.item.detail.principal"))
        .doesNotContain("Buy Cost")
        .isEqualTo("Bought (period)");
  }

  @Test
  void 두_말이_모두_있다() throws IOException {
    Properties ko = messages("src/main/resources/uiMessage_ko.properties");
    Properties en = messages("src/main/resources/uiMessage.properties");

    assertThat(ko.getProperty("stock.item.detail.period.dividend")).isEqualTo("기간 배당");
    assertThat(en.getProperty("stock.item.detail.period.dividend")).isEqualTo("Dividends (period)");
  }

  @Test
  void 대시보드의_누적_배당은_그대로다() throws IOException {
    assertThat(read("src/main/jte/stock/htmx/fragments/summary.jte"))
        .as("대시보드 값은 실제로 전 기간이라(전체=올해=65,652,134) '누적' 이 맞다")
        .contains("stock.summary.label.cumulative.dividend");
  }
}
