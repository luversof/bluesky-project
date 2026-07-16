package net.luversof.api.poe.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.poe.service.PoeBuild;
import net.luversof.api.poe.service.PoePobEngineService;
import net.luversof.api.poe.service.PoePobImportService;

/** PoB 공유 코드 임포트 + 헤드리스 엔진 재계산 API. */
@RestController
@RequestMapping(value = "/api/poe/build", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeBuildController {

  private final PoePobImportService poePobImportService;
  private final PoePobEngineService poePobEngineService;

  public PoeBuildController(
      PoePobImportService poePobImportService, PoePobEngineService poePobEngineService) {
    this.poePobImportService = poePobImportService;
    this.poePobEngineService = poePobEngineService;
  }

  /** PoB 코드 → 빌드 요약. 형식 오류는 400. */
  @PostMapping("/import")
  public PoeBuild importBuild(@RequestParam String code) {
    try {
      return poePobImportService.importCode(code);
    } catch (IllegalArgumentException e) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /** 헤드리스 엔진(LuaJIT+PoB) 가용 여부 — 게이트 admin/빌드 화면 표시용. */
  @GetMapping("/available")
  public boolean available() {
    return poePobEngineService.isAvailable();
  }

  /** PoB 코드 → 헤드리스 엔진 실계산 스탯. */
  @PostMapping("/recalculate")
  public PoePobEngineService.EngineResult recalculate(@RequestParam String code) {
    try {
      String buildXml = poePobImportService.decodeToXml(code);
      return poePobEngineService.recalculate(buildXml);
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
