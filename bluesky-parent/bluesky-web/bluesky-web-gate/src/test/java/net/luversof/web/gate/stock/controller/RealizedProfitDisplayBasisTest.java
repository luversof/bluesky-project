package net.luversof.web.gate.stock.controller;

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
 * 화면에 적는 실현손익의 기준을 한 가지로 고정한다.
 *
 * <p>api-stock 은 두 값을 함께 준다 &mdash; 매도 거래에 기록된 값(증권사 기준, {@code realizedProfit})과 앱이 평균단가로 다시 계산한 값
 * ({@code realizedProfitNet}). 실측 2026-08-23 기준 두 값은 225,584,549 와 225,330,995.90 으로 253,553 원 다르고,
 * 종목 단위로는 28 종목이 달랐다(최대 단일 종목 192,303 원).
 *
 * <p>표시는 <b>기록값</b>으로 통일한다. 거래 목록의 각 행이 기록값을 그대로 찍기 때문에, 헤드라인만 계산값을 쓰면 같은 화면 안에서 합이 맞지 않는다. 예전에 매매
 * 화면이 그랬고({@code TradeProfitStockRealizedTest} 참고), 그때 고치지 않은 요약·종목 상세·계좌 상세가 같은 형태로 남아 있었다.
 *
 * <p>매도 54 건 전부 기록값이 있어(실측) 기록값을 써도 잃는 값이 없다.
 *
 * <p>다만 기록값은 <b>계좌를 합친</b> 원가를 따른다 &mdash; 실측 2026-08-23: 매도 54 건 중 종목 단위 원가로 50 건(92%)이 재현되고 계좌x종목
 * 단위로는 38 건(70%)뿐이다. 그래서 계좌별 배분은 그 계좌의 실제 성과와 다를 수 있고(연금저축1 415,053 vs 2,063,739, ISA 1,555,597 vs
 * 14,921), 그 사실을 <b>보조로</b> 밝히는 것은 이 검사가 막으려는 것이 아니다({@code ...OwnBasis} 이름만 허용).
 *
 * <p>소스를 읽는 이유: 어느 값을 고르는지가 메서드 본문에 있어 리플렉션으로는 보이지 않는다.
 */
class RealizedProfitDisplayBasisTest {

  /** 표시 값을 고르는 컨트롤러들. */
  private static final List<Path> DISPLAY_CONTROLLERS =
      Stream.of(
              "StockSummaryHtmxController",
              "StockViewController",
              "StockTradeHtmxController",
              "StockAssetGrowthHtmxController")
          .map(
              name ->
                  Path.of("src/main/java/net/luversof/web/gate/stock/controller/" + name + ".java"))
          .toList();

  /**
   * 계산값을 그대로 옆으로 넘기기만 하는 자리(레코드 복사 인자)는 표시 선택이 아니다.
   *
   * <p>이 구분을 안 하면 포트폴리오의 {@code s.realizedProfitNet(),} 같은 인자 나열까지 위반으로 잡혀 검사가 잔소리가 된다.
   */
  private boolean isRecordArgument(String line) {
    String trimmed = line.trim();
    return trimmed.endsWith("ProfitNet(),");
  }

  /**
   * 계산값을 <b>대안 기준으로 명시해 따로 보여 주는</b> 자리.
   *
   * <p>이 검사는 "표시 값을 계산값으로 바꾸는 것" 을 막으려는 것이지, 계산값을 화면에서 영영 못 쓰게 하려는 것이 아니다. 기록값은 계좌를 합친 원가를 따르므로 계좌별
   * 배분이 그 계좌의 실제 성과와 크게 다를 수 있다 &mdash; 실측 2026-08-23: 연금저축1 은 기록 415,053 원인데 그 계좌 매매만으로는 2,063,739
   * 원이고, ISA 는 1,555,597 대 14,921 이다. 그 사실을 밝히려면 계산값을 <b>보조로</b> 함께 적어야 한다.
   *
   * <p>그래서 이름이 {@code ...OwnBasis} 인 변수에 담는 경우만 허용한다. 표시 값 자체를 바꾸는 것은 이름이 다르므로 여전히 잡힌다.
   *
   * <p>포매터가 긴 대입을 줄바꿈하면 변수 이름이 <b>앞 줄</b>에 남는다. 그래서 바로 앞 줄까지 함께 본다(그 줄만 보면 이 예외가 영영 걸리지 않아, 예외를
   * 넓히려다 오히려 검사를 못 지나가는 상태가 된다 &mdash; 실제로 그렇게 한 번 막혔다).
   */
  private boolean isAlternativeBasisDisclosure(List<String> lines, int index) {
    if (lines.get(index).contains("OwnBasis")) {
      return true;
    }
    return index > 0 && lines.get(index - 1).contains("OwnBasis");
  }

  @Test
  void 표시하는_손익은_한_가지_기준을_쓴다() throws IOException {
    List<String> violations = new ArrayList<>();
    int scanned = 0;
    for (Path controller : DISPLAY_CONTROLLERS) {
      assertThat(controller).exists();
      List<String> lines = Files.readAllLines(controller, StandardCharsets.UTF_8);
      scanned++;
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        if (!line.contains("realizedProfitNet") && !line.contains("evaluationProfitNet")) {
          continue;
        }
        if (line.trim().startsWith("//")
            || isRecordArgument(line)
            || isAlternativeBasisDisclosure(lines, i)) {
          continue;
        }
        violations.add(controller.getFileName() + ":" + (i + 1) + " " + line.trim());
      }
    }

    assertThat(scanned).as("컨트롤러를 하나도 읽지 못했다").isEqualTo(DISPLAY_CONTROLLERS.size());
    assertThat(violations)
        .as(
            "화면에 적는 손익은 기본값(realizedProfit / evaluationProfit)으로 통일한다."
                + " 계산값(*Net)을 섞으면 거래 행 합계와 253,553 원, 실현+평가=총 항등식이 24,986 원 어긋난다")
        .isEmpty();
  }

  /**
   * 화면의 보유 행도 헤드라인과 같은 기준을 써야 한다.
   *
   * <p>두 기준은 각각 닫힌 삼중항이다 &mdash; 기록실현+기본평가={@code totalProfit}, Net실현+Net평가={@code totalProfitNet}.
   * 섞으면 "실현 + 평가 = 총" 이 깨진다(실측 2026-08-23: 61 행 중 18 행, 평가 기준차 합 24,986 원 &mdash; 이 값은 보유분에 남아 있는
   * 매수 수수료와 <b>정확히</b> 같다. 원장 재계산으로 잔차 0.00 확인, {@code StockProfitBasisUtil} 참고).
   */
  @Test
  void 상세_화면의_보유_행도_같은_기준을_쓴다() throws IOException {
    for (String template : List.of("accountDetail.jte", "stockItemDetail.jte")) {
      String source =
          Files.readString(Path.of("src/main/jte/stock/" + template), StandardCharsets.UTF_8);
      assertThat(source)
          .as(template + " 의 보유 행이 헤드라인과 다른 기준을 쓴다")
          .doesNotContain("evaluationProfitNet");
      assertThat(source)
          .as(template + " 에서 평가손익 행을 찾지 못했다 - 검사가 무력하다")
          .contains("holding.evaluationProfit()");
    }
  }

  /** 거래 행이 기록값을 찍는다는 전제 자체를 확인한다. 이게 깨지면 위 규칙의 근거가 사라진다. */
  @Test
  void 거래_행은_기록값을_찍는다() throws IOException {
    String template =
        Files.readString(Path.of("src/main/jte/stock/stockItemDetail.jte"), StandardCharsets.UTF_8);
    assertThat(template)
        .as("거래 행이 기록값을 찍지 않으면 헤드라인 기준을 다시 정해야 한다")
        .contains("trade.realizedProfit()");
    assertThat(template).doesNotContain("trade.realizedProfitNet()");
  }
}
