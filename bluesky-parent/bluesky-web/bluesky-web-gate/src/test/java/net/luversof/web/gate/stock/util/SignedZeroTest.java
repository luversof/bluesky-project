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
          // @if(x.signum() != 0) 블록 안이면 0 에서는 그 span 자체가 렌더되지 않는다 -
          // 삼항으로 거르는 것과 같은 효과다(2026-09-15: 종목별 기여의 합계·기타 줄이 이 꼴이 됐다).
          boolean insideNonZeroBranch = line.contains("@if(") && line.contains(".signum() != 0)");
          boolean zeroAware =
              line.contains(".signum() == 0 ?")
                  || line.contains(".signum() != 0 ?")
                  || insideNonZeroBranch
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

  /**
   * 짝이 빈 삼항({@code ? "+" : ""})도 0 을 거른다.
   *
   * <p>위 검사는 {@code ? "+" : "-"} 만 본다. 음수 부호를 포맷터에 맡기는 자리는 짝이 비어 있어 그 검사에 안 걸렸고, 판정이 {@code >= 0}
   * 이라 0 에도 "+" 가 붙었다.
   *
   * <p>실측 2026-09-15(종목 필터로 매도 0 건을 만들어): 매매 요약 카드가 <b>실현 손익 "+₩ 0"</b> 을 이득 색으로 찍고 바로 아래에 "매도 0건"
   * 이라 적었다. 같은 꼴이 주식 화면에 다섯 곳 있었다(매매 카드 · 매매 이력 카드 · 보유 스냅샷 합계와 줄 · 배당 원금 증감 비율).
   *
   * <p>판정은 <b>그 줄에서 보여야</b> 한다 - 다른 줄의 boolean 변수에 숨으면 줄 단위 검사가 닿지 않는다.
   */
  @Test
  void 짝이_빈_부호_삼항도_영을_거른다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    // 더하기뿐 아니라 빼기도 본다 - 어느 쪽이든 0 에 붙으면 방향을 지어낸 것이다.
    List<String> markers =
        List.of(
            "? " + (char) 34 + "+" + (char) 34 + " : " + (char) 34 + (char) 34,
            "? " + (char) 34 + "-" + (char) 34 + " : " + (char) 34 + (char) 34);
    // 주식 화면만 본다 - 다른 메뉴는 이 루프의 범위가 아니다.
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        for (String marker : markers) {
          int at = source.indexOf(marker);
          while (at >= 0) {
            int lineStart = source.lastIndexOf((char) 10, at) + 1;
            int lineEnd = source.indexOf((char) 10, at);
            String line = source.substring(lineStart, lineEnd < 0 ? source.length() : lineEnd);
            // 보는 것은 하나다 - **0 을 거르는가**. 빼기 쪽은 두 뜻이 있어 방향까지 강제하면 안 된다:
            //   손익이면 음수일 때 붙이고("< 0"), 차감 금액이면 뺄 것이 있을 때 붙인다("> 0").
            // 둘 다 0 은 제외하므로 규칙을 지킨다. 안 되는 것은 0 을 품는 ">= 0" / "<= 0" 이다.
            boolean zeroAware =
                (line.contains("> 0 ?") || line.contains("< 0 ?"))
                    && !line.contains(">= 0 ?")
                    && !line.contains("<= 0 ?");
            if (zeroAware) {
              guarded++;
            } else {
              offenders.add(
                  file.getFileName()
                      + ": "
                      + line.trim().substring(0, Math.min(70, line.trim().length())));
            }
            at = source.indexOf(marker, at + 1);
          }
        }
      }
    }
    assertThat(offenders).as("0 에 부호가 붙는 자리").isEmpty();
    // 이 꼴을 쓰는 자리가 사라지면 검사가 헛돈다 - 실제로 세고 있는지 확인한다.
    assertThat(guarded).as("주식 화면에서 이 꼴을 쓰는 자리").isGreaterThanOrEqualTo(5);
  }

  /**
   * 원화 기호 앞에 <b>박아 넣은</b> 부호도 0 을 거른다.
   *
   * <p>삼항이 아니라 글자로 적힌 부호는 위 두 검사가 닿지 않는다. 실측 2026-09-15(거래만 있고 배당이 없는 종목으로 좁혀): 배당 요약 카드의 '차감' 이
   * {@code -₩ 0} 이었다 &mdash; 뺄 것이 없는데 뺐다고 말한다.
   *
   * <p>뺄셈을 보여 주는 자리라 음수 기호 자체는 맞다(바로 아래 "세전 - (세금 + 수수료) = 세후" 와 한 벌이다). 0 일 때만 떼면 된다.
   */
  @Test
  void 박아_넣은_부호도_영을_거른다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    // 값 바로 앞에 글자로 붙은 부호: "-₩" / "-&#8361;" / "-&#x20A9;"
    List<String> markers = List.of("-" + (char) 0x20a9, "-&#8361;", "-&#x20A9;");
    try (Stream<Path> files = Files.walk(Path.of("src/main/jte/stock"))) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        for (String marker : markers) {
          int at = source.indexOf(marker);
          while (at >= 0) {
            int lineStart = source.lastIndexOf((char) 10, at) + 1;
            int lineEnd = source.indexOf((char) 10, at);
            String line = source.substring(lineStart, lineEnd < 0 ? source.length() : lineEnd);
            // 삼항의 어느 가지인가 - 마커 바로 앞이 물음표+따옴표(then)냐 콜론+따옴표(else)냐.
            // 물음표 뒤에 부호와 원화 기호를 통째로 넣는 꼴이 있어 낱말로는 못 가른다.
            String before = at >= 3 ? source.substring(at - 3, at) : "";
            boolean thenBranch = before.equals("? " + (char) 34);
            boolean elseBranch = before.equals(": " + (char) 34);
            // 바로 앞 물음표의 **조건**을 본다. 줄 아무 데나 "> 0" 이 있으면 통과시키면 안 된다
            // (실측 2026-09-15: 그렇게 두었더니 0 을 else 로 흘리는 변이를 놓쳤다).
            String upto = source.substring(lineStart, at);
            int q = upto.lastIndexOf((char) 63);
            String cond = q >= 0 ? upto.substring(Math.max(0, q - 60), q) : "";
            // then 가지에 음수 기호가 오면 조건이 "< 0" 이라야 0 이 흘러들지 않는다.
            // else 가지에 오면 조건이 ">= 0" 이라야 0 이 then 으로 빠진다.
            boolean zeroAware =
                (thenBranch && cond.contains("< 0") && !cond.contains("<= 0"))
                    || (elseBranch && cond.contains(">= 0"));
            if (zeroAware) {
              guarded++;
            } else {
              offenders.add(
                  file.getFileName()
                      + ": "
                      + line.trim().substring(0, Math.min(70, line.trim().length())));
            }
            at = source.indexOf(marker, at + 1);
          }
        }
      }
    }
    assertThat(offenders).as("0 에도 붙는 박힌 음수 기호").isEmpty();
    // 이 꼴을 쓰는 자리가 사라지면 검사가 헛돈다.
    assertThat(guarded).as("0 을 거르는 음수 기호 자리").isGreaterThanOrEqualTo(1);
  }

  /**
   * <b>브라우저 쪽</b>도 0 에 부호를 붙이지 않는다.
   *
   * <p>위 검사들은 모두 {@code src/main/jte} 만 걷는다. 화면의 절반은 브라우저에서 글자를 만드는데 그쪽은 검사 밖이었다 &mdash; 실측
   * 2026-09-15: 일곱 자리가 {@code >= 0} 으로 판정해 0 에 "+" 를 붙였다 (자산 성장 차트 축·툴팁 2 · 공용 차트 툴팁 4 · 활동 차트 툴팁 1
   * · 매매 선택 합산 1).
   *
   * <p>규칙은 이미 코드베이스에 있었다 &mdash; {@code compoundSimulator.ts} 가 "먼저 반올림한 수를 다시 찍는다" 고 적어 두고 {@code
   * > 0} 으로 가른다. 서버의 {@link net.luversof.web.gate.stock.util.StockFormatUtil#signedWon} 과 같은 규칙이다.
   * 일부만 지키고 있었을 뿐이다.
   *
   * <p><b>산출물까지 본다</b> &mdash; 브라우저가 도는 것은 {@code resources/static/js} 의 산출물이라, 원본만 고치고 빌드를 잊으면 화면은
   * 그대로다.
   */
  @Test
  void 브라우저_쪽도_영에는_부호를_붙이지_않는다() throws IOException {
    List<String> offenders = new ArrayList<>();
    int guarded = 0;
    // 값 뒤에 부호를 고르는 꼴: `x >= 0 ? "+"` / `x >= 0 ? '+'` (따옴표 두 가지를 다 본다).
    List<String> markers =
        List.of(">= 0 ? " + (char) 34 + "+", ">= 0 ? '+'", ">=0?" + (char) 34 + "+", ">=0?'+'");
    List<Path> roots =
        List.of(
            Path.of("src/main/frontend/src"),
            Path.of("src/main/resources/static/js"),
            Path.of("src/main/jte/stock"));
    for (Path root : roots) {
      if (!Files.exists(root)) {
        continue;
      }
      try (Stream<Path> files = Files.walk(root)) {
        for (Path file : files.filter(Files::isRegularFile).toList()) {
          String name = file.toString();
          boolean looksLikeScript =
              name.endsWith(".ts") || name.endsWith(".js") || name.endsWith(".jte");
          // poe 메뉴는 이 루프의 범위가 아니다. 벤더 번들도 우리 코드가 아니다.
          if (!looksLikeScript || name.contains("poe") || name.contains("vendor")) {
            continue;
          }
          String source = Files.readString(file, StandardCharsets.UTF_8);
          for (String marker : markers) {
            int at = source.indexOf(marker);
            while (at >= 0) {
              offenders.add(file.getFileName() + ": " + around(source, at));
              at = source.indexOf(marker, at + 1);
            }
          }
          // 0 을 거르는 꼴을 실제로 쓰고 있는지도 센다 - 안 그러면 이 검사가 헛돈다.
          for (String good :
              List.of("> 0 ? " + (char) 34 + "+", "> 0 ? '+'", ">0?" + (char) 34 + "+", ">0?'+'")) {
            int at = source.indexOf(good);
            while (at >= 0) {
              guarded++;
              at = source.indexOf(good, at + 1);
            }
          }
        }
      }
    }
    assertThat(offenders).as("브라우저 쪽에서 0 에 부호가 붙는 자리").isEmpty();
    assertThat(guarded).as("0 을 거르는 꼴을 쓰는 자리").isGreaterThanOrEqualTo(8);
  }

  /** 실패 메시지에 앞뒤를 조금 붙인다 - 자리만 알려 주면 어디를 고칠지 알기 어렵다. */
  private static String around(String source, int at) {
    int from = Math.max(0, at - 40);
    int to = Math.min(source.length(), at + 20);
    return source.substring(from, to).replace((char) 10, ' ').replace((char) 13, ' ').trim();
  }
}
