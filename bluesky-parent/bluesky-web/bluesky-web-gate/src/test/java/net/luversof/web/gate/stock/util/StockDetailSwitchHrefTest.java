package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 상세 화면 위쪽 전환기(다른 종목 · 다른 계좌) 링크의 주소.
 *
 * <p>전환기는 대상 아이디만 적고 있었다. 기간은 브라우저 저장값이 받쳐 주므로 화면에서는 그대로 보이지만, <b>그 주소를 공유하면</b> 받는 쪽에는 다른 기간이 열린다
 * - 실측 2026-09-13: 1 년(2025-09-14~2026-09-13)을 보다가 만들어진 계좌 링크가, 저장값이 없는 쪽에서는 전체(2019-12-26~)로 열렸다.
 * 기간을 실어 보내면 같은 화면이 열린다.
 *
 * <p>질의는 풀었다 다시 묶지 않는다 - 다시 묶으면 인코딩이 바뀌고, 같은 이름이 여러 번 오는 값(계좌 목록)의 순서가 흔들린다.
 */
class StockDetailSwitchHrefTest {

  private int count(String text, String token) {
    int found = 0;
    int at = text.indexOf(token);
    while (at >= 0) {
      found++;
      at = text.indexOf(token, at + token.length());
    }
    return found;
  }

  @Test
  void 대상만_바꾸고_나머지는_그대로_옮긴다() {
    String href =
        StockTabLinkUtil.switchHref(
            "/stock/item",
            "stockItemId=OLD&rangeMode=12&timeZone=Asia%2FSeoul",
            "stockItemId",
            "NEW");

    assertThat(href).isEqualTo("/stock/item?rangeMode=12&timeZone=Asia%2FSeoul&stockItemId=NEW");
  }

  /** 인코딩된 값은 다시 인코딩하지 않고 조각째 옮긴다. */
  @Test
  void 인코딩을_건드리지_않는다() {
    String href =
        StockTabLinkUtil.switchHref(
            "/stock/account", "accountId=A&keyword=%EB%B6%80%EB%8F%99%EC%82%B0", "accountId", "B");

    assertThat(href).contains("keyword=%EB%B6%80%EB%8F%99%EC%82%B0");
    assertThat(href).doesNotContain("%25");
  }

  /** 같은 이름이 여러 번 오는 값(계좌 목록)은 개수와 순서가 그대로여야 한다. */
  @Test
  void 같은_이름이_여러_번인_값을_지키다() {
    String href =
        StockTabLinkUtil.switchHref(
            "/stock/item", "accountIdList=a&accountIdList=b&accountIdList=c&stockItemId=OLD",
            "stockItemId", "NEW");

    assertThat(href)
        .isEqualTo("/stock/item?accountIdList=a&accountIdList=b&accountIdList=c&stockItemId=NEW");
  }

  /**
   * 이름이 겹치는 파라미터를 같이 지우면 안 된다 - {@code accountId} 를 바꾸는데 {@code accountIdList}(계좌 필터)까지 사라지면 좁혀 보던
   * 표가 전체로 돌아간다. 이름은 <b>정확히</b> 같을 때만 옛 것으로 본다.
   */
  @Test
  void 이름이_겹치는_다른_파라미터는_지키다() {
    String href =
        StockTabLinkUtil.switchHref(
            "/stock/account", "accountId=A&accountIdList=x&accountIdList=y", "accountId", "B");

    assertThat(href).isEqualTo("/stock/account?accountIdList=x&accountIdList=y&accountId=B");

    String itemHref =
        StockTabLinkUtil.switchHref(
            "/stock/item", "stockItemId=A&stockItemIdList=z", "stockItemId", "B");

    assertThat(itemHref).isEqualTo("/stock/item?stockItemIdList=z&stockItemId=B");
  }

  @Test
  void 질의가_없으면_대상만_적는다() {
    assertThat(StockTabLinkUtil.switchHref("/stock/item", null, "stockItemId", "NEW"))
        .isEqualTo("/stock/item?stockItemId=NEW");
    assertThat(StockTabLinkUtil.switchHref("/stock/item", "", "stockItemId", "NEW"))
        .isEqualTo("/stock/item?stockItemId=NEW");
  }

  /** 옛 대상이 여러 번 실려 있어도 하나도 남기지 않는다 - 남으면 서버가 어느 것을 볼지 알 수 없다. */
  @Test
  void 옛_대상은_하나도_남기지_않는다() {
    String href =
        StockTabLinkUtil.switchHref(
            "/stock/account", "accountId=A&rangeMode=12&accountId=B", "accountId", "C");

    assertThat(href).isEqualTo("/stock/account?rangeMode=12&accountId=C");
  }

  /** 유틸이 맞아도 전환기가 안 쓰면 소용이 없다 - 예전처럼 문자열을 이어 붙이면 기간이 다시 빠진다. */
  @Test
  void 두_전환기가_이_유틸을_쓴다() throws java.io.IOException {
    String source =
        java.nio.file.Files.readString(
                java.nio.file.Path.of(
                    "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java"),
                java.nio.charset.StandardCharsets.UTF_8)
            .replaceAll("[" + (char) 32 + (char) 9 + (char) 13 + (char) 10 + "]+", " ");
    char q = (char) 34;

    assertThat(
            count(
                source,
                "switchHref( "
                    + q
                    + "/stock/item"
                    + q
                    + ", currentQuery, "
                    + q
                    + "stockItemId"
                    + q))
        .as("종목 전환기는 호출부가 둘이다(보유 목록 + 지금 보는 종목) - 하나만 고치면 나머지가 샌다")
        .isEqualTo(2);
    assertThat(
            count(
                source,
                "switchHref( "
                    + q
                    + "/stock/account"
                    + q
                    + ", currentQuery, "
                    + q
                    + "accountId"
                    + q))
        .as("계좌 전환기")
        .isEqualTo(1);
    assertThat(source)
        .as("옛 방식으로 이어 붙인 자리가 남아 있으면 안 된다")
        .doesNotContain(q + "/stock/item?stockItemId=" + q + " +");
    assertThat(source).as("계좌도 마찬가지").doesNotContain(q + "/stock/account?accountId=" + q + " +");
  }

  /** 값이 없으면 대상 없는 주소가 된다(빈 accountId= 를 붙이면 400 이 난다). */
  @Test
  void 값이_없으면_붙이지_않는다() {
    assertThat(StockTabLinkUtil.switchHref("/stock/account", "rangeMode=12", "accountId", ""))
        .isEqualTo("/stock/account?rangeMode=12");
  }
}
