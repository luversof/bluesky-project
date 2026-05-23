package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.util.PlusMonthlyDividendPayoutSourceParser.PlusDividendPage;

class PlusMonthlyDividendPayoutSourceParserTest {

  private final PlusMonthlyDividendPayoutSourceParser parser =
      new PlusMonthlyDividendPayoutSourceParser(new ObjectMapper());

  private final MonthlyDividendPayoutImportParser importParser =
      new MonthlyDividendPayoutImportParser();

  @Test
  void convertsPlusApiJsonToBulkInput() {
    PlusDividendPage firstPage =
        parser.parsePage(
            """
						{
						  "content": [
						    {"wkdate":"20260515","dkdate":"20260519","dividend":"159","taxBase":"44"},
						    {"wkdate":"20260415","dkdate":"20260417","dividend":"163","taxBase":"163"}
						  ],
						  "last": true
						}
						""");

    String bulkInput = parser.toBulkInput(firstPage.content());
    List<MonthlyDividendPayoutUpsertRequest> requests = importParser.parse("0018C0", bulkInput);

    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 5, 19));
    assertThat(requests.get(0).getDividendAmountPerShare()).hasToString("159");
    assertThat(requests.get(0).getTaxableBasePerShare()).hasToString("44");
  }
}
