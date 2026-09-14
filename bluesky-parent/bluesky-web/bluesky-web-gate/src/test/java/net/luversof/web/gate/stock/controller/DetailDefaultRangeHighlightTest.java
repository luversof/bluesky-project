package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 기간을 지정하지 않고 들어온 상세 화면도 '전체' 가 눌린 것으로 보여야 한다.
 *
 * <p>실측 2026-09-11, 같은 종목 상세(삼성전자):
 *
 * <pre>
 *   ?rangeMode=all   강조=전체   배지="전체 · 2020-03-23 ~ 2026-09-11"
 *   (파라미터 없음)   강조=없음   배지="전체 · 2020-03-23 ~ 2026-09-11"
 * </pre>
 *
 * <p>데이터도 배지도 같은데 진입 방법에 따라 버튼 상태만 달랐다.
 *
 * <p>단, 숨은 입력 {@code rangeMode} 는 비워 둔 채 <b>강조만</b> 맞춘다. 거기에 "all" 을 채우면 첫 방문에 전역 기간으로
 * 저장돼(date-range-picker 의 firstVisitRange) 매매·배당 화면의 기본값까지 올해에서 전체로 바뀐다.
 */
class DetailDefaultRangeHighlightTest {

  private static final String FILTER =
      "src/main/jte/stock/htmx/fragments/components/detailDateFilter.jte";

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 기간이_없으면_전체를_눌린_것으로_그린다() throws IOException {
    String template = read(FILTER);

    assertThat(template)
        .contains(
            "String navRangeMode = (rangeMode == null || rangeMode.isBlank())"
                + " && startLocal == null && endLocal == null ? \"all\" : rangeMode;");
    assertThat(template).as("내비바는 표시용 모드를 받아야 한다").contains("rangeMode = navRangeMode,");
  }

  @Test
  void 숨은_입력은_그대로_비어_있다() throws IOException {
    String template = read(FILTER);

    assertThat(template)
        .as("표시용 모드를 숨은 입력에 쓰면 '전체' 가 전역 기간으로 저장돼 다른 화면 기본값이 바뀐다")
        .contains(
            "name=\"rangeMode\" id=\"${idPrefix}RangeModeInput\" value=\"${rangeMode != null ? rangeMode : \"\"}\"");
    assertThat(template).doesNotContain("RangeModeInput\" value=\"${navRangeMode");
  }
}
