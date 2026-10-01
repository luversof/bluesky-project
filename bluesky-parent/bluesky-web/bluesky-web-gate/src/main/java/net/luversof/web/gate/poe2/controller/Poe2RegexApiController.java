package net.luversof.web.gate.poe2.controller;

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
import net.luversof.web.gate.poe2.httpexchange.Poe2RegexClient;

/** 경로석 정규식 프리셋 JSON 프록시 — PoE1 PoeRegexApiController 와 같은 규칙(저장·삭제는 로그인, 세션 만료 시 401). */
@RestController
@RequestMapping(value = "/poe2/api/regex/presets", produces = MediaType.APPLICATION_JSON_VALUE)
public class Poe2RegexApiController {

  private final Poe2RegexClient poe2RegexClient;

  public Poe2RegexApiController(Poe2RegexClient poe2RegexClient) {
    this.poe2RegexClient = poe2RegexClient;
  }

  @GetMapping
  public List<PoeRegexPreset.Entry> list() {
    return poe2RegexClient.list();
  }

  @GetMapping("/{id}")
  public PoeRegexPreset get(@PathVariable long id) {
    return poe2RegexClient.get(id);
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
    return poe2RegexClient.save(request);
  }

  @DeleteMapping("/{id}")
  public Boolean delete(@PathVariable long id, Principal principal, HttpServletResponse response) {
    if (principal == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return null;
    }
    return poe2RegexClient.delete(id);
  }
}
