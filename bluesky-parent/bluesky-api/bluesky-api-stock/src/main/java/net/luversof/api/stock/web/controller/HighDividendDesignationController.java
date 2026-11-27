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

import net.luversof.api.stock.service.HighDividendDesignationService;
import net.luversof.api.stock.web.dto.request.HighDividendDesignationUpsertRequest;
import net.luversof.api.stock.web.dto.response.HighDividendDesignationResponse;

/**
 * 고배당기업 지정
 *
 * <p>고배당기업 배당소득 분리과세 대상 종목과 지급일 기간. 사용자 구분 없는 공용 값이다.
 */
@RestController
@RequestMapping("/api/highDividendDesignation")
public class HighDividendDesignationController {

  @Autowired private HighDividendDesignationService highDividendDesignationService;

  /**
   * 고배당기업 지정 목록 조회
   *
   * @return 사업연도 최신순 목록
   */
  @GetMapping
  public List<HighDividendDesignationResponse> findAll() {
    return highDividendDesignationService.findAll();
  }

  /**
   * 고배당기업 지정 저장
   *
   * <p>id 가 있으면 고치고, 없으면 새로 만든다.
   *
   * @param request 종목 심볼 · 사업연도 · 지급일 기간 · 메모
   * @return 저장한 지정
   */
  @PostMapping
  public HighDividendDesignationResponse upsert(
      @RequestBody HighDividendDesignationUpsertRequest request) {
    return highDividendDesignationService.upsert(request);
  }

  /**
   * 고배당기업 지정 삭제
   *
   * @param id 지정 id
   */
  @DeleteMapping
  public void delete(@RequestParam UUID id) {
    highDividendDesignationService.delete(id);
  }
}
