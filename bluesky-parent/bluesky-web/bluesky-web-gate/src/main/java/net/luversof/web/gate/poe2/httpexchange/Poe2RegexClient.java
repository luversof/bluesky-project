package net.luversof.web.gate.poe2.httpexchange;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import net.luversof.web.gate.poe.dto.PoeRegexPreset;

/** bluesky-api-poe 경로석 정규식 프리셋 클라이언트 — 모양은 PoE1 지도 정규식 프리셋과 같다(저장 폴더만 다름). */
@HttpExchange(url = "/api/poe2/regex/presets", accept = MediaType.APPLICATION_JSON_VALUE)
public interface Poe2RegexClient {

  @GetExchange
  List<PoeRegexPreset.Entry> list();

  @GetExchange("/{id}")
  PoeRegexPreset get(@PathVariable long id);

  @PostExchange(contentType = MediaType.APPLICATION_JSON_VALUE)
  PoeRegexPreset save(@RequestBody PoeRegexPreset.SaveRequest request);

  @DeleteExchange("/{id}")
  boolean delete(@PathVariable long id);
}
