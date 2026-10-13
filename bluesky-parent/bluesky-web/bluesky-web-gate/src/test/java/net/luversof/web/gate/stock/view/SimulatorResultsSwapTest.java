package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 월배당 시뮬레이터 비교표의 좁히기 · 지급 시기 칩 · 정렬 · 초기화는 페이지를 새로 부르지 않고 결과 구역만 바꿔 끼운다(사용자 요청 2026-10-13: 적용할 때마다
 * 맨 위로 튀어 표까지 한참 내려와야 했다).
 *
 * <p>한 곳이라도 hx-boost 가 빠지면 그 컨트롤만 예전처럼 새로 불러 맨 위로 튄다. 구역의 show:none 이 빠지면 htmx 부스트 기본값이 맨 위로 굴린다.
 * 순서 끌기 스크립트는 처음 한 번만 잇던 것이라, 바꿔 끼운 표에도 이어지도록 정착 이벤트에서 다시 훑는다.
 */
class SimulatorResultsSwapTest {

  private static final Path FRAGMENT =
      Path.of("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");
  private static final Path PAGE = Path.of("src/main/jte/stock/simulator.jte");
  private static final Path ORDER_BUILT =
      Path.of("src/main/resources/static/js/stock/monthlyDividendProfileOrder.js");

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 결과_구역이_자기만_바꿔_끼우고_맨_위로_굴리지_않는다() throws IOException {
    String fragment = read(FRAGMENT);
    int region = fragment.indexOf("id=\"monthlyDividendResults\"");
    assertThat(region).isNotNegative();
    String tag =
        fragment.substring(fragment.lastIndexOf('<', region), fragment.indexOf('>', region) + 1);
    assertThat(tag)
        .contains("hx-target=\"#monthlyDividendResults\"")
        .contains("hx-select=\"#monthlyDividendResults\"")
        .contains("show:none")
        .contains("hx-replace-url=\"true\"")
        .contains("hx-select-oob=\"#simulatorTabs\"")
        .as("위 적립 추천 카드 길이가 바뀌어도 칩 · 폼이 같은 자리에 머물게(2026-10-13 지적)")
        .contains("data-swap-anchor=\"#monthlyDividendCompareTable\"");
    assertThat(read(PAGE)).contains("id=\"simulatorTabs\"");

    // 폼 · 칩 · 표가 구역 안에 있다(구역 시작 뒤)
    assertThat(fragment.indexOf("data-monthly-filter-form")).isGreaterThan(region);
    assertThat(fragment.indexOf("data-monthly-window-chips")).isGreaterThan(region);
    assertThat(fragment.indexOf("data-profile-order-list")).isGreaterThan(region);
  }

  @Test
  void 좁히기_칩_정렬_초기화가_모두_부스트된다() throws IOException {
    List<String> lines = read(FRAGMENT).lines().toList();
    List<String> sortLinks =
        lines.stream().filter(l -> l.contains("href=\"${monthlyDividendTableBaseUrl}")).toList();
    assertThat(sortLinks).as("전제: 정렬 링크").hasSizeGreaterThanOrEqualTo(8);
    assertThat(sortLinks.stream().filter(l -> !l.contains("hx-boost=\"true\"")).toList())
        .as("정렬 링크 하나라도 빠지면 그 열만 새로 불러 맨 위로 튄다")
        .isEmpty();
    assertThat(
            lines.stream()
                .filter(l -> l.contains("data-monthly-filter-form"))
                .findFirst()
                .orElseThrow())
        .contains("hx-boost=\"true\"");
    assertThat(
            lines.stream()
                .filter(l -> l.contains("data-filter-reset"))
                .filter(l -> l.contains("<a "))
                .findFirst()
                .orElseThrow())
        .contains("hx-boost=\"true\"");
    assertThat(
            lines.stream()
                .filter(l -> l.contains("data-monthly-window-chip="))
                .findFirst()
                .orElseThrow())
        .contains("hx-boost=\"true\"");
  }

  @Test
  void 순서_끌기가_바꿔_끼운_표에도_이어진다() throws IOException {
    assertThat(read(ORDER_BUILT)).contains("htmx:afterSettle");
  }
}
