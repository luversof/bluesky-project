package net.luversof.api.stock.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.service.MonthlyDividendProfileService;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileReorderRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendProfileResponse;

@RestController
@RequestMapping("/api/monthlyDividendProfile")
public class MonthlyDividendProfileController {

  @Autowired private MonthlyDividendProfileService monthlyDividendProfileService;

  @GetMapping
  public List<MonthlyDividendProfileResponse> findProfiles(MonthlyDividendProfileRequest request) {
    return monthlyDividendProfileService.findProfiles(request);
  }

  @PostMapping
  public MonthlyDividendProfileResponse upsertProfile(
      @RequestBody MonthlyDividendProfileUpsertRequest request) {
    return monthlyDividendProfileService.upsert(request);
  }

  @PutMapping("/order")
  public void reorderProfiles(@RequestBody MonthlyDividendProfileReorderRequest request) {
    monthlyDividendProfileService.reorder(request);
  }

  @DeleteMapping
  public void deleteProfile(@RequestParam String symbol) {
    monthlyDividendProfileService.deleteBySymbol(symbol);
  }
}
