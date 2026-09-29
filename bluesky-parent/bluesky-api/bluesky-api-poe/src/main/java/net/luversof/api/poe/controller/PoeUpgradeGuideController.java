package net.luversof.api.poe.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.poe.service.PoeUpgradeGuideService;

/**
 * 업그레이드 가이드 API — PoB 코드를 받아 "무엇을 바꾸면 얼마나 좋아지는가"를 측정한다. 평가가 수십 번이라 잡으로 돌리고 상태를 폴링한다. 로그인 게이팅은 게이트가
 * 담당(여긴 잡만 구동).
 */
@RestController
@RequestMapping(value = "/api/poe/guide", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeUpgradeGuideController {

  private final PoeUpgradeGuideService poeUpgradeGuideService;

  public PoeUpgradeGuideController(PoeUpgradeGuideService poeUpgradeGuideService) {
    this.poeUpgradeGuideService = poeUpgradeGuideService;
  }

  /**
   * 분석 시작 — 이미 돌고 있거나 코드를 못 읽으면 false(사유는 status 의 error). mercCode = 루미너리 용병 빌드 PoB 코드(선택) — 있으면 모든 측정에
   * 용병 오라·저주가 들어간다.
   */
  @PostMapping("/start")
  public boolean start(@RequestParam String code, @RequestParam(required = false) String mercCode) {
    return poeUpgradeGuideService.start(code, mercCode);
  }

  @GetMapping("/status")
  public PoeUpgradeGuideService.GuideStatus status() {
    return poeUpgradeGuideService.status();
  }
}
