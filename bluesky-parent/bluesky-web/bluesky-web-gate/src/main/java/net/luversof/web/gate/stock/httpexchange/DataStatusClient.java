package net.luversof.web.gate.stock.httpexchange;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import net.luversof.web.gate.stock.dto.response.DataStatusResponse;

/** 데이터 최신 시점(집계) 조회. 관리 화면 표시용. */
@HttpExchange(
    url = "/api/dataStatus",
    contentType = MediaType.APPLICATION_JSON_VALUE,
    accept = MediaType.APPLICATION_JSON_VALUE)
public interface DataStatusClient {

  @GetExchange
  DataStatusResponse findDataStatus(@RequestParam UUID userId);
}
