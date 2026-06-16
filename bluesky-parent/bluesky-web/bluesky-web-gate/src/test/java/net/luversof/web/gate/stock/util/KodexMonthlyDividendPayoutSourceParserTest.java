package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.dto.request.MonthlyDividendPayoutUpsertRequest;
import net.luversof.web.gate.stock.util.KodexMonthlyDividendPayoutSourceParser.KodexDividendResponse;
import tools.jackson.databind.json.JsonMapper;

class KodexMonthlyDividendPayoutSourceParserTest {

  private final KodexMonthlyDividendPayoutSourceParser parser =
      new KodexMonthlyDividendPayoutSourceParser(JsonMapper.builder().build());

  private final MonthlyDividendPayoutImportParser importParser =
      new MonthlyDividendPayoutImportParser();

  @Test
  void convertsKodexApiJsonToBulkInput() {
    KodexDividendResponse response =
        parser.parseResponse(
            """
						{
						  "dividList": [
						    {"basicD":"20260515","payD":"20260519","dividA":"348","taxDividA":"0"},
						    {"basicD":"20260415","payD":"20260417","dividA":"262","taxDividA":"43"}
						  ]
						}
						""");

    String bulkInput = parser.toBulkInput(response.dividList());
    List<MonthlyDividendPayoutUpsertRequest> requests = importParser.parse("498400", bulkInput);

    assertThat(requests).hasSize(2);
    assertThat(requests.get(0).getRecordDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    assertThat(requests.get(0).getPayDate()).isEqualTo(LocalDate.of(2026, 5, 19));
    assertThat(requests.get(0).getDividendAmountPerShare()).hasToString("348");
    assertThat(requests.get(0).getTaxableBasePerShare()).hasToString("0");
  }
}
