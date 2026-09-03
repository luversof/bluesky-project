package net.luversof.api.stock.web.controller;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.YearlyCostService;
import net.luversof.api.stock.web.dto.response.YearlyCostSummary;

/** 연도별 세금·비용 요약. 원장에는 다 있는데 합계를 내는 화면이 없던 값들이다. */
@RestController
@RequestMapping("/api/yearlyCost")
public class YearlyCostController {

  @Autowired private YearlyCostService yearlyCostService;

  public void setYearlyCostService(YearlyCostService yearlyCostService) {
    this.yearlyCostService = yearlyCostService;
  }

  @GetMapping
  public List<YearlyCostSummary> findYearlyCost(
      @RequestParam UUID userId,
      @RequestParam(required = false) Instant startDate,
      @RequestParam(required = false) Instant endDate,
      @RequestParam(required = false) String timeZone) {
    ZoneId zoneId;
    try {
      zoneId = timeZone == null || timeZone.isBlank() ? null : ZoneId.of(timeZone);
    } catch (Exception ex) {
      // 잘못된 존이면 서버 기본으로 떨어뜨리지 않고 한국 기준으로 둔다 - 세금은 그 나라 기준이다.
      zoneId = null;
    }
    return yearlyCostService.findYearlyCost(userId, startDate, endDate, zoneId);
  }
}
