package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 공용 금액 칸의 "-" 는 <b>0 원</b>이라는 뜻이고, 값이 아예 없는 칸과는 구분한다.
 *
 * <p>이 컴포넌트는 0 을 "-" 로 적는다(2026-09-07 결정: "₩0 으로 도배" 를 피하고 "그 해엔 아무 일도 없었다" 를 보이려고). 그런데 같은 기호를 값이
 * 없는 칸에도 쓰는 표가 있어(실측 2026-09-11, 배당 상세 목록) 뜻이 겹쳤다.
 *
 * <p>여기서는 <b>값이 실제로 있을 때만</b>(= 0 원) 짧은 까닭을 title 로 단다. {@code value == null} 이면 0 이라고 말할 근거가 없으므로
 * 그대로 둔다.
 */
class AmountCellZeroTitleTest {

  private static final Path COMPONENT = Path.of("src/main/jte/_components/ui/amountCell.jte");

  @Test
  void 영원_칸에만_까닭을_단다() throws IOException {
    String component = Files.readString(COMPONENT, StandardCharsets.UTF_8);

    assertThat(component).contains("common.amount.zero.title");
    assertThat(component)
        .as("null 이면 title 자체가 나가지 않아야 한다(JTE 스마트 속성)")
        .contains("title=\"${value != null ? amountCellZeroTitle : null}\"");
  }

  @Test
  void 값_표기_규칙은_그대로다() throws IOException {
    String component = Files.readString(COMPONENT, StandardCharsets.UTF_8);

    // 0 을 숫자로 되돌리면 "아무 일도 없던 해" 가 ₩0 으로 도배된다(앞 회차의 측정).
    assertThat(component)
        .contains("boolean amountCellBlank = zeroAsDash && amountCellValue.signum() == 0;");
  }

  @Test
  void 컴포넌트를_안_쓰는_표들도_같은_까닭을_단다() throws IOException {
    // 실측 2026-09-11: 이 두 조각은 공용 금액 칸을 쓰지 않고 직접 "-" 를 찍는다
    // (매매 월별 쪼갬 · 종목별 기여). 0 을 뜻하는 대시에 같은 문구를 단다.
    for (String rel :
        new String[] {
          "src/main/jte/stock/htmx/fragments/trade/tradePeriodBreakdown.jte",
          "src/main/jte/stock/htmx/fragments/stockContributionTable.jte"
        }) {
      String template = Files.readString(Path.of(rel), StandardCharsets.UTF_8);
      assertThat(template).as(rel).contains("common.amount.zero.title");
      assertThat(template).as(rel).contains("title=\"${zeroAmountTitle}\"");
    }
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(Files.readString(Path.of("src/main/resources/" + name), StandardCharsets.UTF_8))
          .contains("common.amount.zero.title");
    }
  }
}
