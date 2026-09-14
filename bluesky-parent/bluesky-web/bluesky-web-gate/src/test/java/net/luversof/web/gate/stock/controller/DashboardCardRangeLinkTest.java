package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 대시보드 지표 카드는 자기가 집계한 기간으로 착지해야 한다.
 *
 * <p>실측 2026-09-11: 카드 링크에 기간이 없어, 눌렀을 때 마지막으로 보던 기간(localStorage 복원)이 그대로 살아났다. 저장된 기간이 1개월인 상태에서
 * "올해 · 매수 2억 1,899만(91건)" 카드를 누르면 매매 화면은 1개월 20 건을 보여 줬다 &mdash; 카드에 적힌 수가 착지 화면 어디에도 없다.
 *
 * <p>기간은 두 종류다. 위 '보조 지표' 넉 장은 전기간 합계(실현 손익 225,630,135 = 매매 화면 '전체' 와 일치), 아래 '올해 요약' 넉 장은
 * 올해(StockSummaryHtmxController 가 1월 1일~오늘로 집계하므로 ytd). 기간 선택기가 없는 화면(analytics)으로 가는 링크는 실을 것이 없다.
 */
class DashboardCardRangeLinkTest {

  private static final String SUMMARY = "src/main/jte/stock/htmx/fragments/summary.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  /** summary.jte 의 statCard href 값을, '올해 요약' 블록 전/후로 나눠 모은다. */
  private List<String> hrefs(String template, boolean insideYearBlock) {
    int split = template.indexOf("@if(periodSummary != null)");
    assertThat(split).as("'올해 요약' 블록을 찾지 못했다").isGreaterThan(0);
    String scope = insideYearBlock ? template.substring(split) : template.substring(0, split);
    List<String> found = new ArrayList<>();
    String key = "href = " + '"';
    int at = scope.indexOf(key);
    while (at >= 0) {
      int from = at + key.length();
      int to = scope.indexOf('"', from);
      found.add(scope.substring(from, to));
      at = scope.indexOf(key, to);
    }
    return found;
  }

  @Test
  void 올해_요약_카드는_올해로_착지한다() throws IOException {
    List<String> links = hrefs(read(SUMMARY), true);

    assertThat(links).as("'올해 요약' 카드가 사라졌다").isNotEmpty();
    assertThat(links).allSatisfy(href -> assertThat(href).contains("rangeMode=ytd"));
  }

  @Test
  void 전기간_카드는_전체로_착지한다() throws IOException {
    List<String> links = hrefs(read(SUMMARY), false);

    assertThat(links).as("'보조 지표' 카드가 사라졌다").isNotEmpty();
    assertThat(links)
        .allSatisfy(
            href -> {
              if (href.startsWith("/stock/analytics")) {
                assertThat(href).as("기간 선택기가 없는 화면에는 기간을 싣지 않는다").doesNotContain("rangeMode");
              } else {
                assertThat(href).contains("rangeMode=all");
              }
            });
  }

  @Test
  void 대시보드가_집계하는_기간은_올해다() throws IOException {
    String controller =
        read(
            "src/main/java/net/luversof/web/gate/stock/controller/"
                + "StockSummaryHtmxController.java");

    assertThat(controller)
        .as("시작이 1월 1일이 아니면 링크의 ytd 가 거짓말이 된다")
        .contains("LocalDate periodFrom = LocalDate.now(periodZone).withDayOfYear(1);");
    assertThat(controller)
        .as("끝이 오늘이 아니면 링크의 ytd 가 거짓말이 된다")
        .contains("LocalDate periodTo = LocalDate.now(periodZone);");
  }
}
