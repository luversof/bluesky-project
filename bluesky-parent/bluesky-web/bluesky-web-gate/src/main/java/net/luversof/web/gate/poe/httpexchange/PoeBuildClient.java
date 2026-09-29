package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.EngineResult;
import net.luversof.web.gate.poe.dto.PoeBuild;

/**
 * bluesky-api-poe 빌드 임포트/재계산 클라이언트.
 *
 * <p>⚠ PoB 코드는 <b>폼 본문</b>으로 보낸다(contentType = form-urlencoded). 지정이 없으면 {@code @RequestParam} 이
 * URL 쿼리로 붙는데, 실제 캐릭터 코드는 13KB 를 넘어 API 톰캣의 헤더 한도(8KB)에 걸린다 — API 가 "Request header is too large"
 * HTML 을 돌려주고 게이트는 그걸 JSON 으로 읽다 {@code Unexpected character ('<')} 로 터졌다(2026-09-29 사용자 제보,
 * poe.ninja 족장 코드 13,440자).
 */
@HttpExchange(url = "/api/poe/build", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeBuildClient {

  @PostExchange(value = "/import", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  PoeBuild importBuild(@RequestParam String code);

  @GetExchange("/available")
  boolean available();

  @PostExchange(value = "/recalculate", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  EngineResult recalculate(
      @RequestParam String code, @RequestParam(required = false) String mercCode);

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
