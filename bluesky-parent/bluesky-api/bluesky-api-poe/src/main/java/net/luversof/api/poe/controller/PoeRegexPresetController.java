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

/**
 * PoE1 지도 정규식 프리셋
 *
 * <p>로그인 확인은 게이트가 한다(이력 삭제와 같은 관례).
 */
@RestController
@RequestMapping(value = "/api/poe/regex/presets", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeRegexPresetController {

  private final PoeRegexPresetService poeRegexPresetService;

  public PoeRegexPresetController(PoeRegexPresetService poeRegexPresetService) {
    this.poeRegexPresetService = poeRegexPresetService;
  }

  public record SaveRequest(Long id, String name, String regex, Map<String, Object> data) {}

  /**
   * 지도 정규식 프리셋 목록 조회
   *
   * @return 프리셋 요약 목록
   */
  @GetMapping
  public List<PoeRegexPresetService.RegexPresetEntry> list() {
    return poeRegexPresetService.list();
  }

  /**
   * 지도 정규식 프리셋 조회
   *
   * <p>없거나 깨졌으면 null(빈 응답).
   */
  @GetMapping("/{id}")
  public PoeRegexPresetService.RegexPreset get(@PathVariable long id) {
    return poeRegexPresetService.get(id);
  }

  /**
   * 지도 정규식 프리셋 저장
   *
   * <p>id 가 있으면 편집(같은 id 유지), 없으면 새로 만든다. 실패하면 null.
   */
  @PostMapping
  public PoeRegexPresetService.RegexPreset save(@RequestBody SaveRequest request) {
    return poeRegexPresetService.save(
        request.id(), request.name(), request.regex(), request.data());
  }

  /**
   * 지도 정규식 프리셋 삭제
   *
   * @param id 프리셋 id
   * @return 지웠으면 true
   */
  @DeleteMapping("/{id}")
  public boolean delete(@PathVariable long id) {
    return poeRegexPresetService.delete(id);
  }
}
