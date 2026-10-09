package net.luversof.web.gate.poe2.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe2.dto.Poe2;

/**
 * PoB-PoE2 엔진을 도는 호출만 따로 — 레벨 100 실빌드에서 가이드가 12~24초라 게이트 공통 읽기 제한(10초)에 걸려 늘 실패했다(09-30 poe.ninja
 * 실빌드 6개로 발견, 합성 표본은 7초라 탐침이 통과). 제한을 넓힌 전용 RestClient 로 만든다(GatePoe2Config).
 */
@HttpExchange("/api/poe2")
public interface Poe2EngineClient {

  /** 엔진 재계산(약 2초) — 코드는 form 본문으로. */
  @PostExchange(url = "/build/recalc", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  Poe2.BuildRecalc recalcBuild(@RequestParam String code);

  /** 업그레이드 가이드(실빌드 5~12초 — API 가 4조각 병렬, 한 프로세스면 22~24초). */
  @PostExchange(url = "/build/guide", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  Poe2.BuildGuide guideBuild(@RequestParam String code);

  /** 패시브 트리 평가(약 1.5초) — 찍은 노드로 엔진 계산(10-01). */
  @PostExchange(url = "/tree/eval", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  Poe2.TreeEval treeEval(
      @RequestParam String className,
      @RequestParam(required = false) String ascendancy,
      @RequestParam(required = false) String nodes,
      @RequestParam(required = false) String skill,
      @RequestParam(required = false) String attrs,
      @RequestParam(required = false) String sets,
      // 꽂은 고유 주얼 "칸:slug,…"(10-03 C73)
      @RequestParam(required = false) String jewels);

  /** 가이드 레어 목표만(10-02) — 가이드 뒤에 따로. */
  @PostExchange(
      url = "/build/guide/rares",
      contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  Poe2.GuideRares guideRares(
      @RequestParam String code, @RequestParam(required = false) Integer set);

  /** 무기 세트를 지정한 가이드(세트를 나눠 쓰는 빌드에서 다른 세트 보기) — 10-01. */
  @PostExchange(url = "/build/guide", contentType = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
  Poe2.BuildGuide guideBuild(@RequestParam String code, @RequestParam Integer set);
}
