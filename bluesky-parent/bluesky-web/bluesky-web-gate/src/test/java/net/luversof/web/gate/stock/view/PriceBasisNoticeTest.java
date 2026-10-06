package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 평가값을 보여 주는 화면은 그 값이 어느 날 종가 기준인지 적는다.
 *
 * <p>실측 2026-09-11: 평가값을 보여 주는 여섯 화면 중 대시보드 · 자산 현황 · 종목 상세 · 계좌 상세 넷만 적고, <b>자산 성장</b>과 <b>월배당
 * 시뮬레이터</b> 둘은 없었다. 특히 월배당 표는 배당 기준값(스냅샷 시점)과 시세(최근 종가) 두 시점을 한 줄에 섞어 보여 주면서 앞의 날짜만 적었다 &mdash; 스냅샷
 * 2026-08-19 인데 현재가는 2026-09-09 종가였다(두 값이 같은 시점처럼 읽힌다).
 */
class PriceBasisNoticeTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 자산_성장도_종가_기준일을_적는다() throws IOException {
    String controller =
        read(
            "src/main/java/net/luversof/web/gate/stock/controller/StockAssetGrowthHtmxController.java");
    String template = read("src/main/jte/stock/htmx/asset-growth.jte");

    assertThat(controller).contains("StockPriceBasisUtil.priceBasisDateWithFallback(");
    // 줄바꿈·들여쓰기에 묶지 않는다 - 이 규칙은 CR LF 와 여덟 칸 들여쓰기까지 고정하고 있어서,
    // 같은 파일 위쪽에 줄을 몇 줄 더한 것만으로 깨졌다(실측 2026-09-12). 확인하려는 것은 서식이 아니라
    // "이 모델 속성을 담는다" 이므로 공백을 눌러서 본다.
    assertThat(flatten(controller).replace("( ", "("))
        .contains("model.addAttribute(" + (char) 34 + "priceBasisDate" + (char) 34);
    assertThat(template).contains("@param java.time.LocalDate priceBasisDate");
    // 문구 틀은 StockPriceBasisUtil.basisMessage 가 고른다(종가 / 장중 시세, 2026-10-02).
    assertThat(template).contains("StockPriceBasisUtil.basisMessage(priceBasisIntradayTime)");
  }

  @Test
  void 월배당_시뮬레이터도_시세_기준일을_적는다() throws IOException {
    String support =
        read(
            "src/main/java/net/luversof/web/gate/stock/service/MonthlyDividendReferenceSupport.java");
    String page = read("src/main/jte/stock/simulator.jte");
    String fragment = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

    assertThat(support).as("현재 보유 조회가 이미 종가 일자를 갖고 있다").contains("priceBasisDate");
    assertThat(page).contains("monthlyDividendPriceBasisDate = monthlyDividendPriceBasisDate");
    assertThat(fragment).contains("@param java.time.LocalDate monthlyDividendPriceBasisDate");
    // 문구 틀은 StockPriceBasisUtil.basisMessage 가 고른다(종가 / 장중 시세, 2026-10-02).
    assertThat(fragment)
        .contains("StockPriceBasisUtil.basisMessage(monthlyDividendPriceBasisIntradayTime)");
  }

  /** 공백을 한 칸으로 눌러 서식 차이를 지운다. */
  private static String flatten(String source) {
    StringBuilder sb = new StringBuilder();
    boolean space = false;
    for (char c : source.toCharArray()) {
      if (Character.isWhitespace(c)) {
        space = true;
        continue;
      }
      if (space && sb.length() > 0) {
        sb.append(' ');
      }
      space = false;
      sb.append(c);
    }
    return sb.toString();
  }
}
