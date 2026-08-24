package net.luversof.web.gate.stock.httpexchange;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.DataFirstDateResponse;

/**
 * 사용자의 최초 데이터 일자(집계) 조회.
 *
 * <p>날짜 선택기 하한(minDate)을 구하려고 전체 거래/배당 이력을 내려받던 것을 대체한다.
 */
@HttpExchange(
    url = "/api/dataFirstDate",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface DataFirstDateClient {

  @GetExchange
  DataFirstDateResponse findDataFirstDate(@RequestParam UUID userId);
}
