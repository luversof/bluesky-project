package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 차트의 날짜 라벨이 서버가 집계에 쓴 타임존을 따르는지 본다.
 *
 * <p>시계열 지점의 {@code timestamp} 는 <b>그 타임존의 자정</b>을 가리키는 instant 다(KST 2026-01-01 → {@code
 * 2025-12-31T15:00:00Z}). {@code toLocaleDateString()} 을 타임존 없이 부르면 브라우저 로컬로 렌더되어 KST 밖에서는 라벨이 하루씩
 * 앞으로 밀린다.
 *
 * <p>실측(같은 instant 를 타임존별로 렌더):
 *
 * <pre>
 *   2025-12-31T15:00:00Z → 서울 2026-01-01 / UTC·뉴욕·런던 2025-12-31
 *   2026-08-21T15:00:00Z → 서울 2026-08-22 / UTC·뉴욕·런던 2026-08-21
 * </pre>
 *
 * <p>연도 경계에서는 <b>해가 바뀐다</b>. 게이트는 이미 요청에 {@code timeZone} 을 실어 보내므로 라벨도 같은 값을 써야 한다.
 */
class ChartLabelTimeZoneTest {

  private static final Path CHART = Path.of("src/main/frontend/src/stock/timeSeriesChart.ts");
  private static final Path BUILT =
      Path.of("src/main/resources/static/js/stock/timeSeriesChart.js");

  private String read(Path path) throws IOException {
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  /**
   * 주석을 뺀 코드만 돌려준다.
   *
   * <p>함정을 설명하는 주석에 금지 표현이 그대로 들어 있어, 파일 전체를 훑으면 설명 때문에 실패한다.
   */
  private String codeOnly(Path path) throws IOException {
    StringBuilder code = new StringBuilder();
    for (String line : read(path).split("\\R")) {
      String trimmed = line.strip();
      if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
        continue;
      }
      code.append(line).append('\n');
    }
    return code.toString();
  }

  @Test
  void 라벨은_타임존_없이_렌더하지_않는다() throws IOException {
    String source = codeOnly(CHART);
    assertThat(source)
        .as("타임존 없이 부르면 브라우저 로컬을 따라 KST 밖에서 하루 밀린다")
        .doesNotContain("toLocaleDateString()");
    assertThat(source)
        .as("서버가 집계에 쓴 타임존을 라벨에도 써야 한다")
        .contains("resolveLabelZone")
        .contains("timeZone: zone");
  }

  @Test
  void 요청의_타임존을_라벨로_넘긴다() throws IOException {
    assertThat(read(CHART))
        .as("params.timeZone 을 넘기지 않으면 고쳐도 쓰이지 않는다")
        .contains("params?.timeZone");
  }

  /** 알 수 없는 타임존이 와도 예외로 화면이 죽지 않아야 한다. */
  @Test
  void 알_수_없는_타임존은_폴백한다() throws IOException {
    assertThat(read(CHART)).contains("catch");
  }

  /** 빌드 산출물에도 반영돼 있어야 실제 화면이 바뀐다. */
  @Test
  void 빌드된_번들에도_반영되어_있다() throws IOException {
    assertThat(read(BUILT)).as("소스만 고치고 번들을 다시 만들지 않으면 화면은 그대로다").contains("resolveLabelZone");
  }
}
