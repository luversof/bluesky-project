package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 시뮬레이터 세 탭은 각자의 제목을 쓴다.
 *
 * <p>실측 2026-09-12: {@code ?tab=monthly-dividend} 에서 화면 제목(H1)과 브라우저 탭이 모두 <b>"지속가능성 시뮬레이터"</b> 였다
 * &mdash; 활성 탭은 "월배당" 인데 제목만 다른 탭의 것이었다. {@code ?tab=compound} 는 "적립식 복리 시뮬레이터" 로 제대로 나왔다. 월배당 분기가
 * {@code stock.page.simulator.title}(지속가능성) 을 그대로 쓴 탓이다.
 */
class SimulatorTabTitleTest {

  private static final Path TEMPLATE = Path.of("src/main/jte/stock/simulator.jte");

  @Test
  void 탭마다_다른_제목_키를_쓴다() throws IOException {
    String template = Files.readString(TEMPLATE, StandardCharsets.UTF_8);

    List<String> keys = new ArrayList<>();
    int at = template.indexOf("pageTitle = MessageUtil.getMessage(");
    while (at >= 0) {
      int start = template.indexOf((char) 34, at) + 1;
      keys.add(template.substring(start, template.indexOf((char) 34, start)));
      at = template.indexOf("pageTitle = MessageUtil.getMessage(", at + 1);
    }
    assertThat(keys).as("탭 셋의 제목 지정").hasSize(3);
    assertThat(keys).as("세 탭이 같은 제목을 쓰면 안 된다").doesNotHaveDuplicates();
    assertThat(keys).contains("stock.page.simulator.monthly.title");
  }

  /**
   * 관리 화면의 두 탭도 브라우저 탭에서 갈려야 한다.
   *
   * <p>실측 2026-09-12(13 화면 제목 전수): 같은 제목을 쓰는 화면은 {@code /stock/admin} 과 {@code
   * ?tab=monthly-reference} 뿐이었다(둘 다 "관리"). 화면 제목(H1)은 한 화면이라 그대로 두고 <b>문서 제목에만</b> 탭 이름을 덧붙인다.
   */
  @Test
  void 관리_문서_제목은_탭을_덧붙인다() throws IOException {
    String template =
        Files.readString(Path.of("src/main/jte/stock/admin.jte"), StandardCharsets.UTF_8);

    assertThat(template).contains("String adminPageTitle =");
    assertThat(template).as("문서 제목에만 쓴다").contains("pageTitle = adminPageTitle");
    assertThat(template)
        .as("화면 제목(H1)은 탭 이름을 붙이지 않는다")
        .contains("pageHeader(title = MessageUtil.getMessage(\"stock.page.admin.title\"))");
    assertThat(template).contains("stock.page.dividend.tab.monthly.reference");
    assertThat(template).contains("stock.admin.label.data.management");
  }

  @Test
  void 두_번들_모두_월배당_제목을_가진다() throws IOException {
    for (String bundle : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(
              Files.readString(
                  Path.of("src/main/resources").resolve(bundle), StandardCharsets.UTF_8))
          .as(bundle)
          .contains("stock.page.simulator.monthly.title");
    }
  }
}
