package net.luversof.web.gate.stock.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.YearlyCostSummary;

/** 연도별 세금·비용 요약. 합계는 서버가 낸다(원장 전체를 받아 더하면 응답이 원장 크기를 따라간다). */
@HttpExchange(
    url = "/api/yearlyCost",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface YearlyCostClient {

  @GetExchange
  List<YearlyCostSummary> findYearlyCost(@RequestParam MultiValueMap<String, String> params);
}
