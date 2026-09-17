package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 월배당 시뮬레이터 표의 과세표준 비중은 스냅샷에 저장된 값이라, 지급 이력이 갱신돼도 사용자가 다시 채우기 전까지 옛 값이다.
 *
 * <p>실측 2026-09-11(저장된 8 종목 전부): 저장값 對 지급 이력 계산값이 4.06/4.15 &middot; 4.29/4.45 &middot; 2.5/3.61
 * &middot; 0/26.66 &middot; 0.51/3.15 &middot; 0/32.05 &middot; 13.42/100.00 &middot; 17.35/81.16
 * 으로 여덟 종목 모두 달랐다(주당 배당은 여덟 종목 전부 정확히 일치). 세후 예상 배당이 그만큼 달라지는데 화면에는 아무 표시가 없었다.
 *
 * <p>계산 규칙은 관리 화면과 같은 {@code buildReferenceSummary} 하나만 쓴다(정의가 갈라지면 두 화면이 다른 값을 낸다).
 */
class MonthlyDividendReferenceRatioTest {

  private String read(String path) throws IOException {
    return Files.readString(Path.of(path), StandardCharsets.UTF_8);
  }

  @Test
  void 기준값은_관리화면과_같은_계산을_재사용한다() throws IOException {
    String support =
        read(
            "src/main/java/net/luversof/web/gate/stock/service/MonthlyDividendReferenceSupport.java");

    assertThat(support).contains("referenceTaxableRatioBySymbol");
    assertThat(support)
        .as("비중 정의는 관리 화면과 같은 buildReferenceSummary 하나여야 한다")
        .contains(
            "monthlyDividendCalculator.buildReferenceSummary(entry.getKey(), entry.getValue())");
    assertThat(support).contains("summary.averageTaxableBaseRatio1y()");
  }

  @Test
  void 컨트롤러가_종목별_기준값을_모델에_싣는다() throws IOException {
    String controller =
        read("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");

    assertThat(controller).contains("referenceTaxableRatioBySymbol()");
    assertThat(controller)
        .as("모델 속성 이름이 바뀌면 조각이 기본값(빈 맵)을 받아 경고가 통째로 사라진다 - 지역 변수 이름만 봐서는 못 잡는다")
        .contains(
            (char) 34
                + "monthlyDividendReferenceTaxableRatios"
                + (char) 34
                + ", monthlyDividendReferenceTaxableRatios");
  }

  @Test
  void 조각까지_인자로_전달된다() throws IOException {
    String page = read("src/main/jte/stock/simulator.jte");

    assertThat(page)
        .as("simulator.jte 는 인자를 하나씩 넘기므로 모델에 넣는 것만으로는 조각에 안 닿는다")
        .contains("monthlyDividendReferenceTaxableRatios = monthlyDividendReferenceTaxableRatios,");
    assertThat(page)
        .contains(
            "@param java.util.Map<java.util.UUID, BigDecimal> monthlyDividendReferenceTaxableRatios");
  }

  @Test
  void 차이가_1퍼센트포인트_이상일_때만_적는다() throws IOException {
    String fragment = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");
    int at = fragment.indexOf("referenceTaxableRatio != null");
    assertThat(at).as("차이 판정 조건이 있어야 한다").isGreaterThan(0);
    String condition = fragment.substring(at, fragment.indexOf((char) 10, at));

    assertThat(condition).contains("abs()");
    assertThat(condition)
        .as("같은 값까지 경고하면 여덟 줄이 모두 소음이 된다")
        .contains("compareTo(new BigDecimal(" + (char) 34 + "1" + (char) 34 + ")) >= 0");
    assertThat(fragment).contains("stock.simulator.monthly.table.cell.taxable.ratio.reference");
  }

  @Test
  void 힌트에서_고치러_갈_수_있다() throws IOException {
    String fragment = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

    // 알리기만 하면 어디서 고치는지 모른다 - 실측 2026-09-11: 이 표의 행에는 링크가 하나도 없었다.
    // 관리 화면은 symbol 로 그 종목을 골라 연다(실측: symbol=472150 이면 숨은 필드와 지급 주기가 그 종목 것으로 바뀐다).
    int at = fragment.indexOf("data-reference-taxable-ratio=");
    assertThat(at).isGreaterThan(0);
    String cell = fragment.substring(at, fragment.indexOf("</div>", at));
    assertThat(cell).contains("/stock/admin?tab=monthly-reference");
    assertThat(cell).contains("symbol=");
    assertThat(cell).contains("row.stockItemSymbol()");
  }

  @Test
  void 합계_카드도_이력_기준을_함께_보여_준다() throws IOException {
    String controller =
        read("src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java");
    String page = read("src/main/jte/stock/simulator.jte");
    String fragment = read("src/main/jte/stock/fragments/monthlyDividendSimulator.jte");

    // 실측 2026-09-11: 총 예상 월 과세표준액이 저장값 기준 220,539 인데 지급 이력 기준이면 994,375(4.5 배)였다.
    // 세금이 이 값에 붙으므로 행에만 알리고 합계는 조용하면 사용자는 합계를 현재 기준으로 읽는다.
    assertThat(controller).contains("monthlyDividendReferenceTaxableTotal");
    assertThat(controller)
        .as("차이가 1%p 이상인 종목만 센다 - 행 표기와 같은 기준")
        .contains("referenceRatio.subtract(savedRatio).abs().compareTo(BigDecimal.ONE) >= 0");
    assertThat(page)
        .as("조각이 인자로 받으므로 전달 줄이 없으면 기본값(0)이라 아무것도 안 뜬다")
        .contains("monthlyDividendReferenceTaxableTotal = monthlyDividendReferenceTaxableTotal,");
    // 색은 뜻에 따라 바뀐다(2026-09-17: 차이는 낡은 값이 아니라 기준 차이라 경고색을 뺐다) - 지키는 것은 금액 가리기다.
    assertThat(fragment)
        .as("금액이 들어가는 안내라 금액 가리기 대상이어야 한다")
        .contains("amount-value" + (char) 34 + " data-reference-taxable-total=");
    assertThat(fragment).contains("stock.simulator.monthly.summary.taxable.reference");
  }

  @Test
  void 두_번들_모두_문구를_가진다() throws IOException {
    for (String name : new String[] {"uiMessage.properties", "uiMessage_ko.properties"}) {
      assertThat(read("src/main/resources/" + name))
          .contains("stock.simulator.monthly.table.cell.taxable.ratio.reference");
      assertThat(read("src/main/resources/" + name))
          .contains("stock.simulator.monthly.table.cell.taxable.ratio.reference.link");
      assertThat(read("src/main/resources/" + name))
          .contains("stock.simulator.monthly.summary.taxable.reference");
    }
  }
}
