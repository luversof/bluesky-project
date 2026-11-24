package net.luversof.api.stock.web.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.MonthlyDividendSnapshotService;
import net.luversof.api.stock.web.dto.request.MonthlyDividendSnapshotRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendSnapshotUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendSnapshotResponse;

@RestController
@RequestMapping("/api/monthlyDividendSnapshot")
public class MonthlyDividendSnapshotController {

  @Autowired private MonthlyDividendSnapshotService monthlyDividendSnapshotService;

  /**
   * 사용자의 월배당 스냅샷 목록. 종목별로 기준일의 보유 수량 · 평단과 월배당 지표(최근 · 1년 평균)를 저장해 둔 것이다.
   *
   * @param request userId
   * @return 스냅샷 목록
   */
  @GetMapping
  public List<MonthlyDividendSnapshotResponse> findSnapshots(
      MonthlyDividendSnapshotRequest request) {
    return monthlyDividendSnapshotService.findByUserId(request.getUserId());
  }

  /**
   * 스냅샷을 넣는다. 같은 사용자 · 종목이 있으면 고친다.
   *
   * @param request 사용자 · 종목 심볼 · 저장할 값
   * @return 저장한 스냅샷
   */
  @PostMapping
  public MonthlyDividendSnapshotResponse upsertSnapshot(
      @RequestBody MonthlyDividendSnapshotUpsertRequest request) {
    return monthlyDividendSnapshotService.upsert(request);
  }

  /**
   * 스냅샷을 지운다.
   *
   * @param userId 사용자 id
   * @param symbol 종목 심볼
   */
  @DeleteMapping
  public void deleteSnapshot(@RequestParam UUID userId, @RequestParam String symbol) {
    monthlyDividendSnapshotService.deleteByUserIdAndSymbol(userId, symbol);
  }
}
