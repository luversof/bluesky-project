package net.luversof.web.gate.stock.httpexchange;

import java.time.Instant;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.ActivityFilterIdsResponse;

/**
 * 필터 드롭다운에 쓸 '해당 기간에 등장한' 계좌·종목 id 조회.
 *
 * <p>예전에는 전체 거래 목록(250건/80KB)이나 배당 목록(44KB)을 통째로 받아 id 만 뽑고 버렸다. 이 집계 엔드포인트는 같은 집합을 2.8KB 로 준다.
 */
@HttpExchange(
    url = "/api/activityFilterIds",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface ActivityFilterIdsClient {

  @GetExchange
  ActivityFilterIdsResponse findFilterIds(
      @RequestParam UUID userId,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate);
}
