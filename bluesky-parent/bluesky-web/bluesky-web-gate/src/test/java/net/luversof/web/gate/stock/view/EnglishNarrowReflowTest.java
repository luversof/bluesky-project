package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 영어 화면 320px · 글꼴 200% 넘침(자율 점검 2026-09-30). 한국어는 음절 사이에서 줄이 바뀌어 0 이었는데, 영어 낱말은 안 끊겨 줄을 못 바꾸는
 * 상자(단추 · 배지 · 줄바꿈 없는 flex 줄)가 화면을 밀어냈다 - 배당 138px · 대시보드 117px · 시뮬 월배당 83px · 지속가능성 36px · 복리
 * 26px · 자산 성장 10px.
 */
class EnglishNarrowReflowTest {

  private static final String NARROW_BUTTON =
      "max-sm:h-auto max-sm:min-h-0 max-sm:py-2 max-sm:whitespace-normal max-sm:break-keep max-sm:wrap-anywhere";

  private static String read(String path) throws IOException {
    return Files.readString(Path.of("src/main/jte/" + path), StandardCharsets.UTF_8);
  }

  @Test
  void 배당_화면_월배당_도구_단추는_좁은_폭에서_줄을_바꾼다() throws IOException {
    String source = read("stock/dividend.jte");
    assertThat(source.split("class=\"btn btn-sm btn-outline gap-1 " + NARROW_BUTTON + "\"", -1))
        .as("\"Monthly Dividend Reference Data\" · \"Dividend Simulator\" 두 단추(426 · 314px)")
        .hasSize(3);
  }

  @Test
  void 다가올_배당_머리는_좁은_폭에서_줄을_바꾼다() throws IOException {
    assertThat(read("stock/htmx/fragments/upcomingDividends.jte"))
        .contains("<div class=\"flex flex-wrap justify-between items-center gap-2 mb-2\">")
        .as("제목이 shrink-0 이면 \"Upcoming Dividends\"(301px)가 줄지 않는다")
        .contains("card-title text-base m-0 shrink-0 max-sm:shrink max-sm:min-w-0");
  }

  /**
   * 숨은 말풍선이 문서 가로 폭을 넓혔다(영어 배당 48px · 매매 51px) - 긴 영어 제목이 도움말 단추를 오른쪽으로 밀어 가운데 정렬 말풍선이 화면 밖. 숨어 있을
   * 땐 점(scale 0), 보일 때 1. 줄어드는 것은 visibility 와 같이 0.2s 뒤라 포인터가 틈을 건널 수 있다(WCAG 1.4.13).
   */
  @Test
  void 숨은_말풍선은_자리를_차지하지_않는다() throws IOException {
    String css =
        Files.readString(Path.of("src/main/frontend/main.css"), StandardCharsets.UTF_8)
            .replaceAll("\\s+", " ");
    assertThat(css)
        .contains("transition: opacity .12s, visibility 0s linear .2s, scale 0s linear .2s;")
        .contains("scale: var(--tooltip-hidden-scale, 0);")
        .contains("pointer-events: auto; transition-delay: 0s; scale: 1; }")
        .contains("pointer-events: none; scale: 0; }");
    // 최소화가 "scale: 0" 을 같은 규칙의 transform 에 scale(0) 으로 합쳐, 위치 변형(tooltip-bottom 등)이 transform 을 덮으면
    // 줄이기가 사라졌다(숨은 말풍선 288x176 그대로) - 배포본(산출물)에서 따로 남아 있는지 본다.
    String built =
        Files.readString(Path.of("src/main/resources/static/main.css"), StandardCharsets.UTF_8);
    assertThat(built)
        .contains("scale:var(--tooltip-hidden-scale,0)")
        .doesNotContain("translateY(-.25rem)scale(0)");
    // 숨김 기본 규칙에 pointer-events:none 이 있으면 hover 가 풀리는 순간 말풍선이 포인터를 못 받아, 0.25rem 틈을 건너 올라가도
    // hover 가 안 살아났다(실측 2026-09-30, WCAG 1.4.13 hoverable). 숨김은 0.2s 뒤 visibility 가 맡는다.
    int base = built.indexOf(".tooltip>.tooltip-content,.tooltip:before{");
    assertThat(base).as("말풍선 기본 규칙을 못 찾았다").isGreaterThanOrEqualTo(0);
    assertThat(built.substring(base, built.indexOf('}', base)))
        .contains("visibility 0s linear .2s")
        .doesNotContain("pointer-events:none");
  }

  @Test
  void 페이지_제목은_끊을_자리가_없어도_상자_안에_머문다() throws IOException {
    assertThat(read("_components/ui/pageHeader.jte"))
        .as("계좌 상세(영어 계좌 이름) 12px - min-w-0 만으로는 긴 낱말이 상자 밖으로 나간다")
        .contains("<h1 class=\"${size} font-bold wrap-anywhere\">${title}</h1>")
        .as("제목 줄이 안 접히면 제목이 0 폭으로 눌리고 오른쪽 동작이 화면을 민다")
        .contains(
            "<div class=\"flex flex-wrap items-start justify-between gap-x-4 gap-y-2 mb-3\">");
    assertThat(read("stock/htmx/fragments/detailNavSwitcher.jte"))
        .as("\"Other accounts\" 단추(글꼴 200% 300px 넘음) - 좁은 폭에서 줄을 바꾼다")
        .contains("<span class=\"whitespace-nowrap max-sm:whitespace-normal max-sm:break-keep\">");
  }

  @Test
  void 줄바꿈_없던_flex_줄과_배지() throws IOException {
    assertThat(read("stock/htmx/tradeHistory.jte"))
        .as("자산 성장 매매 내역 머리(\"263 items\" 배지)")
        .contains("<div class=\"flex flex-wrap items-center justify-between gap-2 mb-3\">");
    assertThat(read("stock/fragments/compoundSimulator.jte"))
        .as("복리 원금 · 수익 비율 범례(\"Cumulative Gain 14.4%\")")
        .contains(
            "<div class=\"flex flex-wrap justify-between gap-x-3 text-xs text-base-content/60\">");
    String simulator = read("stock/simulator.jte");
    assertThat(
            simulator.split(
                "<span class=\"badge max-sm:h-auto max-sm:whitespace-normal border-", -1))
        .as("지속가능성 단계 배지 다섯(\"Principal Drawdown\" 275px)")
        .hasSize(6);
    assertThat(read("stock/fragments/monthlyDividendSimulator.jte"))
        .as("시뮬 월배당 기준일 줄 - 날짜를 한 덩어리로 묶은 뒤 영어 라벨 + 날짜가 최소 폭이 됐다")
        .contains(
            "<div class=\"flex flex-wrap items-center justify-between gap-x-3 gap-y-1 text-sm\" data-as-of-row>")
        .as("우선 후보 줄(\"Priority Candidate\" + 종목)")
        .contains(
            "<div class=\"flex flex-wrap items-start justify-between gap-x-3 gap-y-1 text-sm\">");
  }
}
