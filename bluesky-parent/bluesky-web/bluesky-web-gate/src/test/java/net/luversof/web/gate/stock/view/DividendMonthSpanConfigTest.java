package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월평균의 분모는 기간 길이(월 환산)다 - 그러려면 조각이 날짜를 실어 보내야 한다.
 *
 * <p>실측 2026-09-11(배당 화면, 실데이터): 걸친 달력 월 수로 나누던 때 1개월 프리셋(2026-08-12~09-11, 31일)이 2개월로 나뉘어 월평균이
 * 2,428,796 원이었다. 실제 기간은 1.01 개월이고 고친 뒤 값은 4,800,808 원이다(+98%). 3개월 92일은 4개월(-25%), 6개월 184일은
 * 7개월(-14%), 12개월 365일은 13개월(-8%)로 나뉘고 있었다.
 *
 * <p>설정에서 {@code filterStartDay}/{@code filterEndDay} 가 빠지면 스크립트가 조용히 옛 분모(달력 월 수)로 되돌아간다.
 */
class DividendMonthSpanConfigTest {

  private static final Path TABS =
      Path.of("src/main/jte/stock/htmx/fragments/tabsDividendHistory.jte");
  private static final Path BUILT =
      Path.of("src/main/resources/static/js/stock/dividendHistory.js");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 조각이_기간의_시작일과_종료일을_싣는다() throws IOException {
    String template = read(TABS);

    assertThat(template).contains("filterStartDay:");
    assertThat(template).contains("filterEndDay:");
    assertThat(template)
        .as("달(yyyy-MM)이 아니라 날짜여야 한다")
        .contains("startLocal != null ? startLocal.toString()");
  }

  @Test
  void 빌드된_스크립트가_일수_가중_분모를_쓴다() throws IOException {
    String built = read(BUILT);

    assertThat(built)
        .as("메이븐은 프론트엔드를 빌드하지 않는다 - npm run build 산출물이 배포본이다")
        .contains("monthEquivalent");
    assertThat(built).contains("averageMonthSpan");
  }
}
