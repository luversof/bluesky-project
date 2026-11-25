package net.luversof.api.poe.poe2;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.controller.PoeRegexPresetController;
import net.luversof.api.poe.service.PoeRegexPresetService;

/**
 * PoE2 경로석 정규식 프리셋
 *
 * <p>PoE2 경로석 정규식 프리셋 CRUD — PoE1 지도 정규식 프리셋과 같은 저장 방식(파일 한 건 = 프리셋 하나)을 PoE2 데이터 폴더
 * (poe2.data-dir/regex-presets)에 따로 둔다. 선택 키가 게임마다 달라 한 목록에 섞이면 불러올 때 엉뚱한 옵션이 켜진다.
 */
@RestController
@RequestMapping(value = "/api/poe2/regex/presets", produces = MediaType.APPLICATION_JSON_VALUE)
public class Poe2RegexPresetController {

  private final PoeRegexPresetService presetService;

  public Poe2RegexPresetController(
      @Value("${poe2.data-dir:${user.home}/.poe-gamedata/poe2}") String dataDir) {
    this.presetService = new PoeRegexPresetService(dataDir);
  }

  /**
   * 경로석 정규식 프리셋 목록 조회
   *
   * @return 프리셋 요약 목록
   */
  @GetMapping
  public List<PoeRegexPresetService.RegexPresetEntry> list() {
    return presetService.list();
  }

  /**
   * 경로석 정규식 프리셋 조회
   *
   * @param id 프리셋 id
   * @return 프리셋
   */
  @GetMapping("/{id}")
  public PoeRegexPresetService.RegexPreset get(@PathVariable long id) {
    return presetService.get(id);
  }

  /**
   * 경로석 정규식 프리셋 저장
   *
   * <p>id 가 있으면 고친다.
   *
   * @param request id(선택) · 이름 · 정규식 · 화면 데이터
   * @return 저장한 프리셋
   */
  @PostMapping
  public PoeRegexPresetService.RegexPreset save(
      @RequestBody PoeRegexPresetController.SaveRequest request) {
    return presetService.save(request.id(), request.name(), request.regex(), request.data());
  }

  /**
   * 경로석 정규식 프리셋 삭제
   *
   * @param id 프리셋 id
   * @return 지웠으면 true
   */
  @DeleteMapping("/{id}")
  public boolean delete(@PathVariable long id) {
    return presetService.delete(id);
  }
}
