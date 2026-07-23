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

  /** 트리 에디터에서 찍은 노드 그대로 실계산(장비/보조젬 없음). */
  @PostExchange("/tree-stats")
  net.luversof.web.gate.poe.dto.PoeTreeEvaluation treeStats(
      @RequestParam int classId,
      @RequestParam(required = false) String ascendancy,
      @RequestParam String nodes,
      @RequestParam(required = false) String gem,
      @RequestParam(required = false) String masteries,
      @RequestParam(required = false) String jewels,
      @RequestParam(required = false) String clusters,
      @RequestParam(required = false) String tattoos,
      @RequestParam(required = false) Integer anoint);
}
