package net.luversof.web.gate.poe.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeCraft;

/** PoE1 제작 에뮬레이터 API(10-15 C335) — 상태는 게이트가 들고 다닌다. */
@HttpExchange(url = "/api/poe/craft", accept = MediaType.APPLICATION_JSON_VALUE)
public interface PoeCraftClient {

  @PostExchange(value = "/apply", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.View apply(@RequestBody PoeCraft.ApplyRequest request);

  @PostExchange(value = "/view", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.View view(@RequestBody PoeCraft.Item item);

  @PostExchange(value = "/pool", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<PoeCraft.PoolLine> pool(@RequestBody PoeCraft.PoolRequest request);

  @PostExchange(value = "/calc", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.CalcResponse calc(@RequestBody PoeCraft.CalcRequest request);

  /** 최적 조합(C457) — 에센스 · 제작대 · 짐승 · 화폐를 섞은 최소 비용(시간 예산 안). */
  @PostExchange(value = "/optimize", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.OptimizeResult optimize(@RequestBody PoeCraft.OptimizeRequest request);

  /** 에뮬레이터 따라 하기(C464) — 끝난 작업 정책으로 다음 수단. */
  @PostExchange(value = "/optimize/{id}/next", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.OptimizeNext optimizeNext(
      @org.springframework.web.bind.annotation.PathVariable String id,
      @RequestBody PoeCraft.OptimizeNextRequest body);

  /** 최적 조합 백그라운드 시작(C457k) — {"id": …}. */
  @PostExchange(value = "/optimize/start", contentType = MediaType.APPLICATION_JSON_VALUE)
  java.util.Map<String, String> optimizeStart(@RequestBody PoeCraft.OptimizeRequest request);

  @org.springframework.web.service.annotation.GetExchange("/optimize/{id}")
  PoeCraft.OptimizeStatus optimizeStatus(
      @org.springframework.web.bind.annotation.PathVariable String id);

  @PostExchange(value = "/strategy", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.StrategyResult strategy(@RequestBody PoeCraft.StrategyRequest request);

  /** 쓴 화폐 · 총비용(C365). */
  @PostExchange(value = "/spent", contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeCraft.SpentResult spent(@RequestBody PoeCraft.SpentRequest request);

  /** 상태마다 옵션 줄(C455 — 단계 기록의 붙은 · 지운 옵션). */
  @PostExchange(value = "/lines", contentType = MediaType.APPLICATION_JSON_VALUE)
  List<List<PoeCraft.Line>> lines(@RequestBody List<PoeCraft.Item> items);

  @GetExchange("/options")
  PoeCraft.Options options(@RequestParam String itemClass, @RequestParam String variant);

  /** 이 종류의 장막 해제 옵션(C421). */
  @GetExchange("/veiled")
  List<PoeCraft.VeiledLine> veiled(@RequestParam String itemClass, @RequestParam String variant);

  @GetExchange("/essences")
  List<PoeCraft.Essence> essences(@RequestParam String itemClass);
}
