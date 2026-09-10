package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.response.StockPriceHistoryPoint;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.util.ChartSeriesJs;

/**
 * 상세 화면 차트 시리즈는 점마다 push 하는 줄이 아니라 배열 리터럴 한 줄로 나간다.
 *
 * <p>실측 2026-09-10(qa/item-requests.cjs): 종목 상세 조각 239 KB 중 대부분이 주가 이력 1,599일을 {@code @for} 로 점마다
 * 들여쓴 {@code series.labels.push(...)} 세 줄(약 120 바이트/점)로 찍은 몫이었다(api-stock 원본 71.8 KB).
 */
class ChartSeriesJsTest {

  @Test
  void 주가_시리즈는_배열_리터럴이고_값은_예전_출력과_같다() {
    List<StockPriceHistoryPoint> pts =
        List.of(
            new StockPriceHistoryPoint(LocalDate.of(2026, 9, 8), new BigDecimal("269500.4")),
            new StockPriceHistoryPoint(LocalDate.of(2026, 9, 9), null),
            new StockPriceHistoryPoint(null, new BigDecimal("270000")));
    String js = ChartSeriesJs.priceSeries(pts, new BigDecimal("71886.79"));
    assertThat(js)
        .isEqualTo(
            "{labels:[\"2026-09-08\",\"2026-09-09\",\"\"],value:[269500,0,270000],cost:new Array(3).fill(71887),buyCount:[],dailyRealized:[]}");
    assertThat(ChartSeriesJs.priceSeries(null, null))
        .isEqualTo("{labels:[],value:[],cost:new Array(0).fill(0),buyCount:[],dailyRealized:[]}");
  }

  @Test
  void 보유_추이_시리즈는_다섯_배열을_같은_길이로_낸다() {
    DateTimeFormatter f =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("Asia/Seoul"));
    TradeProfitTimeSeriesPoint p1 =
        new TradeProfitTimeSeriesPoint(
            Instant.parse("2026-09-09T15:00:00Z"),
            null,
            new BigDecimal("1500.5"),
            2,
            1,
            10,
            new BigDecimal("1234567.6"),
            new BigDecimal("1000000"),
            null,
            null);
    TradeProfitTimeSeriesPoint p2 =
        new TradeProfitTimeSeriesPoint(null, null, null, 0, 0, 0, null, null, null, null);
    String js = ChartSeriesJs.holdingsSeries(List.of(p1, p2), f);
    assertThat(js)
        .isEqualTo(
            "{labels:[\"2026-09-10\",\"\"],value:[1234568,0],cost:[1000000,0],buyCount:[1,0],dailyRealized:[1501,0]}");
  }

  @Test
  void 라벨은_스크립트를_깨지_못한다() {
    assertThat(ChartSeriesJs.jsString("a\"b\\c</script>\n"))
        .isEqualTo("\"a\\\"b\\\\c\\u003C/script>\\n\"");
  }

  @Test
  void 점당_출력은_30바이트_아래다() {
    List<StockPriceHistoryPoint> pts = new ArrayList<>();
    for (int i = 0; i < 1599; i++)
      pts.add(
          new StockPriceHistoryPoint(
              LocalDate.of(2020, 1, 1).plusDays(i), new BigDecimal(70000 + i)));
    String js = ChartSeriesJs.priceSeries(pts, new BigDecimal("71887"));
    assertThat(js.length() / 1599).as("점당 바이트(예전 템플릿은 약 120)").isLessThan(30);
  }

  @Test
  void 상세_템플릿에_점마다_push_하는_루프가_남아_있지_않다() throws IOException {
    Pattern perPoint = Pattern.compile("series\\.[a-zA-Z]+\\.push\\(");
    List<String> offenders = new ArrayList<>();
    int compact = 0;
    try (Stream<Path> walk = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String html = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = perPoint.matcher(html);
        while (m.find()) offenders.add(p.getFileName() + ": " + m.group());
        Matcher c = Pattern.compile("var series = \\$unsafe\\{ChartSeriesJs\\.").matcher(html);
        while (c.find()) compact++;
      }
    }
    assertThat(compact).as("배열 리터럴 시리즈를 찾지 못했다").isGreaterThanOrEqualTo(3);
    assertThat(offenders).as("점마다 들여쓴 push 는 조각 크기를 6배로 키운다").isEmpty();
  }
}
