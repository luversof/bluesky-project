package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 시뮬레이터 월배당 표를 지급 시기(월중 &middot; 월말) &middot; 계좌(위탁 &middot; ISA/연금)로 좁힌다(사용자 요청 2026-09-23: "선택한
 * 것만 보여주도록").
 *
 * <p>사용자 결정 2026-09-23: 계좌는 적립 추천과 같은 규칙(과세표준 비중 10% 이내 위탁, 초과 ISA/연금). 실측(보유 8 종목): 월중 위탁 2
 * &middot; ISA/연금 2, 월말 위탁 2 &middot; ISA/연금 2.
 *
 * <p>JTE 조각은 부모가 넘겨 줘야 그린다 &mdash; 컨트롤러 &middot; 부모 &middot; 조각을 한 덩이로 본다. 소스는 공백을 누르고 본다.
 */
class SimulatorSlotFilterTest {

  private static final String CONTROLLER =
      "src/main/java/net/luversof/web/gate/stock/controller/StockViewController.java";

  private static final String PARENT = "src/main/jte/stock/simulator.jte";

  private static final String FRAGMENT =
      "src/main/jte/stock/fragments/monthlyDividendSimulator.jte";

  /**
   * 공백을 전부 지운다. 실측 2026-09-23: 서식 정리로 {@code model.addAttribute(} 뒤에서 줄이 접히자 공백을 하나로 누르는 비교는 깨졌다.
   * 비교하는 쪽도 {@link #norm} 으로 똑같이 지운다.
   */
  private static String read(String path) throws IOException {
    return norm(Files.readString(Path.of(path), StandardCharsets.UTF_8));
  }

  private static String norm(String text) {
    return text.replaceAll("\\s+", "");
  }

  @Test
  void 컨트롤러가_두_조건을_받아_거르고_화면에_넘긴다() throws IOException {
    String controller = read(CONTROLLER);
    assertThat(controller)
        .contains(
            norm("@RequestParam(required = false) String payoutWindow, @RequestParam(required = false) String account, @RequestParam(required = false) String symbol) {"))
        .contains(norm("positiveOnly, payoutWindow, account, symbol);"))
        .as("걸러낸 행이 정렬 · 합계 · 추천까지 이어진다")
        .contains(
            norm("keepSlotRows( monthlyDividendViewSupport.filterRows( allRows, monthlyDividendKeyword, minAnnualYield, positiveOnly), slotSymbols),"))
        .as("적립 추천과 같은 규칙 · 같은 카탈로그")
        .contains(
            norm("monthlyContributionPickSupport.symbolsInSlot( monthlyDividendPayoutWindowFilter, monthlyDividendAccountFilter, monthlyDividendCatalog)"))
        .contains(norm("loadContributionPicks(filteredRows, monthlyDividendCatalog)"))
        .as("카탈로그를 못 받았는데 조건이 걸렸으면 알린다 - 조용히 전체를 보이면 걸린 줄로 읽힌다")
        .contains(
            norm("boolean monthlyDividendSlotFilterUnavailable = slotFilterRequested && monthlyDividendCatalog == null;"))
        .contains(
            norm("model.addAttribute(\"monthlyDividendPayoutWindowFilter\", monthlyDividendPayoutWindowFilter);"))
        .contains(
            norm("model.addAttribute(\"monthlyDividendAccountFilter\", monthlyDividendAccountFilter);"))
        .contains(
            norm("model.addAttribute(\"monthlyDividendSlotFilterUnavailable\", monthlyDividendSlotFilterUnavailable);"));
  }

  @Test
  void 부모가_조각에_넘긴다() throws IOException {
    String parent = read(PARENT);
    assertThat(parent)
        .contains(norm("@param String monthlyDividendPayoutWindowFilter = \"\""))
        .contains(norm("@param String monthlyDividendAccountFilter = \"\""))
        .contains(norm("@param boolean monthlyDividendSlotFilterUnavailable = false"))
        .contains(norm("monthlyDividendPayoutWindowFilter = monthlyDividendPayoutWindowFilter,"))
        .contains(norm("monthlyDividendAccountFilter = monthlyDividendAccountFilter,"))
        .contains(norm("monthlyDividendSlotFilterUnavailable = monthlyDividendSlotFilterUnavailable,"));
  }

  @Test
  void 조각이_두_칸을_그리고_정렬_링크가_조건을_싣는다() throws IOException {
    String fragment = read(FRAGMENT);
    String form =
        fragment.substring(
            fragment.indexOf("data-monthly-filter-form>"),
            fragment.indexOf("</form>", fragment.indexOf("data-monthly-filter-form>")));
    assertThat(form)
        .as("필터 폼 안에 있어야 적용 단추로 함께 보낸다")
        .contains(
            norm("<select name=\"payoutWindow\" class=\"select select-bordered select-sm w-full max-w-full\" data-monthly-slot-window>"))
        .contains(
            norm("<select name=\"account\" class=\"select select-bordered select-sm w-full max-w-full\" data-monthly-slot-account>"))
        .contains(
            norm("<option value=\"MID_MONTH\" selected=\"${\"MID_MONTH\".equals(monthlyDividendPayoutWindowFilter)}\">"))
        .contains(
            norm("<option value=\"MONTH_END\" selected=\"${\"MONTH_END\".equals(monthlyDividendPayoutWindowFilter)}\">"))
        .contains(
            norm("<option value=\"BROKERAGE\" selected=\"${\"BROKERAGE\".equals(monthlyDividendAccountFilter)}\">"))
        .contains(
            norm("<option value=\"PENSION\" selected=\"${\"PENSION\".equals(monthlyDividendAccountFilter)}\">"));
    assertThat(fragment)
        .as("정렬 한 번에 조건이 풀리면 안 된다(SimulatorSortKeepsFilterTest 와 같은 까닭)")
        .contains(
            norm("+ (monthlyDividendPayoutWindowFilter.isBlank() ? \"\" : \"&payoutWindow=\" + monthlyDividendPayoutWindowFilter)"))
        .contains(
            norm("+ (monthlyDividendAccountFilter.isBlank() ? \"\" : \"&account=\" + monthlyDividendAccountFilter);"))
        .as("카탈로그를 못 받으면 조건을 못 건 것을 알린다")
        .contains(norm("@if(monthlyDividendSlotFilterUnavailable)"));
  }
}
