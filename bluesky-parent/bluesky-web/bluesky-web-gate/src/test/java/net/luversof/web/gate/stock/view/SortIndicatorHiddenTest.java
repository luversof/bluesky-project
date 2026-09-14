package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 정렬 화살표는 장식이다 - 보조기술에 읽히면 안 된다.
 *
 * <p>실측 2026-09-11: 정렬 헤더 40 개(배당 22 · 자산 현황 18) 전부 접근 가능한 이름이 "종목명 ↕" 처럼 기호까지 포함했다. 화면 낭독기는 이 기호를
 * "위아래 화살표" 로 읽는다 &mdash; 실제 정렬 상태는 같은 칸의 {@code aria-sort}(none/ascending/descending)가 이미 전하고 있어
 * 이름에는 군더더기다.
 *
 * <p>JS 는 이 칸의 {@code textContent} 만 바꾸므로(▲/▼/↕) 템플릿에 붙인 속성은 정렬을 바꿔도 남는다.
 */
class SortIndicatorHiddenTest {

  private static final String[] TEMPLATES = {
    "src/main/jte/stock/htmx/fragments/assetStatus.jte",
    "src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte"
  };

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
  void 모든_정렬_표시는_보조기술에서_숨긴다() throws IOException {
    for (String rel : TEMPLATES) {
      String template = Files.readString(Path.of(rel), StandardCharsets.UTF_8);

      int indicators = count(template, "data-sort-indicator");
      int hidden =
          count(template, "data-sort-indicator aria-hidden=" + (char) 34 + "true" + (char) 34);
      assertThat(indicators).as(rel + " 의 정렬 표시 수").isPositive();
      assertThat(hidden).as(rel + " 에서 숨기지 않은 정렬 표시가 있으면 안 된다").isEqualTo(indicators);
    }
  }

  /** 상태는 aria-sort 가 전한다 - 기호를 숨겨도 정보가 사라지지 않는다는 전제를 함께 고정한다. */
  @Test
  void 정렬_상태는_aria_sort_로_전한다() throws IOException {
    for (String rel :
        new String[] {
          "src/main/frontend/src/stock/assetStatus.ts",
          "src/main/frontend/src/stock/dividendHistory.ts"
        }) {
      String script = Files.readString(Path.of(rel), StandardCharsets.UTF_8);

      assertThat(script).as(rel).contains("aria-sort");
      assertThat(script).as(rel + " 는 표시 글자만 바꿔야 한다").contains("indicator.textContent");
    }
  }
}
