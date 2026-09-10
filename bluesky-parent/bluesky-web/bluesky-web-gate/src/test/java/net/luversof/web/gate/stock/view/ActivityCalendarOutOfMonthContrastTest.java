package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 활동 달력의 달 밖 채움 칸(앞뒤 달 날짜)은 불투명도가 아니라 글자색으로 눌러야 한다.
 *
 * <p>axe 실측 2026-09-09(달력 보기): 셀 전체 {@code opacity-30} 때문에 날짜 숫자 대비가 1.59:1, 붉은 날 1.78:1 이었다(11px,
 * 기준 4.5:1). 타임라인 보기가 기본이라 그동안 0건으로 보였던 것뿐이다. 숫자 색만 본문색 60% 로 낮추는 규칙({@code .cal-day-out
 * .cal-day-num})은 레이어 밖에 있어 붉은 날의 {@code text-error} 유틸리티도 이긴다.
 */
class ActivityCalendarOutOfMonthContrastTest {

  private static final Path TEMPLATE =
      Path.of("src/main/jte/stock/htmx/fragments/activityList.jte");
  private static final Path CSS = Path.of("src/main/frontend/main.css");

  @Test
  void 달_밖_칸은_opacity_대신_cal_day_out_클래스를_쓴다() throws IOException {
    String src = Files.readString(TEMPLATE, StandardCharsets.UTF_8);
    assertThat(src).contains("\" cal-day-out\"");
    assertThat(src).as("셀 전체 불투명도는 자식 글자 대비까지 곱해 떨어뜨린다").doesNotContain("opacity-30");
  }

  @Test
  void 달_밖_숫자색_규칙은_본문색_60퍼센트이고_레이어_밖에_있다() throws IOException {
    String css = Files.readString(CSS, StandardCharsets.UTF_8);
    int at = css.indexOf(".cal-day-out .cal-day-num");
    assertThat(at).as("달 밖 숫자색 규칙이 없다").isGreaterThan(0);
    String rule = css.substring(at, css.indexOf("}", at));
    assertThat(rule).contains("var(--color-base-content) 60%");
    // 레이어 밖: 규칙 앞쪽의 마지막 @layer 블록이 이미 닫혀 있어야 한다(열린 '{' 수 == 닫힌 '}' 수).
    String before = css.substring(0, at);
    long opens = before.chars().filter(ch -> ch == '{').count();
    long closes = before.chars().filter(ch -> ch == '}').count();
    assertThat(opens).as("규칙이 @layer 안에 있으면 text-error 유틸리티에 진다").isEqualTo(closes);
  }
}
