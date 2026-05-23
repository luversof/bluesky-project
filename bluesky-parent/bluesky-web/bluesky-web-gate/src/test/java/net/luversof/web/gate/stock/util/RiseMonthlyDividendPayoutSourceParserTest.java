package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;

class RiseMonthlyDividendPayoutSourceParserTest {

  private final RiseMonthlyDividendPayoutSourceParser parser =
      new RiseMonthlyDividendPayoutSourceParser();

  private final MonthlyDividendPayoutImportParser importParser =
      new MonthlyDividendPayoutImportParser();

  @Test
  void convertsRiseHtmlTableToBulkInput() {
    String bulkInput =
        parser.toBulkInput(
            """
						<div class="wrap_inner mt">
						  <div class="heading_area center">
						    <h3 class="heading03">분배금 지급현황</h3>
						  </div>
						</div>
						<div class="table_type02 center">
						  <table>
						    <thead>
						      <tr>
						        <th scope="col">지급기준일</th>
						        <th scope="col">실지급일</th>
						        <th scope="col">분배금액(원)</th>
						        <th scope="col">주당과세<br class="m_show">표준액(원)</th>
						      </tr>
						    </thead>
						    <tbody>
						      <tr>
						        <td>2026-04-15</td>
						        <td>2026-04-17</td>
						        <td>340</td>
						        <td>54</td>
						      </tr>
						      <tr>
						        <td>2026-03-13</td>
						        <td>2026-03-17</td>
						        <td>400</td>
						        <td>1</td>
						      </tr>
						    </tbody>
						  </table>
						</div>
						""");

    List<MonthlyDividendPayoutUpsertRequest> requests = importParser.parse("0094M0", bulkInput);

    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 4, 15));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 4, 17));
    assertThat(requests.get(0).getDividendAmountPerShare()).hasToString("340");
    assertThat(requests.get(0).getTaxableBasePerShare()).hasToString("54");
  }
}
