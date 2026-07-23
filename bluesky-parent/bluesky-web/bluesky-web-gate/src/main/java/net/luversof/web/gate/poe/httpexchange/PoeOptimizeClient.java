package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeJobStatus;

/** bluesky-api-poe 최적 조합 탐색 잡 클라이언트. */
@HttpExchange(url = "/api/poe/optimize", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeOptimizeClient {

  @PostExchange("/start")
  boolean start(
      @RequestParam String slug,
      @RequestParam(required = false) String objective,
      @RequestParam(required = false) String scenario,
      @RequestParam(required = false) Boolean buffs,
      @RequestParam(required = false) String className,
      @RequestParam(required = false) String ascendancy,
      @RequestParam(required = false) String uniques,
      @RequestParam(required = false) String skills,
      @RequestParam(required = false) String treeNodes,
      @RequestParam(required = false) String masteries,
      @RequestParam(required = false) String jewels,
      @RequestParam(required = false) String clusters,
      @RequestParam(required = false) String tattoos,
      @RequestParam(required = false) String anoint);

  @GetExchange("/status")
  PoeJobStatus.Optimize status();
}
