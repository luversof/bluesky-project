package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.util.StockFormatUtil;

/**
 * 금액을 원 단위로 찍을 때 버리지 않고 반올림하는지 본다.
 *
 * <p>{@code BigDecimal.longValue()} 는 0 방향으로 버린다. 그래서 나눗셈이 섞인 값에서 실제와 1 원 어긋나고, <b>음수는 손실이 작게</b>
 * 보인다 &mdash; 실측 2026-08-23 매매 화면: 실현손익(net) 61 행 중 23 행에 소수부가 있었고 그중 9 행이 다르게 찍혔다. 예를 들어 {@code
 * -8147.9999998918} 이 -8,147 로(정답 -8,148), {@code 1198069.999999998} 이 1,198,069 로(정답 1,198,070),
 * {@code 138557497.89861974} 가 138,557,497 로 나갔다.
 *
 * <p>표의 행과 합계가 서로 다른 규칙을 쓰면 사용자가 열을 더한 값이 합계와 맞지 않는다(배당 달력에서 실제로 2 원 어긋났다). 그래서 화면에 원 단위로 찍는 자리는 전부
 * {@link StockFormatUtil#displayWon} 하나로 모은다.
 */
class MoneyDisplayRoundingTest {

  private static final Path JTE_ROOT = Path.of("src/main/jte/stock");

  /** 금액을 원 단위로 찍는 호출. */
  private static final List<String> MONEY_FORMATS =
      List.of("String.format(\"%,d\"", "String.format(\"%+,d\"", "fullKrw(");

  private List<String> moneyLinesUsingLongValue() throws IOException {
    List<String> offenders = new ArrayList<>();
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".jte")).toList()) {
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 0; i < lines.size(); i++) {
          String line = lines.get(i);
          if (MONEY_FORMATS.stream().noneMatch(line::contains)) {
            continue;
          }
          if (!line.contains(".longValue()")) {
            continue;
          }
          // setScale 로 이미 반올림한 뒤의 longValue() 는 같은 규칙이므로 괜찮다.
          if (line.contains("RoundingMode.HALF_UP).longValue()")) {
            continue;
          }
          offenders.add(file.getFileName() + ":" + (i + 1));
        }
      }
    }
    return offenders;
  }

  @Test
  void 금액은_버리지_않고_반올림해서_찍는다() throws IOException {
    assertThat(moneyLinesUsingLongValue())
        .as(
            "BigDecimal.longValue() 는 0 방향 버림이라 1 원 어긋나고 음수는 손실이 작게 보인다."
                + " StockFormatUtil.displayWon(...) 을 쓸 것")
        .isEmpty();
  }

  /** 검사가 실제로 훑고 있는지. 파일을 하나도 못 읽으면 위 검사는 늘 통과한다. */
  @Test
  void 검사가_실제로_템플릿을_훑는다() throws IOException {
    long moneyLines;
    try (Stream<Path> files = Files.walk(JTE_ROOT)) {
      moneyLines =
          files
              .filter(p -> p.toString().endsWith(".jte"))
              .flatMap(
                  p -> {
                    try {
                      return Files.readAllLines(p, StandardCharsets.UTF_8).stream();
                    } catch (IOException e) {
                      throw new IllegalStateException(e);
                    }
                  })
              .filter(line -> MONEY_FORMATS.stream().anyMatch(line::contains))
              .count();
    }
    // 실측 2026-08-23: 금액을 찍는 줄 90 개.
    assertThat(moneyLines).isGreaterThan(60);
  }

  /** 규칙 자체. 0 방향 버림과 반올림이 실제로 갈리는 값들이다(실데이터에서 그대로 나온 수). */
  @Test
  void displayWon_은_0_방향_버림이_아니라_반올림이다() {
    assertThat(StockFormatUtil.displayWon(new BigDecimal("-8147.9999998918"))).isEqualTo(-8148L);
    assertThat(StockFormatUtil.displayWon(new BigDecimal("1198069.999999998"))).isEqualTo(1198070L);
    assertThat(StockFormatUtil.displayWon(new BigDecimal("138557497.89861974")))
        .isEqualTo(138557498L);
    assertThat(StockFormatUtil.displayWon(new BigDecimal("550128.5"))).isEqualTo(550129L);
    assertThat(StockFormatUtil.displayWon(new BigDecimal("102263.4"))).isEqualTo(102263L);
    assertThat(StockFormatUtil.displayWon(null)).isZero();
  }
}
