package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * 자산 성장의 표 넷을 묶은 <b>탭</b>.
 *
 * <p>표를 세로로 쌓으면 아래쪽은 스크롤해야 나와서 있는 줄도 모르게 된다 &mdash; 실측 2026-09-07: 이 화면이 5,075px(5.6 화면) · 카드 9
 * 개였고, 그중 표 셋이 이번에 늘어난 것이다. 실제로 "월별 성과를 못 찾겠다" 는 일이 이미 한 번 있었다.
 *
 * <p>탭은 <b>내용이 있는 것만</b> 내야 한다. 표마다 안 그리는 조건이 다르므로(구간이 하나뿐이거나, 그 해에 아무 일도 없었거나) 조건이 어긋나면 눌러도 아무것도 안
 * 나오는 빈 탭이 생긴다. 그래서 탭 이름과 패널 이름이 짝이 맞는지 소스에서 센다.
 */
class AssetGrowthTableTabsTest {

  private static final Path PAGE = Path.of("src/main/jte/stock/htmx/asset-growth.jte");
  private static final Path SCRIPT = Path.of("src/main/frontend/src/common.ts");

  private static String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  private static java.util.List<String> matches(String source, String regex) {
    Matcher matcher = Pattern.compile(regex).matcher(source);
    java.util.List<String> found = new java.util.ArrayList<>();
    while (matcher.find()) {
      found.add(matcher.group(1));
    }
    return found;
  }

  /** 탭 이름과 패널 이름이 짝이 맞아야 한다. 한쪽만 늘리면 눌러도 아무것도 안 나오는 탭이 생긴다. */
  @Test
  void 탭과_패널의_이름이_짝을_이룬다() throws IOException {
    String page = read(PAGE);

    java.util.List<String> tabs = matches(page, "data-panel-tab=\"([a-zA-Z]+)\"");
    java.util.List<String> panels = matches(page, "data-panel=\"([a-zA-Z]+)\"");

    assertThat(tabs).as("탭을 하나도 찾지 못했다 - 검사가 무력해진다").isNotEmpty();
    assertThat(panels).as("패널 이름이 탭 이름과 다르다").containsExactlyInAnyOrderElementsOf(tabs);
    assertThat(tabs)
        .as("표 넷을 다 묶어야 한다")
        .containsExactlyInAnyOrder("breakdown", "contribution", "yearly", "cost");
  }

  /**
   * 탭을 낼지 말지는 <b>각 조각이 그리는 조건과 같아야</b> 한다.
   *
   * <p>조건을 화면 쪽에 한 벌 더 적는 셈이라 어긋날 수 있다. 조각의 가드와 같은 식을 쓰는지 본다.
   */
  @Test
  void 탭_조건이_조각의_가드와_같다() throws IOException {
    String page = read(PAGE);

    assertThat(page)
        .as("기간별 표는 구간이 둘 이상이거나 안내문이 있을 때 그린다")
        .contains("periodBreakdown.size() > 1")
        .contains("!periodBreakdownNote.isEmpty()")
        .as("종목별 기여는 접힌 줄까지 세어 둘 이상일 때 그린다")
        .contains("stockContributions.size() + stockContributionOthersCount > 1")
        .as("연도별 성과·세금비용은 줄이 있으면 그린다")
        .contains("!yearlySummaries.isEmpty()")
        .contains("!yearlyCosts.isEmpty()");
  }

  /** 탭이 하나뿐이면 고를 것이 없어 자리만 차지한다. */
  @Test
  void 탭이_하나뿐이면_탭_바를_내지_않는다() throws IOException {
    assertThat(read(PAGE)).contains("@if(panelCount > 1)");
  }

  /**
   * 전환은 <b>이미 받아 둔 조각을 보이고 숨기는 것</b>이어야 한다.
   *
   * <p>탭마다 서버를 다시 부르면 같은 시뮬레이션이 탭 수만큼 돈다. 서버는 네 표를 한 번에 그려 보내므로 왕복이 필요 없다.
   */
  @Test
  void 탭_전환에_왕복이_없다() throws IOException {
    String page = read(PAGE);
    int bar = page.indexOf("data-panel-tab-group");
    assertThat(bar).as("탭 바를 찾지 못했다").isGreaterThan(0);
    String tabBar = page.substring(bar, page.indexOf("</div>", bar));

    assertThat(tabBar).as("탭이 서버를 다시 부르면 같은 시뮬레이션이 탭 수만큼 돈다").doesNotContain("hx-get");
  }

  /** 고른 탭은 다음에 와도 유지돼야 한다. 매번 첫 탭으로 돌아가면 아래 표는 여전히 안 보게 된다. */
  @Test
  void 고른_탭을_기억한다() throws IOException {
    String script = read(SCRIPT);

    assertThat(script)
        .contains("panel-tab:")
        .as("htmx 로 조각이 갈릴 때마다 다시 적용해야 한다")
        .contains("document.addEventListener(\"htmx:afterSettle\", restorePanelTabs)");
  }

  /**
   * 저장된 탭이 지금 화면에 없으면 첫 탭으로 떨어져야 한다.
   *
   * <p>기간을 바꾸면 있던 표가 사라진다(예: '전체' 를 고르면 월별 표가 안내문만 남는다). 그때 저장된 탭을 그대로 고르면 아무 패널도 안 보인다.
   */
  @Test
  void 없는_탭이_저장돼_있으면_첫_탭으로_떨어진다() throws IOException {
    // 2026-09-10 부터 화면이 기본 탭을 선언할 수 있다(data-panel-tab-default). 저장값이 없거나 없는 탭이면
    // 선언 기본 -> 그것도 없으면 첫 탭 순으로 떨어진다.
    String script = read(SCRIPT);
    assertThat(script).contains("names.indexOf(saved) >= 0 ? saved : fallback");
    assertThat(script)
        .contains("preferred && names.indexOf(preferred) >= 0 ? preferred : names[0]");
  }
}
