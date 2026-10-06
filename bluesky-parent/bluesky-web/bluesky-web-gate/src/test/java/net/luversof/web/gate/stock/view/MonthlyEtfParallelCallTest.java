package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 ETF 화면은 서로 의존 없는 원격 호출 둘(카탈로그 · 원장 보유)을 먼저 다 던지고 나서 받는다. 월배당 스냅샷은 2026-10-02 부터 부르지 않는다(이번 적립
 * 배지를 보유 여부와 상관없이 카탈로그 전체에서 고른다).
 *
 * <p>실측 2026-09-23: 예전에는 차례로 불러 카탈로그가 끝난 뒤(+101ms)에야 둘째 호출이 나갔다. 하나라도 던지기 전에 받으면(join) 그만큼 다시 줄을 선다
 * &mdash; 그래서 "세 supply 가 모두 첫 join 보다 앞" 을 본다. 스냅샷 실패는 예전처럼 배지만 비우고 목록은 살려야 하므로 그 join 이 try 안에
 * 있는지도 본다. 공백을 전부 지워 비교한다(서식기 · 줄 나눔과 무관).
 */
class MonthlyEtfParallelCallTest {

  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockMonthlyEtfViewController.java");

  private static String squash(String text) {
    return text.replaceAll("[\\s]+", "");
  }

  private static int count(String text, String needle) {
    int found = 0;
    for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + needle.length())) {
      found++;
    }
    return found;
  }

  @Test
  void 두_호출을_다_던진_뒤_받는다() throws IOException {
    String source = squash(Files.readString(CONTROLLER, StandardCharsets.UTF_8));
    String catalog =
        squash(
            "stockAsync.supply(() -> monthlyDividendCatalogClient.findCatalog(new LinkedMultiValueMap<>()))");
    String holdings =
        squash(
            "stockAsync.supply(() -> monthlyDividendReferenceSupport.loadCurrentHoldings(userId))");

    assertThat(count(source, catalog)).as("카탈로그를 따로 던진다").isEqualTo(1);
    assertThat(count(source, holdings)).as("원장 보유를 따로 던진다").isEqualTo(1);

    int firstJoin = source.indexOf("StockAsyncSupport.join(");
    assertThat(firstJoin).as("받는 자리가 없다").isGreaterThan(0);
    assertThat(source.indexOf(catalog)).as("카탈로그를 받기 전에 던진다").isLessThan(firstJoin);
    assertThat(source.indexOf(holdings)).as("원장 보유를 첫 join 전에 던진다").isLessThan(firstJoin);

    // 던진 것과 별개로 같은 호출을 동기로 한 번 더 부르면 동시화가 헛돈다(원격 호출이 둘).
    assertThat(count(source, "loadCurrentHoldings(userId)")).as("원장 보유 호출은 한 번").isEqualTo(1);
    assertThat(count(source, "loadMonthlyDividendRows(")).as("스냅샷은 부르지 않는다").isZero();
    assertThat(count(source, "findCatalog(")).as("카탈로그 호출은 한 번").isEqualTo(1);
  }

  @Test
  void 배지_실패는_배지만_비운다() throws IOException {
    String source = squash(Files.readString(CONTROLLER, StandardCharsets.UTF_8));
    int method =
        source.indexOf(
            "Map<String,MonthlyContributionPickSupport.ContributionPick>loadContributionPicks(");
    assertThat(method).as("배지 메서드가 사라졌다").isGreaterThan(0);
    int tryAt = source.indexOf("try{", method);
    int joinAt = source.indexOf("monthlyContributionPickSupport.pickFromCatalog(catalog", method);
    int catchAt = source.indexOf("}catch(Exceptionex){", method);
    assertThat(tryAt).as("try").isGreaterThan(method);
    assertThat(joinAt).as("카탈로그 전체에서 고르는 자리가 try 안").isGreaterThan(tryAt).isLessThan(catchAt);
    assertThat(source.substring(catchAt, source.indexOf("}", catchAt + 20) + 1))
        .as("실패는 로그로 남기고 빈 배지")
        .contains("log.warn(")
        .contains("returnMap.of();");
  }
}
