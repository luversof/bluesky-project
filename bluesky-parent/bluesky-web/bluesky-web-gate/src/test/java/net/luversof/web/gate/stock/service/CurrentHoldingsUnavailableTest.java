package net.luversof.web.gate.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.TradeProfit;
import net.luversof.web.gate.stock.httpexchange.TradeProfitClient;

/**
 * 원장 조회 실패를 "보유 없음" 과 구분한다.
 *
 * <p>월배당 표와 배당 캘린더는 <b>어긋나는 줄에만</b> 표시를 단다. 그래서 조회가 실패해 빈 맵이 오면 표시가 통째로 사라지고, 표시가 없는 화면은 "원장과 같다" 로
 * 읽힌다 - 실측 2026-09-12: 월배당 8 줄 중 3 줄이 "현재 N" 표시를 달고 있었고(4,915 / 11,281 / 4,367), 캘린더도 어긋난 줄 수가 0 이면
 * 안내 자체가 사라진다.
 *
 * <p>실패는 조용하지 않다 - {@code unavailable} 로 표시해 화면이 그 사실을 말하게 한다.
 */
class CurrentHoldingsUnavailableTest {

  private static final UUID ITEM = UUID.randomUUID();

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  /** api-stock 이 죽은 상태 - 원격 호출이 예외를 던진다. */
  private MonthlyDividendReferenceSupport supportThatFails() {
    TradeProfitClient client = org.mockito.Mockito.mock(TradeProfitClient.class);
    org.mockito.Mockito.when(client.calculateProfit(org.mockito.ArgumentMatchers.any()))
        .thenThrow(new IllegalStateException("api-stock down"));
    MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();
    support.setTradeProfitClient(client);
    return support;
  }

  private MonthlyDividendReferenceSupport supportThatAnswers(List<TradeProfit> rows) {
    TradeProfitClient client = org.mockito.Mockito.mock(TradeProfitClient.class);
    org.mockito.Mockito.when(client.calculateProfit(org.mockito.ArgumentMatchers.any()))
        .thenReturn(rows);
    MonthlyDividendReferenceSupport support = new MonthlyDividendReferenceSupport();
    support.setTradeProfitClient(client);
    return support;
  }

  /** 수량과 평단가만 있으면 되는 최소 행. */
  private TradeProfit holding(int quantity) {
    java.math.BigDecimal price = new java.math.BigDecimal("1200");
    return new TradeProfit(
        ITEM,
        "이름",
        UUID.randomUUID(),
        "계좌",
        price,
        price,
        0,
        null,
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        quantity,
        price,
        null,
        null,
        null,
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        price,
        java.math.BigDecimal.ZERO,
        price,
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        java.math.BigDecimal.ZERO,
        java.time.LocalDate.of(2026, 9, 9));
  }

  @Test
  void 조회에_실패하면_실패라고_말한다() {
    var holdings = supportThatFails().loadCurrentHoldings(UUID.randomUUID());

    assertThat(holdings.unavailable()).as("실패를 감추면 화면이 '원장과 같다' 로 읽힌다").isTrue();
    assertThat(holdings.quantities()).as("없는 값을 지어내지도 않는다").isEmpty();
  }

  @Test
  void 조회에_성공하면_실패가_아니다() {
    var holdings =
        supportThatAnswers(List.of(holding(4915))).loadCurrentHoldings(UUID.randomUUID());

    assertThat(holdings.unavailable()).isFalse();
    assertThat(holdings.quantities()).containsEntry(ITEM, 4915);
  }

  /** 진짜로 한 줄도 없는 경우까지 실패로 부르면, 원장이 정상인데도 매번 경고가 뜬다. */
  @Test
  void 원장이_비어_있는_것은_실패가_아니다() {
    var holdings = supportThatAnswers(List.of()).loadCurrentHoldings(UUID.randomUUID());

    assertThat(holdings.unavailable()).isFalse();
    assertThat(holdings.quantities()).isEmpty();
  }

  /** 표시를 만들어도 화면이 안 보면 소용이 없다 - 두 화면 모두 이 값을 보고 안내를 낸다. */
  @Test
  void 두_화면이_그_표시를_본다() throws IOException {
    String key = "MessageUtil.getMessage(\"stock.holdings.ledger.unavailable\")";

    String simulator = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");
    assertThat(simulator).contains("@param boolean monthlyDividendCurrentHoldingsUnavailable");
    assertThat(simulator).contains("@if(monthlyDividendCurrentHoldingsUnavailable)");
    assertThat(simulator).as("시뮬레이터가 그 문구를 낸다").contains(key);

    String dividend = read("src/main/jte/stock/dividend.jte");
    assertThat(dividend).contains("@param boolean calendarCurrentQuantityUnavailable");
    assertThat(dividend).contains("@if(calendarCurrentQuantityUnavailable)");
    assertThat(dividend).as("배당 캘린더가 그 문구를 낸다").contains(key);
  }

  /** 안내 문구가 두 로케일 모두에 있어야 한다(하나가 비면 키 이름이 그대로 화면에 나간다). */
  @Test
  void 문구가_두_로케일에_있다() throws IOException {
    for (String path :
        new String[] {
          "src/main/resources/uiMessage.properties", "src/main/resources/uiMessage_ko.properties"
        }) {
      String text = read(path);
      int at = text.indexOf("stock.holdings.ledger.unavailable");
      assertThat(at).as(path + " 에 키가 있다").isGreaterThanOrEqualTo(0);
      String value = text.substring(text.indexOf("=", at) + 1, text.indexOf((char) 10, at)).trim();
      assertThat(value).as(path + " 값이 비어 있지 않다").isNotEmpty();
    }
  }
}
