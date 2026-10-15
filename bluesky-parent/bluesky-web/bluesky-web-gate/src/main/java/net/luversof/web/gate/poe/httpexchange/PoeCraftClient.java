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

  @GetExchange("/essences")
  List<PoeCraft.Essence> essences(@RequestParam String itemClass);
}
