package net.luversof.web.gate.stock.httpexchange;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.stock.dto.request.TaxYearInputUpsertRequest;
import net.luversof.web.gate.stock.dto.response.DividendTaxReport;

/** 배당 종소세 신고 자료와 그 계산에 쓰는 사용자 입력값. */
@HttpExchange(
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface DividendTaxReportClient {

  @GetExchange("/api/dividendTaxReport")
  DividendTaxReport report(@RequestParam UUID userId, @RequestParam int year);

  @PostExchange("/api/taxYearInput")
  void saveInput(@RequestBody TaxYearInputUpsertRequest request);
}
