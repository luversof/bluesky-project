package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeJobStatus;

/** bluesky-api-poe 젬 DPS 랭킹 배치 잡 클라이언트. */
@HttpExchange(url = "/api/poe/sim", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeSimClient {

  @PostExchange("/start")
  void start();

  @GetExchange("/status")
  PoeJobStatus.Sim status();

  @GetExchange("/ranking")
  PoeJobStatus.SimRanking ranking();
}
