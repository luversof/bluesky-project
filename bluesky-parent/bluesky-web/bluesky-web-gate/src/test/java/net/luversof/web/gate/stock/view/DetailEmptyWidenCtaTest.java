package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 상세 두 화면의 <b>기간 한정</b> 빈 안내에도 '전체 기간으로 보기' 를 준다.
 *
 * <p>실측 2026-09-13, 빈 구간(2019-02-01~07): 목록 네 화면은 단추를 띄우는데 종목 상세 3 자리(시세 · 매매 · 배당)와 계좌 상세 2 자리(매매
 * · 배당)는 평문 상자라 <b>막다른 길</b>이었다. 두 화면 모두 프리셋 '전체' 단추는 갖고 있었다(각 1 개).
 *
 * <p>기간과 무관한 안내(계좌별 보유 현황의 "지금 보유 중인 수량이 없습니다", 종목/계좌를 못 찾음)에는 붙이지 않는다 - 기간을 넓혀도 달라지지 않는다.
 */
class DetailEmptyWidenCtaTest {

  private static final String ITEM = "src/main/jte/stock/htmx/stockItemDetailContent.jte";
  private static final String ACCOUNT = "src/main/jte/stock/htmx/accountDetailContent.jte";
  private static final String ITEM_SHELL = "src/main/jte/stock/stockItemDetail.jte";
  private static final String ACCOUNT_SHELL = "src/main/jte/stock/accountDetail.jte";
  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockDetailViewController.java";
  private static final String CTA = "widenRangeAction = !\"all\".equals(rangeMode)";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  private int count(String src, String token) {
    int seen = 0;
    int at = src.indexOf(token);
    while (at >= 0) {
      seen++;
      at = src.indexOf(token, at + 1);
    }
    return seen;
  }

  /** 컨트롤러가 지금 기간 모드를 화면에 넘긴다 - 두 상세 화면 모두. */
  @Test
  void 컨트롤러가_기간_모드를_넘긴다() throws IOException {
    assertThat(
            count(
                read(CONTROLLER),
                "model.addAttribute(\"rangeMode\", rangeMode == null ? \"\" : rangeMode);"))
        .as("종목 상세 · 계좌 상세")
        .isEqualTo(2);
  }

  /** 껍데기가 조각에 그대로 흘려 준다. */
  @Test
  void 껍데기가_조각에_넘긴다() throws IOException {
    for (String path : new String[] {ITEM_SHELL, ACCOUNT_SHELL}) {
      assertThat(read(path)).as(path).contains("@param String rangeMode = \"\"");
      assertThat(read(path)).as(path).contains("rangeMode = rangeMode");
    }
  }

  /** 종목 상세 세 자리(시세 · 매매 · 배당)에 모두 붙어 있다. */
  @Test
  void 종목_상세_세_자리에_붙는다() throws IOException {
    String src = read(ITEM);

    assertThat(src).contains("@param String rangeMode = \"\"");
    assertThat(count(src, CTA)).as("종목 상세 CTA 수").isEqualTo(3);
    for (String key :
        new String[] {
          "stock.item.detail.empty.price.history",
          "stock.item.detail.empty.trades",
          "stock.item.detail.empty.dividends"
        }) {
      int at = src.indexOf(key);

      assertThat(at).as(key).isPositive();
      assertThat(src.substring(at, Math.min(src.length(), at + 130))).as(key).contains(CTA);
    }
  }

  /** 계좌 상세 두 자리(매매 · 배당)에 모두 붙어 있다. */
  @Test
  void 계좌_상세_두_자리에_붙는다() throws IOException {
    String src = read(ACCOUNT);

    assertThat(src).contains("@param String rangeMode = \"\"");
    assertThat(count(src, CTA)).as("계좌 상세 CTA 수").isEqualTo(2);
  }

  /** 기간과 무관한 안내에는 붙이지 않는다 - 넓혀도 달라지지 않는다. */
  @Test
  void 기간과_무관한_안내에는_안_붙인다() throws IOException {
    for (String path : new String[] {ITEM, ACCOUNT}) {
      String src = read(path);
      for (String key :
          new String[] {
            "stock.item.detail.empty.account.holdings",
            "stock.item.detail.notfound",
            "stock.account.detail.notfound",
            "stock.account.detail.empty.holdings"
          }) {
        int at = src.indexOf(key);
        if (at < 0) {
          continue;
        }
        assertThat(src.substring(at, Math.min(src.length(), at + 130)))
            .as(path + " 의 " + key)
            .doesNotContain("widenRangeAction");
      }
    }
  }
}
