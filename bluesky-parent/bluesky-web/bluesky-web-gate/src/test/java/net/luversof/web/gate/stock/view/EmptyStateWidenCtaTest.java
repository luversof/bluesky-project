package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 빈 기간에서 다음 행동을 준다 - '기간 넓히기'.
 *
 * <p>실측 2026-09-13, 같은 빈 구간(2019-02-01~07)을 네 화면에 똑같이 넣었더니 <b>활동 목록만</b> 그 단추를 띄웠고 매매 · 배당 · 자산 성장은
 * 문구만 남겼다. 네 화면 모두 기간 프리셋 '전체' 단추를 갖고 있으므로(실측: 넷 다 1 개) CTA 가 그것을 눌러 주면 된다 - 실제로 눌러 보니
 * 2019-02-01~07 이 2009-10-06~2026-09-02 로 넓어지고 단추는 사라졌다.
 *
 * <p>이미 '전체' 면 넓힐 곳이 없으므로 띄우지 않는다. 조각은 스스로 알 수 없으므로 부모가 {@code !isAllMode} 를 넘긴다.
 */
class EmptyStateWidenCtaTest {

  private static final String COMPONENT = "src/main/jte/_components/ui/emptyState.jte";
  private static final String ACTIVITY = "src/main/jte/stock/htmx/fragments/activityList.jte";
  private static final String TRADE_FRAGMENT =
      "src/main/jte/stock/htmx/fragments/trade/tradeDetailList.jte";
  private static final String TRADE_PARENT = "src/main/jte/stock/htmx/tradeList.jte";
  private static final String DIVIDEND_FRAGMENT =
      "src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte";
  private static final String DIVIDEND_PARENT =
      "src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte";
  private static final String ASSET_GROWTH = "src/main/jte/stock/htmx/asset-growth.jte";
  private static final String TRADE_HISTORY = "src/main/jte/stock/htmx/tradeHistory.jte";
  private static final String MARK = "widenRangeAction = !safeFrom.isEmpty() || !safeTo.isEmpty()";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8).replaceAll("[ \t\r\n]+", " ");
  }

  /** 단추를 눌러 줄 상대가 있어야 한다 - 프리셋 '전체' 는 인자 0 이다. */
  @Test
  void 컴포넌트가_넓히기_단추를_그린다() throws IOException {
    String src = read(COMPONENT);

    assertThat(src).contains("@param boolean widenRangeAction = false");
    assertThat(src).contains("data-empty-widen-range");
    assertThat(read("src/main/frontend/src/common.ts"))
        .as("클릭 핸들러")
        .contains("[data-empty-widen-range]")
        .contains("[data-picker-arg=" + (char) 34 + "0" + (char) 34 + "]");
  }

  /** 기간을 따르는 목록 네 곳이 모두 CTA 를 넘긴다. */
  @Test
  void 네_화면이_모두_CTA_를_넘긴다() throws IOException {
    assertThat(read(ACTIVITY)).as("활동").contains("widenRangeAction = !isAllMode");
    assertThat(read(TRADE_PARENT)).as("매매").contains("widenRangeAction = !isAllMode");
    assertThat(read(DIVIDEND_PARENT)).as("배당").contains("widenRangeAction = !isAllMode");
    assertThat(read(ASSET_GROWTH)).as("자산 성장 차트").contains("widenRangeAction = !isAllMode");
  }

  /**
   * 활동 목록은 빈 상태를 <b>세 뷰</b>(달력 · 타임라인 · 목록)에서 각각 그린다. 하나만 빠져도 그 뷰에서는 막다른 길이 된다 - 실측: 셋 중 하나만 지우는
   * 변이를 "어딘가에 있다" 식 단언은 놓쳤다.
   */
  @Test
  void 활동_세_뷰_모두에_CTA_가_붙어_있다() throws IOException {
    String src = read(ACTIVITY);
    String marker = "emptyState(message = noActivityDataLabel";
    int at = src.indexOf(marker);
    int seen = 0;
    while (at >= 0) {
      assertThat(src.substring(at, Math.min(src.length(), at + marker.length() + 40)))
          .as("CTA 없는 빈 상태")
          .contains("widenRangeAction = !isAllMode");
      seen++;
      at = src.indexOf(marker, at + 1);
    }
    assertThat(seen).as("활동 빈 상태 수").isEqualTo(3);
  }

  /** 조각은 부모가 준 값을 그대로 쓴다 - 스스로 '전체' 인지 알 수 없다. */
  @Test
  void 조각은_부모가_준_값을_쓴다() throws IOException {
    for (String path : new String[] {TRADE_FRAGMENT, DIVIDEND_FRAGMENT}) {
      String src = read(path);

      assertThat(src).as(path).contains("@param boolean widenRangeAction = false");
      assertThat(src).as(path).contains("widenRangeAction = widenRangeAction");
    }
  }

  /**
   * 자산 성장 안의 매매 내역은 rangeMode 가 없다 - 기간이 실제로 걸렸는지로 판단한다.
   *
   * <p>이 화면은 거래가 하나도 없으면 표에 닿기 전에 <b>자기 블록</b>에서 끝난다({@code totalItems == 0}). 실측 2026-09-13: 표의 빈
   * 상태만 고쳤을 때 전체 페이지에는 단추가 하나도 안 떴다 - 그 블록도 함께 고쳐야 한다.
   */
  @Test
  void 매매_내역_조각은_기간_유무로_판단한다() throws IOException {
    String src = read(TRADE_HISTORY);

    int seen = 0;
    int at = src.indexOf(MARK);
    while (at >= 0) {
      seen++;
      at = src.indexOf(MARK, at + 1);
    }
    assertThat(seen).as("표의 빈 상태와 기간 빈 블록 둘 다").isEqualTo(2);
    assertThat(src)
        .as("기간 빈 블록도 공용 컴포넌트를 쓴다")
        .contains("@template._components.ui.emptyState(message = noTradeInPeriodLabel");
    assertThat(src).as("옛 평문 블록이 남아 있지 않다").doesNotContain("<p>${noTradeInPeriodLabel}</p>");
  }

  /** 넘기는 자리가 조각마다 하나씩이어야 한다 - 두 번 넘기면 JTE 가 같은 인자를 두 번 받는다. */
  @Test
  void 같은_인자를_두_번_넘기지_않는다() throws IOException {
    for (String path : new String[] {TRADE_PARENT, DIVIDEND_PARENT}) {
      String src = read(path);
      int at = src.indexOf("widenRangeAction = !isAllMode");
      int again = src.indexOf("widenRangeAction = !isAllMode", at + 1);

      assertThat(again).as(path + " 에 같은 인자가 두 번 있다").isEqualTo(-1);
    }
  }
}
