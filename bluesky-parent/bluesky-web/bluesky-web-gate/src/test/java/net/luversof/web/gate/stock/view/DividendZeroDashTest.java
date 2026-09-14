package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 배당 상세 목록의 "-" 는 두 뜻으로 읽힌다 &mdash; <b>0 원</b>과 <b>값 없음</b>.
 *
 * <p>실측 2026-09-11(전체 기간 461 행): 세금 "-" 147 개 &middot; 과세 금액 "-" 119 개인데 api-stock 의 {@code tax} 는
 * <b>null 이 하나도 없고 0 이 147 건</b>이다(= 전부 0 원). 같은 표의 기준 가격 &middot; 기준일 원금 "-" 5 개는 진짜로 값이 없는 경우이고
 * 그쪽에는 까닭 tooltip 이 붙어 있다.
 *
 * <p>0 을 숫자로 되돌리면 표가 0 으로 도배된다(2026-09-08 에 그 이유로 대시로 바꿨다 &mdash; {@code
 * DividendYieldFooterRenderTest}). 그래서 기호는 그대로 두고 <b>0 쪽 칸에만 까닭을 단다.</b>
 */
class DividendZeroDashTest {

  private static final Path TABLE =
      Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendTable.jte");

  private String read() throws IOException {
    return Files.readString(TABLE, StandardCharsets.UTF_8);
  }

  @Test
  void 영원_대시에는_까닭이_붙는다() throws IOException {
    String template = read();

    assertThat(template).contains("stock.dividend.zero.amount.title");
    assertThat(template).as("세금 칸").contains("item.tax().signum() == 0 ? zeroAmountTitle : null");
    assertThat(template)
        .as("과세 금액 칸")
        .contains("item.taxableAmount().signum() == 0 ? zeroAmountTitle : null");
  }

  @Test
  void 값은_예전_규칙_그대로_대시다() throws IOException {
    String template = read();

    // 0 을 숫자로 되돌리면 202 줄 중 147 줄이 0 으로 도배된다(앞 회차의 측정).
    assertThat(template)
        .contains(
            "item.tax() != null && item.tax().signum() != 0 ? decimalFormat.format(item.tax()) : ")
        .contains(
            "item.taxableAmount().signum() != 0 ? decimalFormat.format(item.taxableAmount()) : ");
  }

  @Test
  void 집계_표들도_같은_규칙을_쓴다() throws IOException {
    String analytics =
        Files.readString(
            Path.of("src/main/jte/stock/htmx/fragments/dividend/dividendYieldAnalytics.jte"),
            StandardCharsets.UTF_8);

    // 실측 2026-09-11(전체 기간): 연도별 · 종목별 · 계좌별 표에도 설명 없는 "-" 가 25 칸 있었고
    // 그중 19 칸(세금 12 · 과세 금액 7)이 0 원이었다.
    assertThat(analytics).contains("stock.dividend.zero.amount.title");
    String marker = "title=" + (char) 34 + "${";
    int titled = 0;
    for (int at = analytics.indexOf(marker); at >= 0; at = analytics.indexOf(marker, at + 1)) {
      titled++;
    }
    int zeroTitles = 0;
    String zeroMarker = "? zeroAmountTitle : null}";
    for (int at = analytics.indexOf(zeroMarker);
        at >= 0;
        at = analytics.indexOf(zeroMarker, at + 1)) {
      zeroTitles++;
    }
    // 칸마다 title 과 aria-label 을 함께 단다 - 마우스와 보조기술 양쪽(DashReasonAccessibleTest).
    assertThat(zeroTitles).as("세금·과세 금액 칸 여덟 곳 x (title + aria-label)").isEqualTo(16);
    assertThat(titled).as("title 속성 자체도 있어야 한다").isGreaterThan(0);
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(Files.readString(Path.of("src/main/resources/" + name), StandardCharsets.UTF_8))
          .contains("stock.dividend.zero.amount.title");
    }
  }
}
