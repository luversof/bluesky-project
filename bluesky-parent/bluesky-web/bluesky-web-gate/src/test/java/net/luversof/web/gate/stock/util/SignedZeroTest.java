package net.luversof.web.gate.stock.util;

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
 * 0 에는 부호를 붙이지 않는다.
 *
 * <p>{@code String.format("%+,d", 0)} 은 {@code +0} 이다. 부호는 방향을 말하는데 0 에는 방향이 없다 &mdash; "+0" 은 "0 원
 * 벌었다" 처럼 읽힌다. 실측 2026-09-11(10 화면): 부호 붙은 0 이 8 곳이었고 그중 7 곳이 <b>거래 이력이 없는 종목</b> 상세의 카드였다(합산 손익 ·
 * 평가 변동 · 실현 손익 · 기간 배당). 그 종목은 아무 일도 없었던 것이지 0 원을 번 것이 아니다.
 */
class SignedZeroTest {

  @Test
  void 영은_부호_없이_찍는다() {
    assertThat(StockFormatUtil.signedWon(0)).isEqualTo("0");
  }

  @Test
  void 영이_아니면_부호를_붙인다() {
    assertThat(StockFormatUtil.signedWon(1234)).isEqualTo("+1,234");
    assertThat(StockFormatUtil.signedWon(-1234)).isEqualTo("-1,234");
    assertThat(StockFormatUtil.signedWon(1)).isEqualTo("+1");
  }

  /** 템플릿이 옛 형식으로 되돌아가면 "+0" 이 다시 나간다. */
  @Test
  void 템플릿은_옛_형식을_쓰지_않는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (source.contains("String.format(" + (char) 34 + "%+,d" + (char) 34)) {
          offenders.add(file.getFileName().toString());
        }
      }
    }
    assertThat(offenders).as("부호 붙은 금액은 StockFormatUtil.signedWon 으로 찍는다").isEmpty();
  }

  /**
   * 인라인 삼항으로 부호를 찍는 자리도 0 을 거른다.
   *
   * <p>템플릿 7 개에 {@code ${flag ? "+" : "-"}₩${...abs()}} 꼴이 30 곳 있었다(자산 성장 · 기간별 · 종목 기여 · 연도별 비용 ·
   * 배당 요약 · 매매 구간). 이 꼴은 0 에서도 "+₩0" 을 낸다 &mdash; 실측 2026-09-11: 자산 성장에서 한 곳이 실제로 그렇게 나갔다.
   */
  @Test
  void 인라인_부호도_영을_거른다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        int at =
            source.indexOf(
                "? " + (char) 34 + "+" + (char) 34 + " : " + (char) 34 + "-" + (char) 34);
        while (at >= 0) {
          int lineStart = source.lastIndexOf((char) 10, at) + 1;
          String line =
              source.substring(
                  lineStart,
                  source.indexOf((char) 10, at) < 0
                      ? source.length()
                      : source.indexOf((char) 10, at));
          // 0 을 거르는 형태: .signum() == 0 ? "" / xxxZero ? "" / 항 사이 연산자(0 이면 그 줄 자체가 렌더되지 않는다)
          boolean zeroAware =
              line.contains(".signum() == 0 ?")
                  || line.contains(".signum() != 0 ?")
                  || line.contains("Zero ? " + (char) 34 + (char) 34)
                  || line.contains("Operator =");
          if (zeroAware) {
            guarded++;
          } else {
            offenders.add(
                file.getFileName()
                    + ": "
                    + line.trim().substring(0, Math.min(60, line.trim().length())));
          }
          at =
              source.indexOf(
                  "? " + (char) 34 + "+" + (char) 34 + " : " + (char) 34 + "-" + (char) 34, at + 1);
        }
      }
    }
    assertThat(guarded).as("2026-09-11 기준 30 곳").isGreaterThanOrEqualTo(30);
    assertThat(offenders).as("0 에서 부호가 나가는 자리").isEmpty();
  }

  /** 실제로 옮겨졌는지 - 호출이 하나도 없으면 위 검사는 늘 통과한다. */
  @Test
  void 템플릿이_새_형식을_쓴다() throws IOException {
    int calls = 0;
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        int at = source.indexOf("StockFormatUtil.signedWon(");
        while (at >= 0) {
          calls++;
          at = source.indexOf("StockFormatUtil.signedWon(", at + 1);
        }
      }
    }
    assertThat(calls).as("2026-09-11 기준 30 곳").isGreaterThanOrEqualTo(30);
  }
}
