package net.luversof.web.gate.poe.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeUpgradeGuide;

/** bluesky-api-poe 업그레이드 가이드 잡 클라이언트 — PoB 코드를 기준선으로 "무엇을 바꾸면 얼마나 좋아지는가"를 측정한다. */
@HttpExchange(url = "/api/poe/guide", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeUpgradeGuideClient {

  /**
   * 분석 시작 — 이미 돌고 있거나 코드를 못 읽으면 false(사유는 status 의 error). PoB 코드는 폼 본문으로 보낸다 — URL 쿼리에 실으면 실제 캐릭터
   * 코드(13KB+)가 API 헤더 한도에 걸린다(PoeBuildClient 주석 참고).
   */
  @PostExchange(value = "/start", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  boolean start(@RequestParam String code, @RequestParam(required = false) String mercCode);

  @GetExchange("/status")
  PoeUpgradeGuide.Status status();

  /** 자동 다듬기 시작 — 가이드 교체안 중 가장 이득이 큰 것을 실제로 적용하고 다시 재기를 반복한다. 코드는 폼 본문으로. */
  @PostExchange(value = "/refine/start", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  boolean startRefine(@RequestParam String code);

  @GetExchange("/refine/status")
  PoeUpgradeGuide.RefineStatus refineStatus();
}
