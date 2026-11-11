package net.luversof.api.stock.service.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import net.luversof.api.stock.service.analysis.InvestorFlowService.InvestorFlow;

class InvestorFlowServiceTest {

  private static Map<String, Object> row(String date, String foreign, String institution) {
    return Map.of(
        "stck_bsop_date", date, "frgn_ntby_tr_pbmn", foreign, "orgn_ntby_tr_pbmn", institution);
  }

  @Test
  void 기준일까지_최근_5거래일_합과_마지막_날() {
    List<Map<String, Object>> rows = new ArrayList<>();
    // KIS 는 최근 날짜가 먼저 - 순서와 무관하게 날짜로 정렬한다. 11-11 은 기준일 뒤라 빠진다.
    rows.add(row("20261111", "999", "999"));
    rows.add(row("20261110", "10", "-5"));
    rows.add(row("20261109", "-20", "5"));
    rows.add(row("20261106", "30", "5"));
    rows.add(row("20261105", "40", "5"));
    rows.add(row("20261104", "50", "5"));
    rows.add(row("20261103", "1000", "1000"));
    InvestorFlow flow = InvestorFlowService.flowOf(rows, LocalDate.of(2026, 11, 10));
    assertThat(flow.lastDate()).isEqualTo(LocalDate.of(2026, 11, 10));
    assertThat(flow.foreignNetBuy1d()).isEqualByComparingTo("10");
    assertThat(flow.foreignNetBuy5d()).isEqualByComparingTo("110");
    assertThat(flow.institutionNetBuy5d()).isEqualByComparingTo("15");
  }

  @Test
  void 장중_빈_행과_범위_밖_기준일은_null() {
    // 장중에는 당일 대금이 비어 온다.
    assertThat(InvestorFlowService.flowOf(List.of(row("20261111", "", "")), null)).isNull();
    assertThat(
            InvestorFlowService.flowOf(
                List.of(row("20261110", "1", "1")), LocalDate.of(2026, 9, 1)))
        .isNull();
  }
}
