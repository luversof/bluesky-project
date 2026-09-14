package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 자료가 한 점도 없는 기간에서 도넛은 캔버스를 감추고 안내를 띄우는데 월별 막대는 빈 캔버스만 남겼다.
 *
 * <p>실측 2026-09-11(2013-01~03, 거래 0 건): 배당 화면 도넛 자리에는 "해당 기간에 배당 내역이 없습니다" 가 떴지만 바로 옆 <b>"월별 배당금" 은
 * 280px 빈 캔버스</b>였고, 매매 화면 "월별 매매 금액" 도 <b>220px 빈 캔버스</b>였다 &mdash; 같은 화면 안에서 빈 상태 규칙이 카드마다 달랐다.
 *
 * <p>규칙은 `StockCharts.renderChartEmptyNote` 하나만 쓴다(두 화면이 다르게 굴지 않도록).
 */
class ChartEmptyNoteTest {

  private static final Path CHARTS_TS = Path.of("src/main/frontend/src/stock-charts.ts");
  private static final Path DIVIDEND_TS = Path.of("src/main/frontend/src/stock/dividendHistory.ts");
  private static final Path BUILT_CHARTS = Path.of("src/main/resources/static/js/stock-charts.js");
  private static final Path BUILT_DIVIDEND =
      Path.of("src/main/resources/static/js/stock/dividendHistory.js");
  private static final Path LAYOUT = Path.of("src/main/jte/_layout/defaultLayout.jte");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 규칙은_한_곳에만_있다() throws IOException {
    String charts = read(CHARTS_TS);
    assertThat(charts).contains("function renderChartEmptyNote");
    assertThat(charts).contains("StockCharts.renderChartEmptyNote = renderChartEmptyNote;");
    // 감추기만 하고 안내를 안 넣으면 카드가 텅 빈다.
    int at = charts.indexOf("function renderChartEmptyNote");
    String body = charts.substring(at, charts.indexOf("StockCharts.renderChartEmptyNote", at));
    assertThat(body).contains("createElement");
    assertThat(body).contains("textContent");
    assertThat(body).as("자료가 생기면 안내를 걷어내야 한다").contains("removeChild");
  }

  @Test
  void 두_월별_막대가_모두_그_규칙을_쓴다() throws IOException {
    assertThat(read(CHARTS_TS)).as("매매 월별 막대").contains("renderChartEmptyNote(");
    assertThat(read(DIVIDEND_TS))
        .as("배당 월별 막대")
        .contains("renderChartEmptyNote(canvasId, monthlyPointCount === 0, noPeriodHistoryLabel)");
    // StockCharts 는 ensureStockCharts 콜백 안에서만 보장된다 - 그 밖에서 부르면 조용히 건너뛰어
    // 안내가 안 뜬다(실측 2026-09-11: 배당 월별 막대가 그 바람에 빈 캔버스 그대로였다).
    String dividend = read(DIVIDEND_TS);
    int noteAt = dividend.indexOf("renderChartEmptyNote(canvasId, monthlyPointCount");
    int ensureAt = dividend.lastIndexOf("win.ensureStockCharts", noteAt);
    int createAt = dividend.indexOf("createChart(canvasId", ensureAt);
    assertThat(ensureAt).isGreaterThan(0);
    assertThat(noteAt).as("ensureStockCharts 콜백 안에서 불러야 한다").isGreaterThan(ensureAt);
    assertThat(noteAt).as("차트를 만들기 전에 판정해야 한다").isLessThan(createAt);
    // 라벨은 자료가 0건이어도 기간에서 만들어진다(실측: 라벨 1 · 점 0) - 라벨 수로 판정하면 안내가 안 뜬다.
    assertThat(read(DIVIDEND_TS))
        .doesNotContain("renderChartEmptyNote(canvasId, m.labels.length === 0");
  }

  @Test
  void 문구는_번들과_app_config_를_거쳐_온다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(read(Path.of("src/main/resources/" + name)))
          .contains("stock.trade.chart.message.no.trade.data");
    }
    // 속성 이름 뒤 ="..." 까지 못박는다 - 이름 일부만 보면 오타 난 속성도 통과한다.
    assertThat(read(LAYOUT))
        .as("app-config 에 안 실리면 화면에는 영어 기본값이 나간다")
        .contains(
            "data-stock-chart-message-no-trade-data="
                + (char) 34
                + "${MessageUtil.getMessage("
                + (char) 34
                + "stock.trade.chart.message.no.trade.data"
                + (char) 34
                + ")}");
  }

  @Test
  void 빈_안내는_대비를_지킨다() throws IOException {
    // 실측 2026-09-11(빈 기간): opacity-40 은 라이트에서 2.61:1 이었다(axe serious, 12px 는 4.5:1 필요).
    // 도넛 쪽 안내 두 곳도 같은 값이라 함께 고쳤다 - 빈 기간을 감사한 적이 없어 여태 안 보였다.
    for (java.nio.file.Path path : new java.nio.file.Path[] {CHARTS_TS, DIVIDEND_TS}) {
      String source = read(path);
      int at = 0;
      while (true) {
        at = source.indexOf("text-xs", at);
        if (at < 0) break;
        String className = source.substring(at, Math.min(source.length(), at + 90));
        assertThat(className).as(path + " 의 안내 글자").doesNotContain("opacity-40");
        at += 7;
      }
      assertThat(source).contains("text-xs text-base-content/70");
    }
  }

  @Test
  void 빌드_산출물에도_들어_있다() throws IOException {
    assertThat(read(BUILT_CHARTS)).contains("renderChartEmptyNote");
    assertThat(read(BUILT_DIVIDEND)).contains("renderChartEmptyNote");
  }
}
