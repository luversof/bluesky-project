package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.EngineResult;
import net.luversof.web.gate.poe.dto.PoeBuild;

/** bluesky-api-poe 빌드 임포트/재계산 클라이언트. */
@HttpExchange(url = "/api/poe/build", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeBuildClient {

  @PostExchange("/import")
  PoeBuild importBuild(@RequestParam String code);

  @GetExchange("/available")
  boolean available();

  @PostExchange("/recalculate")
  EngineResult recalculate(@RequestParam String code);
}
