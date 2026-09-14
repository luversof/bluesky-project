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

  /**
   * 종목·계좌로 좁힌 최초 일자. 상세 화면의 '가장 이른 기간으로'(«) 가 쓸 하한이다.
   *
   * <p>사용자 전체의 최초일을 쓰면 그 종목이 아직 없던 창으로 뛴다 - 실측 2026-09-13: 삼성전자 최초 매매는 2020-03-04 인데 사용자 전체는
   * 2009-10-06 이다.
   */
  @GetExchange
  DataFirstDateResponse findDataFirstDate(
      @RequestParam UUID userId,
      @RequestParam(required = false) UUID stockItemId,
      @RequestParam(required = false) UUID accountId);
}
