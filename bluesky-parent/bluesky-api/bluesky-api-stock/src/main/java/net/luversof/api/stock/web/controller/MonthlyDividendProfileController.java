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

  /**
   * 월배당 프로필(관심 월배당 종목)을 찾는다.
   *
   * @param request 조건
   * @return 프로필 목록(표시 순서대로)
   */
  @GetMapping
  public List<MonthlyDividendProfileResponse> findProfiles(MonthlyDividendProfileRequest request) {
    return monthlyDividendProfileService.findProfiles(request);
  }

  /**
   * 월배당 프로필을 넣는다. 같은 종목이 있으면 고친다.
   *
   * @param request 종목 심볼과 프로필 값
   * @return 저장한 프로필
   */
  @PostMapping
  public MonthlyDividendProfileResponse upsertProfile(
      @RequestBody MonthlyDividendProfileUpsertRequest request) {
    return monthlyDividendProfileService.upsert(request);
  }

  /**
   * 프로필 표시 순서를 바꾼다.
   *
   * @param request 새 순서의 종목 목록
   */
  @PutMapping("/order")
  public void reorderProfiles(@RequestBody MonthlyDividendProfileReorderRequest request) {
    monthlyDividendProfileService.reorder(request);
  }

  /**
   * 월배당 프로필을 지운다.
   *
   * @param symbol 종목 심볼
   */
  @DeleteMapping
  public void deleteProfile(@RequestParam String symbol) {
    monthlyDividendProfileService.deleteBySymbol(symbol);
  }
}
