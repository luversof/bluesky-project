package net.luversof.web.gate.poe.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.gate.poe.dto.PoeRegexPreset;
import net.luversof.web.gate.poe.httpexchange.PoeRegexClient;

/**
 * 지도 정규식 프리셋 JSON 프록시 — 저장/삭제는 로그인 게이팅(이력 삭제와 동일 관례). 세션 만료 시 401 을 명시적으로 내려 화면이 "저장되었습니다" 착시를 타지
 * 않게 한다(#261 교훈).
 */
@RestController
@RequestMapping(value = "/poe/api/regex/presets", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeRegexApiController {

  private final PoeRegexClient poeRegexClient;

  public PoeRegexApiController(PoeRegexClient poeRegexClient) {
    this.poeRegexClient = poeRegexClient;
  }

  @GetMapping
  public List<PoeRegexPreset.Entry> list() {
    return poeRegexClient.list();
  }

  @GetMapping("/{id}")
  public PoeRegexPreset get(@PathVariable long id) {
    return poeRegexClient.get(id);
  }

  @PostMapping
  public PoeRegexPreset save(
      @RequestBody PoeRegexPreset.SaveRequest request,
      Principal principal,
      HttpServletResponse response) {
    if (principal == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    return poeRegexClient.save(request);
  }

  @DeleteMapping("/{id}")
  public Boolean delete(@PathVariable long id, Principal principal, HttpServletResponse response) {
    if (principal == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    return poeRegexClient.delete(id);
  }
}
