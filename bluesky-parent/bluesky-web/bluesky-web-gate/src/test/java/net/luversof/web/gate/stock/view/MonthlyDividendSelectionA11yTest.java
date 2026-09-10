package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 선택 가능한 표의 행 선택은 보조기술에 상태가 전달되고, 선택한 행의 글자도 읽힌다.
 *
 * <p>실측 2026-09-10(qa/selectable-rows-a11y.cjs): 선택을 행의 aria-selected 로만 표시했는데, 순수 table 안의 row 는 그
 * 속성을 접근성 트리에 내보내지 않는다 - 자산 현황 종목 9행·계좌 5행, 배당 수익률 14행, 매매 실현손익 8행, 월배당 시뮬레이터 8행 모두 행을 고른 뒤 접근성 노드
 * 속성이 focusable/focused 뿐이었다(선택 여부가 없다). 표마다 선택 코드가 따로 있어(assetStatus.ts 2곳·dividendHistory.ts·조각 안
 * 인라인 2곳) 각자 고치면 같은 코드가 다섯 벌 늘어난다 - 행에 data-row-select 만 달고 체크박스는 common.ts 가 한 번만 만들어 넣는다.
 *
 * <p>실측 2026-09-10(qa/monthly-sim-selected-contrast.cjs): 8 행을 모두 고르면 선택 배경(primary 10% 를 섞은
 * #eaf0fc) 위의 11px text-warning 글자 3곳이 4.39:1 로 AA(4.5) 에 못 미쳤다. 라이트 팔레트의 warning 을 한 단계 어둡게 해 흰
 * 배경과 선택 배경 어디에서도 4.5 를 넘긴다.
 */
class MonthlyDividendSelectionA11yTest {

  /** 선택 가능한 행이 있는 조각들. 행마다 data-row-select 가 있어야 체크박스가 붙는다. */
  private static final List<String> SELECTABLE_FRAGMENTS =
      List.of(
          "src/main/jte/stock/htmx/fragments/assetStatus.jte",
          "src/main/jte/stock/htmx/fragments/dividend/dividendSummaryCards.jte",
          "src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte",
          "src/main/jte/stock/htmx/fragments/trade/tradeRealizedSections.jte",
          "src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

  private static final Path COMMON = Path.of("src/main/resources/static/js/common.js");
  private static final Path CSS = Path.of("src/main/frontend/main.css");

  /** 실측한 선택 행 배경(라이트). color-mix 결과라 계산 대신 브라우저가 내놓은 값을 쓴다. */
  private static final String SELECTED_ROW_BG = "#eaf0fc";

  private static final String WARNING_KEY = "--color-warning:";

  private static double luminance(String hex) {
    int rgb = Integer.parseInt(hex.substring(1), 16);
    double[] channels = {(rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0, (rgb & 255) / 255.0};
    for (int i = 0; i < channels.length; i++) {
      double c = channels[i];
      channels[i] = c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }
    return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2];
  }

  private static double contrast(String foreground, String background) {
    double a = luminance(foreground);
    double b = luminance(background);
    return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);
  }

  private static int count(String text, String needle) {
    int found = 0;
    int at = text.indexOf(needle);
    while (at >= 0) {
      found++;
      at = text.indexOf(needle, at + needle.length());
    }
    return found;
  }

  @Test
  void 선택_가능한_행은_모두_체크박스를_요청한다() throws IOException {
    List<String> missing = new ArrayList<>();
    int rows = 0;
    for (String fragment : SELECTABLE_FRAGMENTS) {
      String html = Files.readString(Path.of(fragment), StandardCharsets.UTF_8);
      int selectable = count(html, "aria-selected=" + (char) 34 + "false" + (char) 34);
      int marked = count(html, "data-row-select=");
      rows += selectable;
      if (selectable != marked) {
        missing.add(
            fragment + " 선택행 " + selectable + " 개 중 " + marked + " 개만 data-row-select 를 달았다");
      }
    }
    assertThat(rows).as("선택 가능한 행을 찾지 못했다").isGreaterThanOrEqualTo(8);
    assertThat(missing).as("체크박스가 붙지 않는 행은 선택 상태가 보조기술에 전달되지 않는다").isEmpty();
  }

  @Test
  void 체크박스는_공용_처리가_한_번만_만든다() throws IOException {
    String js = Files.readString(COMMON, StandardCharsets.UTF_8);
    assertThat(js).as("공용 처리가 빌드 산출물에 없다").contains("data-row-select-checkbox");
    assertThat(js).as("행의 선택 상태를 체크박스에 맞추지 않는다").contains(".checked=");
    assertThat(js).as("체크박스 change 를 듣지 않는다").contains("change");
    assertThat(js).as("24px 목표 크기를 만드는 라벨 클래스를 쓰지 않는다").contains("row-select-box");

    String css = Files.readString(CSS, StandardCharsets.UTF_8);
    assertThat(css).as("row-select-box 모양 정의가 없다").contains(".row-select-box {");

    for (String fragment : SELECTABLE_FRAGMENTS) {
      String html = Files.readString(Path.of(fragment), StandardCharsets.UTF_8);
      assertThat(html)
          .as(fragment + " 이 조각 안에서 체크박스를 따로 만들고 있다 - 처리는 한 곳이어야 한다")
          .doesNotContain("data-row-select-checkbox");
    }
  }

  @Test
  void 라이트_warning_글자는_선택한_행에서도_AA_를_넘는다() throws IOException {
    String css = Files.readString(CSS, StandardCharsets.UTF_8);
    int theme = css.indexOf("@theme {");
    assertThat(theme).as("@theme 블록을 찾지 못했다").isGreaterThan(-1);
    int at = css.indexOf(WARNING_KEY, theme);
    assertThat(at).as("라이트 --color-warning 을 찾지 못했다").isGreaterThan(theme);
    String warning =
        css.substring(at + WARNING_KEY.length(), css.indexOf((char) 59, at)).trim().toLowerCase();
    assertThat(warning).as("warning 이 6자리 hex 가 아니다").hasSize(7).startsWith("#");

    assertThat(contrast(warning, SELECTED_ROW_BG))
        .as("선택한 행 배경 " + SELECTED_ROW_BG + " 위의 warning 글자가 AA 에 못 미친다")
        .isGreaterThanOrEqualTo(4.5);
    assertThat(contrast(warning, "#ffffff"))
        .as("흰 배경 위 warning 글자가 AA 에 못 미친다")
        .isGreaterThanOrEqualTo(4.5);

    // 인쇄용 다크 재정의도 같은 값이어야 한다(다크로 인쇄해도 종이에서는 라이트 팔레트를 쓴다).
    assertThat(count(css, WARNING_KEY + " " + warning + ";"))
        .as("라이트와 인쇄용 다크 두 곳에 같은 warning 값이 있어야 한다")
        .isEqualTo(2);
  }
}
