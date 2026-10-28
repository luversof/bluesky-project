package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.ui.ExtendedModelMap;

import net.luversof.web.gate.stock.dto.response.DividendResponse;
import net.luversof.web.gate.stock.dto.response.TradeProfitTimeSeriesPoint;
import net.luversof.web.gate.stock.dto.response.TradeResponse;

/**
 * 종목 상세 · 계좌 상세가 함께 쓰는 내역 정렬과 기간 필터 바 · 기간 배지 모델(StockDetailViewController 의 정적 도우미).
 *
 * <p>2026-10-22 까지 두 핸들러가 같은 덩어리를 한 벌씩 들고 있었고, 시험(DetailDataFirstDateTest ·
 * DetailEmptyWidenCtaTest)은 소스에서 그 줄이 "두 번" 나오는지를 셌다. 이제 규칙은 여기서 값으로 보고, 소스 시험은 두 핸들러가 도우미를 부르는지만
 * 본다.
 */
class DetailRangeAttributesTest {

  private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

  /** 서울 자정(그날의 시작). */
  private static Instant day(String date) {
    return LocalDate.parse(date).atStartOfDay(SEOUL).toInstant();
  }

  private static TradeProfitTimeSeriesPoint point(String date) {
    return new TradeProfitTimeSeriesPoint(day(date), null, null, 0, 0, 0, null, null, null, null);
  }

  private static TradeResponse trade(String date) {
    return new TradeResponse(
        null,
        null,
        null,
        "t" + date,
        null,
        1,
        null,
        null,
        null,
        null,
        null,
        date == null ? null : day(date));
  }

  private static DividendResponse dividend(String date, String net) {
    return new DividendResponse(
        null,
        null,
        null,
        "d" + date,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        net == null ? null : new BigDecimal(net),
        null,
        date == null ? null : day(date));
  }

  @Test
  void 내역은_최근_날짜부터이고_날짜가_없는_줄은_뒤로_간다() {
    List<TradeResponse> trades =
        StockDetailViewController.tradesByDate(
            Arrays.asList(trade("2026-03-02"), trade(null), trade("2026-01-05")));
    assertThat(trades)
        .extracting(TradeResponse::stockItemName)
        .containsExactly("t2026-03-02", "t2026-01-05", "tnull");
    List<DividendResponse> dividends =
        StockDetailViewController.dividendsByPayDate(
            Arrays.asList(
                dividend(null, "1"), dividend("2026-05-01", "2"), dividend("2026-02-01", "3")));
    assertThat(dividends)
        .extracting(DividendResponse::stockItemName)
        .containsExactly("d2026-05-01", "d2026-02-01", "dnull");
    assertThat(StockDetailViewController.tradesByDate(null)).isEmpty();
    assertThat(StockDetailViewController.dividendsByPayDate(null)).isEmpty();
  }

  @Test
  void 배당_합계는_세후_금액이고_값이_없으면_0_으로_센다() {
    assertThat(
            StockDetailViewController.netDividendTotal(
                List.of(
                    dividend("2026-01-01", "1500"),
                    dividend("2026-02-01", null),
                    dividend("2026-03-01", "250"))))
        .isEqualByComparingTo("1750");
    assertThat(StockDetailViewController.netDividendTotal(List.of())).isEqualByComparingTo("0");
  }

  /** 시계열은 평가액이 잡히는 날부터라 첫 거래보다 늦게 시작할 수 있다 - 배지 시작은 최초 일자로 당긴다. 끝은 내역의 마지막 날로 늘린다. */
  @Test
  void 배지는_최초일과_내역의_마지막_날까지_덮는다() {
    ExtendedModelMap model = new ExtendedModelMap();
    StockDetailViewController.addRangeFilterAttributes(
        model,
        day("2026-01-01"),
        day("2026-07-01"),
        "Asia/Seoul",
        null,
        SEOUL,
        List.of(point("2026-02-10"), point("2026-04-30")),
        List.of(trade("2026-05-20")),
        List.of(dividend("2026-06-15", "100")),
        "2026-01-19");
    assertThat(model.get("filterStartLocal")).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(model.get("filterEndLocal")).as("끝은 포함 마지막 날").isEqualTo(LocalDate.of(2026, 6, 30));
    assertThat(model.get("filterStartInstant")).isEqualTo(day("2026-01-01"));
    assertThat(model.get("filterEndInstant")).isEqualTo(day("2026-07-01"));
    assertThat(model.get("filterTimeZone")).isEqualTo("Asia/Seoul");
    assertThat(model.get("coveredStartLocal")).isEqualTo(LocalDate.of(2026, 1, 19));
    assertThat(model.get("coveredEndLocal")).isEqualTo(LocalDate.of(2026, 6, 15));
    assertThat(model.get("dataFirstDate")).isEqualTo("2026-01-19");
    assertThat(model.containsAttribute("filterRangeMode")).isTrue();
    assertThat(model.get("filterRangeMode")).isNull();
    assertThat(model.get("rangeMode")).as("모드가 없으면 빈 글자(빈 기간 안내가 판단)").isEqualTo("");
  }

  @Test
  void 시계열이_없으면_최초일과_내역으로_배지를_정하고_전체_모드를_넘긴다() {
    ExtendedModelMap model = new ExtendedModelMap();
    StockDetailViewController.addRangeFilterAttributes(
        model,
        null,
        null,
        null,
        "all",
        SEOUL,
        List.of(),
        List.of(trade("2026-03-03")),
        List.of(),
        "");
    assertThat(model.get("filterStartLocal")).isNull();
    assertThat(model.get("filterEndLocal")).isNull();
    assertThat(model.get("coveredStartLocal")).as("최초일을 못 구하면 시계열 시작(없음)").isNull();
    assertThat(model.get("coveredEndLocal")).isEqualTo(LocalDate.of(2026, 3, 3));
    assertThat(model.get("filterRangeMode")).isEqualTo("all");
    assertThat(model.get("rangeMode")).isEqualTo("all");
  }

  @Test
  void 두_상세_핸들러가_도우미를_부른다() throws IOException {
    String source =
        Files.readString(
            Path.of(
                "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"),
            StandardCharsets.UTF_8);
    for (String helper :
        List.of(
            "addRangeFilterAttributes(",
            "tradesByDate(",
            "dividendsByPayDate(",
            "netDividendTotal(")) {
      assertThat(source.split(java.util.regex.Pattern.quote(helper), -1))
          .as(helper + " - 정의 하나 + /item · /account")
          .hasSize(4);
    }
  }
}
