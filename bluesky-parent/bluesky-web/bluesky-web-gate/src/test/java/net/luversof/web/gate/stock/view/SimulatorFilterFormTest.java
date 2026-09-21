package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.StockControllerSources;

/**
 * 월배당 시뮬레이터 표를 화면에서 좁힐 수 있다(사용자 선택 2026-09-21: "검색·필터를 화면으로").
 *
 * <p>서버는 오래전부터 {@code keyword} · {@code minAnnualYield} · {@code positiveOnly} 를 받았지만 화면에는 입력 칸이
 * 하나도 없어 주소를 직접 쳐야만 걸렸다 &mdash; 실측 2026-09-21: {@code name="keyword"} 입력 요소 0 개.
 *
 * <p>이 표는 <b>보유 종목</b>만 다룬다. 안 가진 월배당 ETF 는 별도 화면({@code /stock/monthly-etf})이 맡는다.
 */
class SimulatorFilterFormTest {

  private static final String FRAGMENT =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";

  @Test
  void 검색과_필터가_화면에_있다() throws IOException {
    // 주석에도 name="keyword" 가 적혀 있다 - 주석을 지우고 '입력 요소' 로 본다(복사본 변이 2026-09-21 이 이 틈으로 통과했다).
    String template = withoutComments(Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8));

    assertThat(template).as("종목 검색").contains("<input name=\"keyword\" type=\"search\"");
    assertThat(template)
        .as("최소 연배당 수익률")
        .contains("<input name=\"minAnnualYield\" type=\"number\"");
    assertThat(template)
        .as("합산 수익률 양수만")
        .contains("<input name=\"positiveOnly\" type=\"checkbox\"");
    assertThat(template)
        .as("GET 폼이라 탭을 함께 지켜야 다른 탭으로 가지 않는다")
        .contains("<input type=\"hidden\" name=\"tab\" value=\"monthly-dividend\" />");
    assertThat(template)
        .as("정렬 상태도 지켜야 필터를 걸 때 표 순서가 되돌아가지 않는다")
        .contains("<input type=\"hidden\" name=\"sort\" value=\"${monthlyDividendSort}\" />");

    String controller = StockControllerSources.all();
    assertThat(controller)
        .as("컨트롤러가 이미 받던 파라미터를 화면이 쓴다")
        .contains("@RequestParam(required = false) String keyword")
        .contains("@RequestParam(defaultValue = \"false\") boolean positiveOnly");
  }

  /** 이 표는 보유 종목만 보여 준다 - 미보유 종목 비교는 월배당 ETF 화면이 맡는다(사용자 요청 2026-09-21). */
  @Test
  void 보유_수량_없는_행은_저장할_수_없다() throws IOException {
    String template = Files.readString(Path.of(FRAGMENT), StandardCharsets.UTF_8);
    int quantityAt = template.indexOf("<input name=\"heldQuantity\"");
    assertThat(quantityAt).as("보유 수량 입력이 없다").isGreaterThan(0);
    String quantityInput = template.substring(quantityAt, template.indexOf(">", quantityAt));
    assertThat(quantityInput).as("보유 수량은 1 주부터").contains("min=\"1\"").contains("required");

    assertThat(StockControllerSources.all())
        .as("서버도 0 을 막는다 - 미보유 종목은 이 표에 올리지 않는다")
        .contains("request.getHeldQuantity() == null || request.getHeldQuantity() <= 0");
  }

  /** JTE 주석({@code <%-- --%>})을 지운다 - 주석 속 예시가 판정을 만족시키면 안 된다. */
  private static String withoutComments(String template) {
    return template.replaceAll("(?s)<%--.*?--%>", "");
  }

  /** 두 화면이 서로를 가리킨다 - 성격이 다른 곳으로 건너가는 길. */
  @Test
  void 월배당_ETF_화면과_서로_잇는다() throws IOException {
    assertThat(
            Files.readString(Path.of("src/main/jte/stock/monthlyEtf.jte"), StandardCharsets.UTF_8))
        .as("ETF 목록에서 내 배당 시뮬레이터로")
        .contains("/stock/simulator?tab=monthly-dividend");
  }
}
