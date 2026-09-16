package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 압축 표기는 반올림이 다음 단위에 닿으면 그 단위로 올린다.
 *
 * <p>압축 표기는 자릿수를 줄이려고 쓰는 것이라, 경계에서 한 단위 아래 표기가 남으면 목적을 정확히 어긴다. 이 규칙은 이미 두 번 정해졌다 &mdash; 서버({@code
 * StockFormatUtil.compactWestern}: 999,999 → "1,000K")와 차트 축({@code stock-charts.ts}: 99,999,999 →
 * "10,000만").
 *
 * <p>그런데 <b>세 번째 사본</b>이 있었다. 실측 2026-09-15: 지속가능성 시뮬레이터의 시나리오 배지가 원금 99,999,999 를 "₩10000만"(ko),
 * 999,999,999 를 "₩1000M"(en) 으로 찍었다 &mdash; 같은 화면의 차트 축은 같은 수를 "1억" 으로 찍고 있었다. 규칙을 아는 파일이 둘인데 규칙을
 * 어기는 파일이 하나 더 있던 것이라, 여기서는 <b>사본마다 승격 규칙을 갖고 있는지</b>를 본다.
 *
 * <p>원본과 산출물을 함께 본다 &mdash; 원본만 고치고 빌드를 잊으면 브라우저는 그대로다.
 */
class CompactUnitPromotionTest {

  /** 압축 표기가 올라가야 하는 경계. 원본(10000)과 산출물(1e4) 표기를 둘 다 적는다. */
  private static final List<String[]> BOUNDARIES =
      List.of(
          new String[] {"만→억", "promote(10000,100000000,", "promote(1e4,1e8,"},
          new String[] {"K→M", "promote(1000,1000000,", "promote(1e3,1e6,"},
          new String[] {"M→B", "promote(1000000,1000000000,", "promote(1e6,1e9,"});

  @Test
  void 압축_표기_사본마다_승격_규칙이_있다() throws IOException {
    List<String> offenders = new ArrayList<>();
    List<String> compactFiles = new ArrayList<>();
    int guarded = 0;
    // 걷는 자리가 곧 가드의 범위다. 실측 2026-09-15: 사본 둘이 **JTE 인라인 스크립트**에 있었고
    // 둘 다 승격이 빠져 있었다(대시보드 축 · 활동 축). 그래서 .jte 도 걷는다.
    List<Path> roots =
        List.of(
            Path.of("src/main/jte"),
            Path.of("src/main/frontend/src"),
            Path.of("src/main/resources/static/js"));
    for (Path root : roots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p : walk.filter(Files::isRegularFile).toList()) {
          String name = p.toString().replace('\\', '/');
          if (!name.endsWith(".ts") && !name.endsWith(".js") && !name.endsWith(".jte")) {
            continue;
          }
          if (name.contains("vendor") || name.contains("poe")) {
            continue;
          }
          String source = Files.readString(p, StandardCharsets.UTF_8);
          // 빌드가 공백을 지우므로 공백을 눌러 비교한다.
          String squeezed = source.replaceAll("\\s+", "");
          if (!makesCompactUnits(squeezed)) {
            continue;
          }
          compactFiles.add(name);
          for (String[] boundary : BOUNDARIES) {
            if (squeezed.contains(boundary[1]) || squeezed.contains(boundary[2])) {
              guarded++;
            } else {
              offenders.add(name + ": " + boundary[0] + " 경계에 승격이 없다");
            }
          }
        }
      }
    }

    assertThat(offenders).as("압축 표기를 만드는데 승격 규칙이 없는 자리").isEmpty();
    // 원본 2 + 산출물 2. 사본이 사라지면 검사가 헛돈다.
    assertThat(compactFiles).as("압축 표기를 만드는 파일").hasSizeGreaterThanOrEqualTo(4);
    assertThat(guarded).as("확인한 (파일 x 경계)").isGreaterThanOrEqualTo(12);
  }

  /**
   * 이 파일이 압축 표기를 <b>만드는가</b>.
   *
   * <p>단위 글자만 보면 주석에 "기억해" 가 든 파일까지 걸린다(실측: common.ts). 억 단위를 <b>만드는 나눗셈</b>이 함께 있어야 한다. 산출물은 한글을
   * {@code \\uC5B5} 로 이스케이프하고 수를 {@code 1e8} 로 줄이므로 두 표기를 다 본다.
   */
  private static boolean makesCompactUnits(String squeezed) {
    boolean hasEokLabel =
        squeezed.contains("억") || squeezed.toLowerCase(java.util.Locale.ROOT).contains("\\uc5b5");
    boolean dividesToEok = squeezed.contains("/100000000") || squeezed.contains("/1e8");
    return hasEokLabel && dividesToEok;
  }
}
