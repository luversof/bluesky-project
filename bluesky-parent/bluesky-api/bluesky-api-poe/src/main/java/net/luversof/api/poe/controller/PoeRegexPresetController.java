package net.luversof.api.poe.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.PoeRegexPresetService;

/** 지도 정규식 프리셋 CRUD — 로그인 게이팅은 게이트가 담당(이력 삭제와 동일 관례). */
@RestController
@RequestMapping(value = "/api/poe/regex/presets", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeRegexPresetController {

  private final PoeRegexPresetService poeRegexPresetService;

  public PoeRegexPresetController(PoeRegexPresetService poeRegexPresetService) {
    this.poeRegexPresetService = poeRegexPresetService;
  }

  public record SaveRequest(Long id, String name, String regex, Map<String, Object> data) {}

  @GetMapping
  public List<PoeRegexPresetService.RegexPresetEntry> list() {
    return poeRegexPresetService.list();
  }

  /** 한 건 전체 조회 — 없거나 깨졌으면 null(빈 응답). */
  @GetMapping("/{id}")
  public PoeRegexPresetService.RegexPreset get(@PathVariable long id) {
    return poeRegexPresetService.get(id);
  }

  /** 저장 — id 있으면 편집(같은 id 유지), 없으면 신규. 실패 시 null. */
  @PostMapping
  public PoeRegexPresetService.RegexPreset save(@RequestBody SaveRequest request) {
    return poeRegexPresetService.save(
        request.id(), request.name(), request.regex(), request.data());
  }

  @DeleteMapping("/{id}")
  public boolean delete(@PathVariable long id) {
    return poeRegexPresetService.delete(id);
  }
}
