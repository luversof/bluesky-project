package net.luversof.api.poe.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.poe.service.PoeCraftDataService;
import net.luversof.api.poe.service.PoeCraftEngine;
import net.luversof.api.poe.service.PoeCraftService;

/** PoE1 제작 에뮬레이터 API(10-15 C335) — 아이템 상태를 그대로 주고받는다(서버는 상태를 들고 있지 않는다). */
@RestController
@RequestMapping(value = "/api/poe/craft", produces = MediaType.APPLICATION_JSON_VALUE)
public class PoeCraftController {

  /** 화폐 적용 요청 — essence = 에센스 슬러그(ESSENCE 일 때), seed = 고정 난수(시험용, 없으면 무작위). */
  public record ApplyRequest(
      PoeCraftEngine.Item item, PoeCraftEngine.Action action, String essence, Long seed) {}

  /** 옵션 풀 요청 — rarity = 그 희귀도로 볼 자리(없으면 일반 → 레어, 그 밖엔 지금 희귀도). */
  public record PoolRequest(PoeCraftEngine.Item item, PoeCraftEngine.Rarity rarity) {}

  private final PoeCraftService craft;
  private final net.luversof.api.poe.service.PoeCraftCalcService calc;

  public PoeCraftController(
      PoeCraftService craft, net.luversof.api.poe.service.PoeCraftCalcService calc) {
    this.craft = craft;
    this.calc = calc;
  }

  /** 계산기(10-15 C336) — 목표 옵션 묶음 → 방법별 성공 확률 · 기대 시도 · 기대 비용(카오스, 비용 순). */
  @PostMapping(value = "/calc", consumes = MediaType.APPLICATION_JSON_VALUE)
  public net.luversof.api.poe.service.PoeCraftCalcService.Response calc(
      @RequestBody net.luversof.api.poe.service.PoeCraftCalcService.Request req) {
    check(req.item());
    return calc.calc(req);
  }

  @PostMapping(value = "/apply", consumes = MediaType.APPLICATION_JSON_VALUE)
  public PoeCraftService.View apply(@RequestBody ApplyRequest req) {
    check(req.item());
    if (req.action() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "action 이 없습니다");
    }
    return craft.apply(req.item(), req.action(), req.essence(), req.seed());
  }

  @PostMapping(value = "/view", consumes = MediaType.APPLICATION_JSON_VALUE)
  public PoeCraftService.View view(@RequestBody PoeCraftEngine.Item item) {
    check(item);
    return craft.view(item);
  }

  @PostMapping(value = "/pool", consumes = MediaType.APPLICATION_JSON_VALUE)
  public List<PoeCraftService.PoolLine> pool(@RequestBody PoolRequest req) {
    check(req.item());
    return craft.pool(req.item(), req.rarity());
  }

  @GetMapping("/essences")
  public List<PoeCraftDataService.Essence> essences(@RequestParam String itemClass) {
    return craft.essences(itemClass);
  }

  private void check(PoeCraftEngine.Item item) {
    if (item == null || item.itemClass() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "item 이 없습니다");
    }
    if (!craft.knowsPool(item)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "모르는 풀: " + item.poolKey());
    }
  }
}
