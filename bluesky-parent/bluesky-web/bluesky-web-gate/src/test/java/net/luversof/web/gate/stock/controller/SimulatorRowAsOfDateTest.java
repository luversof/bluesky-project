package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 시뮬레이터는 행마다 기준일을 적어야 한다.
 *
 * <p>요약 카드는 "기준일 2026-08-19 ~ 2026-09-02" 처럼 <b>범위</b> 로만 적는다. 그 범위는 갱신이 밀린 것과 지급 시기가 다른 것을 구분해 주지
 * 못한다 &mdash; 실측 2026-09-11: 월중 지급 4 종목(476800·0018C0·498400·0094M0)이 08-19, 월말 지급 4
 * 종목(329200·0104P0·475720·472150)이 09-02 로, <b>여덟 종목 모두 자기 최신 지급일</b>이었다(지연 아님). 그런데 화면만 보면 2 주 뒤처진
 * 데이터가 섞인 것처럼 읽힌다.
 *
 * <p>{@code asOfDate} 는 스냅샷 응답에 이미 있으므로 행에 적기만 하면 된다.
 */
class SimulatorRowAsOfDateTest {

  private static final String TEMPLATE =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";

  private String read() throws IOException {
    return Files.readString(Path.of(TEMPLATE), StandardCharsets.UTF_8);
  }

  @Test
  void 행마다_기준일을_적는다() throws IOException {
    String template = read();

    assertThat(template).contains("data-row-as-of");
    assertThat(template).contains("row.asOfDate().toString()");
    assertThat(template).as("값이 없으면 빈 줄을 만들지 않는다").contains("@if(row.asOfDate() != null)");
  }

  @Test
  void 요약의_범위_표시는_그대로다() throws IOException {
    String template = read();

    assertThat(template).as("행 표시는 범위를 대체하는 것이 아니라 보완한다").contains("oldestAsOfDate.toString()");
    assertThat(template).contains("newestAsOfDate");
  }

  @Test
  void 행과_요약이_같은_라벨을_쓴다() throws IOException {
    String template = read();

    int uses = 0;
    int at = template.indexOf("stock.simulator.monthly.summary.as.of.date");
    while (at >= 0) {
      uses++;
      at = template.indexOf("stock.simulator.monthly.summary.as.of.date", at + 1);
    }
    assertThat(uses).as("요약 1 곳 + 행 1 곳").isEqualTo(2);
  }
}
