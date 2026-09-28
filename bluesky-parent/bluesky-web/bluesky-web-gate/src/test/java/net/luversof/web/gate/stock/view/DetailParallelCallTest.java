package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 종목 · 계좌 상세 조각은 최초 데이터 일자(계좌는 이동 목록까지)를 다른 조회와 함께 던진다.
 *
 * <p>실측 2026-09-23: 계좌 상세 조각이 이동 목록을 동시 묶음 앞에서 동기로, 최초 일자를 묶음이 다 끝난 뒤(+35ms) 동기로 불러 왕복 두 번이 더해졌다. 두
 * 메서드에 같은 줄이 있어 메서드 몸체를 잘라 따로 본다. 공백을 전부 지워 비교한다.
 */
class DetailParallelCallTest {

  private static final Path CONTROLLER =
      Path.of(
          "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java");

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

  private static String body(String source, String signature) {
    int start = source.indexOf(signature);
    assertThat(start).as(signature + " 가 사라졌다").isGreaterThan(0);
    int next = source.indexOf("publicString", start + signature.length());
    return next > 0 ? source.substring(start, next) : source.substring(start);
  }

  @Test
  void 종목_상세는_최초_일자를_함께_던진다() throws IOException {
    String item =
        body(
            squash(Files.readString(CONTROLLER, StandardCharsets.UTF_8)),
            "publicStringstockItemDetailPage(");
    String supply =
        "firstDateFuture=stockAsync.supply(()->detailDataFirstDate(userId,resolvedId,null,filterZone));";
    int firstJoin = item.indexOf("StockAsyncSupport.join(");
    assertThat(count(item, supply)).as("최초 일자를 던진다").isEqualTo(1);
    assertThat(item.indexOf(supply)).as("첫 join 전에").isGreaterThan(0).isLessThan(firstJoin);
    assertThat(count(item, "detailDataFirstDate(")).as("동기로 또 부르지 않는다").isEqualTo(1);
    assertThat(item)
        .contains(
            "StringdetailFirstDate=net.luversof.web.gate.stock.support.StockAsyncSupport.join(firstDateFuture);");
  }

  @Test
  void 계좌_상세는_이동_목록과_최초_일자를_함께_던진다() throws IOException {
    String account =
        body(
            squash(Files.readString(CONTROLLER, StandardCharsets.UTF_8)),
            "publicStringaccountDetailPage(");
    String nav =
        "accountNavFuture=stockAsync.supply(()->accountClient.getAccountsByUserId(userId));";
    String first =
        "firstDateFuture=stockAsync.supply(()->detailDataFirstDate(userId,null,resolvedId,filterZone));";
    int firstJoin = account.indexOf("StockAsyncSupport.join(");
    assertThat(firstJoin).as("받는 자리").isGreaterThan(0);
    assertThat(account.indexOf(nav))
        .as("이동 목록을 첫 join 전에 던진다")
        .isGreaterThan(0)
        .isLessThan(firstJoin);
    assertThat(account.indexOf(first))
        .as("최초 일자를 첫 join 전에 던진다")
        .isGreaterThan(0)
        .isLessThan(firstJoin);
    assertThat(account.indexOf(nav))
        .as("이동 목록은 다섯 조회 묶음(accCalls)보다 앞에서 던진다")
        .isLessThan(account.indexOf("varaccCalls=stockAsync.deduper();"));
    assertThat(count(account, "getAccountsByUserId(")).as("이동 목록 호출은 한 번").isEqualTo(1);
    assertThat(count(account, "detailDataFirstDate(")).as("최초 일자 호출은 한 번").isEqualTo(1);
    assertThat(account)
        .as("받은 이동 목록을 모델에 싣는다")
        .contains(
            "accountNavEntries(net.luversof.web.gate.stock.support.StockAsyncSupport.join(accountNavFuture),resolvedId,request.getQueryString())");
  }
}
