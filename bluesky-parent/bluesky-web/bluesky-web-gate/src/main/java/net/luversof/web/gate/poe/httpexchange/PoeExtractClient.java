package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeJobStatus;

/** bluesky-api-poe 게임 데이터 추출 파이프라인 잡 클라이언트. */
@HttpExchange(url = "/api/poe/extract", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeExtractClient {

  @PostExchange("/start")
  boolean start();

  @GetExchange("/status")
  PoeJobStatus.Extract status();
}
