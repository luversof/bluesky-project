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

  @GetMapping
  public List<MonthlyDividendSnapshotResponse> findSnapshots(
      MonthlyDividendSnapshotRequest request) {
    return monthlyDividendSnapshotService.findByUserId(request.getUserId());
  }

  @PostMapping
  public MonthlyDividendSnapshotResponse upsertSnapshot(
      @RequestBody MonthlyDividendSnapshotUpsertRequest request) {
    return monthlyDividendSnapshotService.upsert(request);
  }

  @DeleteMapping
  public void deleteSnapshot(@RequestParam UUID userId, @RequestParam String symbol) {
    monthlyDividendSnapshotService.deleteByUserIdAndSymbol(userId, symbol);
  }
}
