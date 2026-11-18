package net.luversof.web.gate.stock;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import net.luversof.web.gate.stock.httpexchange.MonthlyDividendPayoutClient;
import net.luversof.web.gate.stock.httpexchange.StockAnalysisClient;

/**
 * 날짜 요청 매개변수는 ISO(2025-10-30)로 보낸다. 형식을 정하지 않으면 JVM 로캘(ko_KR)의 짧은 형식("25. 10. 30.")으로 나가 api-stock
 * 이 400 을 돌려준다(2026-11-15, 시황 분석 회차 비교에서 이전 날짜를 고르면 에러).
 */
class StockClientDateParamTest {

  private final Locale original = Locale.getDefault();

  @AfterEach
  void restoreLocale() {
    Locale.setDefault(original);
  }

  @Test
  void 날짜_매개변수는_로캘과_무관하게_ISO_로_보낸다() {
    Locale.setDefault(Locale.KOREA);
    RestClient.Builder builder = RestClient.builder().baseUrl("http://stock");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    HttpServiceProxyFactory factory =
        HttpServiceProxyFactory.builderFor(RestClientAdapter.create(builder.build())).build();
    StockAnalysisClient analysis = factory.createClient(StockAnalysisClient.class);
    MonthlyDividendPayoutClient dividend = factory.createClient(MonthlyDividendPayoutClient.class);

    server
        .expect(queryParam("date", "2025-10-30"))
        .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
    server
        .expect(queryParam("baseDate", "2025-10-30"))
        .andRespond(withSuccess("{\"deleted\":0,\"keptGraded\":0}", MediaType.APPLICATION_JSON));
    server
        .expect(queryParam("recordDate", "2025-10-30"))
        .andExpect(queryParam("payDate", "2025-11-05"))
        .andRespond(withSuccess());

    analysis.runs(LocalDate.of(2025, 10, 30), null);
    analysis.deletePredictions(LocalDate.of(2025, 10, 30));
    dividend.deletePayout("005930", LocalDate.of(2025, 10, 30), LocalDate.of(2025, 11, 5));
    server.verify();
  }
}
