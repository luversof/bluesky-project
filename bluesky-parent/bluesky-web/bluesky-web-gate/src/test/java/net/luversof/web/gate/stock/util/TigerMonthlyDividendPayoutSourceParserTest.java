package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;

class TigerMonthlyDividendPayoutSourceParserTest {

  private final TigerMonthlyDividendPayoutSourceParser parser =
      new TigerMonthlyDividendPayoutSourceParser();

  private final MonthlyDividendPayoutImportParser importParser =
      new MonthlyDividendPayoutImportParser();

  @Test
  void convertsTigerAjaxRowsToBulkInput() {
    String bulkInput =
        parser.toBulkInput(
            """
						<tr data-tot-cnt="29">
						    <td> 2026-04-30</td>
						    <td>2026-05-06</td>
						    <td>415</td>
						    <td>25</td>
						</tr>
						<tr data-tot-cnt="29">
						    <td>2026-03-31</td>
						    <td>2026-04-02</td>
						    <td>350</td>
						    <td>60</td>
						</tr>
						""");

    List<MonthlyDividendPayoutUpsertRequest> requests = importParser.parse("472150", bulkInput);

    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 5, 6));
    assertThat(requests.get(0).getDividendAmountPerShare()).hasToString("415");
    assertThat(requests.get(0).getTaxableBasePerShare()).hasToString("25");
  }
}
