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
      @RequestParam(required = false) String timeZone,
      @RequestParam(required = false) List<UUID> accountIdList,
      @RequestParam(required = false) List<UUID> stockItemIdList) {
    // 값이 없으면 널을 넘겨 서비스가 한국 기준을 쓰게 둔다 - 세금은 그 나라 기준이다.
    // 값을 줬는데 모르는 존이면 조용히 한국 기준으로 바꾸지 않고 400 으로 끊는다(granularity 와 같은 규칙).
    ZoneId zoneId = net.luversof.api.stock.web.support.RequestZoneUtil.parse(timeZone, null);
    // 뒤집힌 구간은 어느 질의에도 안 걸려 빈 결과가 된다 - 그 빈 결과는 "자료 없음" 과 구분되지 않는다.
    Instant[] range =
        net.luversof.api.stock.web.support.RequestRangeUtil.ordered(startDate, endDate);
    // 계좌/종목을 좁히는 값은 형제 엔드포인트(배당·매매)와 같은 이름을 쓴다. 형태가 틀리면 조용히 무시하지 않고
    // 400 으로 끊는다 - 무시하면 전 계좌 합계가 "좁힌 결과" 인 척 나간다.
    return yearlyCostService.findYearlyCost(
        userId, range[0], range[1], zoneId, accountIdList, stockItemIdList);
  }
}
