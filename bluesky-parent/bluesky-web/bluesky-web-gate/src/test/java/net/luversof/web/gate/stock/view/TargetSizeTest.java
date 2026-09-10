package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 인터랙티브 목표는 24×24 CSS px 이상이거나 이웃과 24px 이상 떨어져 있다(WCAG 2.5.8).
 *
 * <p>실측 2026-09-10(qa/target-size-spacing.cjs, 375/1280px, 인라인·간격 예외 + 히트테스트): 실제 실패는 월배당 시뮬레이터 표
 * 머리의 세로로 쌓인 정렬 링크 4개(16px, 간격 4px → 중심 거리 20px)였다. 자산 현황·배당 수익률 표의 정렬/토글 버튼 41개({@code btn-xs
 * h-auto min-h-0}, 18px)는 간격 예외로 2.5.8 은 통과했지만 어떤 지침의 최소 크기(24px)에도 못 미쳐 함께 {@code min-h-6} 으로
 * 올렸다(첫 측정에서 이들이 겹친다고 본 것은 접힌 선택 패널의 버튼·고정 독을 이웃으로 잘못 센 오탐이었다).
 */
class TargetSizeTest {

  private static final Path JTE = Path.of("src/main/jte/stock");

  @Test
  void btn_xs_버튼은_최소_높이를_0으로_낮추지_않는다() throws IOException {
    Pattern small = Pattern.compile("class=\"[^\"]*\\bbtn-xs\\b[^\"]*\\bmin-h-0\\b[^\"]*\"");
    List<String> offenders = new ArrayList<>();
    int raised = 0;
    try (Stream<Path> walk = Files.walk(JTE)) {
      for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
        String html = Files.readString(p, StandardCharsets.UTF_8);
        Matcher m = small.matcher(html);
        while (m.find())
          offenders.add(
              p.getFileName() + ": " + m.group().substring(0, Math.min(70, m.group().length())));
        Matcher r = Pattern.compile("btn-xs h-auto min-h-6").matcher(html);
        while (r.find()) raised++;
      }
    }
    assertThat(raised).as("24px 로 올린 정렬/토글 버튼을 찾지 못했다").isGreaterThanOrEqualTo(41);
    assertThat(offenders).as("18px 버튼은 이웃 목표와 24px 안에 겹친다").isEmpty();
  }

  @Test
  void 월배당_시뮬레이터_표_머리의_쌓인_정렬_링크는_세로_여백을_가진다() throws IOException {
    String html =
        Files.readString(
            JTE.resolve("fragments/monthlyDividendSimulator.jte"), StandardCharsets.UTF_8);
    Matcher m =
        Pattern.compile("<a [^>]*class=\"link link-hover block[^\"]*text-xs[^\"]*leading-4[^\"]*\"")
            .matcher(html);
    int links = 0;
    List<String> flat = new ArrayList<>();
    while (m.find()) {
      links++;
      if (!m.group().contains("py-1"))
        flat.add(m.group().substring(0, Math.min(90, m.group().length())));
    }
    assertThat(links).as("쌓인 정렬 링크를 찾지 못했다").isGreaterThanOrEqualTo(4);
    assertThat(flat).as("16px 링크가 4px 간격으로 쌓이면 중심 거리 20px 다").isEmpty();
  }
}
