package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

/** 시황 분석 최근 예측 쪽 나누기(사용자 요청 2026-11-15). */
class StockAnalysisRecentPageTest {

  private static final List<Integer> ROWS = IntStream.range(0, 45).boxed().toList();

  @Test
  void 한_쪽은_20_줄이고_마지막_쪽은_남은_줄만() {
    Page<Integer> first = StockAnalysisViewController.pageOf(ROWS, 1);
    Page<Integer> last = StockAnalysisViewController.pageOf(ROWS, 3);
    assertThat(first.getContent()).hasSize(20).startsWith(0);
    assertThat(first.getTotalPages()).isEqualTo(3);
    assertThat(last.getContent()).containsExactly(40, 41, 42, 43, 44);
  }

  @Test
  void 범위를_벗어난_쪽_번호는_가장_가까운_쪽() {
    assertThat(StockAnalysisViewController.pageOf(ROWS, 0).getNumber()).isZero();
    assertThat(StockAnalysisViewController.pageOf(ROWS, 99).getNumber()).isEqualTo(2);
    assertThat(StockAnalysisViewController.pageOf(List.of(), 1).getContent()).isEmpty();
  }

  private static net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem item(
      String symbol) {
    return new net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem(
        symbol, symbol, null, null, null, null, null, null);
  }

  @Test
  void 종목별_예측은_고른_종목_없으면_삼성전자_그것도_없으면_첫_대상() {
    var items = List.of(item("000660"), item("005930"));
    assertThat(StockAnalysisViewController.historySymbolOf("000660", items)).isEqualTo("000660");
    assertThat(StockAnalysisViewController.historySymbolOf("999999", items)).isEqualTo("005930");
    assertThat(StockAnalysisViewController.historySymbolOf(null, List.of(item("000660"))))
        .isEqualTo("000660");
    assertThat(StockAnalysisViewController.historySymbolOf(null, List.of())).isNull();
  }

  private static net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem item(
      String symbol, String name) {
    return new net.luversof.web.gate.stock.httpexchange.StockAnalysisClient.AnalysisItem(
        symbol, name, null, null, null, null, null, null);
  }

  @Test
  void 검색이_한_종목만_가리킬_때만_그_종목_기준() {
    var items = List.of(item("005930", "삼성전자"), item("005935", "삼성전자우"), item("000660", "SK하이닉스"));
    assertThat(StockAnalysisViewController.focusOf("하이닉스", items).symbol()).isEqualTo("000660");
    // 여러 종목에 맞으면 코드가 정확히 같은 것만, 그것도 없으면 전체 기준
    assertThat(StockAnalysisViewController.focusOf("005930", items).symbol()).isEqualTo("005930");
    assertThat(StockAnalysisViewController.focusOf("삼성전자", items)).isNull();
    assertThat(StockAnalysisViewController.focusOf("", items)).isNull();
  }
}
