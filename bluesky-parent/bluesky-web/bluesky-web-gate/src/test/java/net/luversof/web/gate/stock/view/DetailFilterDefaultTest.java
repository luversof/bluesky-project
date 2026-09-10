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
 * 상세 조회 필터는 걸린 필터가 있을 때만 펼쳐진다.
 *
 * <p>실측 2026-09-10(qa/filter-space.cjs): 배당·매매·활동·자산 성장 네 화면 모두 상세 조회가 펼친 채로 열려 화면당 116~124px 를 먹는데
 * 적용된 필터는 0개였다. 첫 표·차트가 465~796px 아래로 밀려 첫 화면이 조회 도구로 채워졌다.
 *
 * <p>고친 뒤(qa/filter-default.cjs): 접힘 28px, 첫 표·차트가 377~698px 로 88~98px 올라왔다. 필터가 걸리면 왜 결과가 좁혀졌는지
 * 보여야 하므로 펼친다. 사용자가 한 번이라도 접거나 펼치면 그 선택이 이긴다(localStorage).
 */
class DetailFilterDefaultTest {

  private static final List<Path> FORMS =
      List.of(
          Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendSearchForm.jte"),
          Path.of("src/main/jte/stock/htmx/fragments/trade/tradeSearchForm.jte"),
          Path.of("src/main/jte/stock/htmx/fragments/activityList.jte"),
          Path.of("src/main/jte/stock/htmx/asset-growth.jte"));

  @Test
  void 상세_조회는_서버에서_펼친_채로_내려오지_않는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    for (Path p : FORMS) {
      String html = Files.readString(p, StandardCharsets.UTF_8);
      int at = html.indexOf("data-detail-filter");
      assertThat(at).as(p + " 에 상세 조회 필터가 없다").isGreaterThan(0);
      int open = html.lastIndexOf((char) 60, at);
      String tag = html.substring(open, html.indexOf((char) 62, at) + 1);
      if (tag.contains(" open")) offenders.add(p.getFileName().toString());
      assertThat(tag)
          .as(p + " 가 필터 적용 여부를 알려주지 않으면 걸린 필터가 있어도 접힌 채로 나온다")
          .contains("data-filter-active");
    }
    assertThat(offenders).as("적용된 필터가 없어도 펼친 채로 내려오는 화면").isEmpty();
  }

  @Test
  void 걸린_필터가_있으면_펼치고_사용자_선택이_그보다_우선한다() throws IOException {
    String js =
        Files.readString(
            Path.of("src/main/resources/static/js/stock/detailFilterState.js"),
            StandardCharsets.UTF_8);
    assertThat(js).as("필터 적용 여부를 보지 않는다").contains("data-filter-active");
    assertThat(js).as("저장된 사용자 선택을 먼저 보지 않는다").contains("stockDetailFilterOpen");
  }
}
