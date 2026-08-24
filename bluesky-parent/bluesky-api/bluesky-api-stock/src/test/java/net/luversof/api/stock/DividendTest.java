package net.luversof.api.stock;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralTest;
import net.luversof.api.stock.constant.TestConstant;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.service.DividendService;
import net.luversof.api.stock.service.StockAdminService;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;

class DividendTest implements GeneralTest {

  private static final Logger log = LoggerFactory.getLogger(DividendTest.class);

  @Autowired StockAdminService stockAdminService;

  @Autowired DividendRepository dividendRepository;

  @Autowired DividendService dividendService;

  UUID userId = TestConstant.USER_ID;

  /**
   * 실사용자 데이터를 실제로 바꾸는 개발용 도구다. 자동 실행에서 돌면 안 된다. 실측 사고(2026-08-22): 프로필을 주고 AccountTest 를 돌리자
   * deleteAllByUserId 가 계좌 7 -> 0, 거래 250 -> 0, 배당 193 -> 0 으로 지웠다. 원장은 시트 재가져오기로 되돌렸지만 계좌
   * 설정(manualPrincipalAmount)은 복구 경로가 없어 잃었다. 필요할 때 이 애노테이션을 손으로 떼고 쓸 것.
   */
  @Disabled("실사용자 데이터를 바꾼다 - 필요할 때만 손으로 실행")
  @Test
  void dividendBulkInsert() throws IOException {
    stockAdminService.dividendBulkInsert(TestConstant.USER_ID);

    // Ensure service.findDividends returns stockItemId populated
    DividendSearchRequest request = new DividendSearchRequest();
    request.setUserId(userId);
    List<Dividend> found = dividendService.findDividends(request);
    assertThat(found).isNotEmpty();
    found.forEach(d -> assertThat(d.getStockItemId()).isNotNull());
  }

  @Test
  void selectAllDividends() {
    var all = StreamSupport.stream(dividendRepository.findAll().spliterator(), false).toList();
    log.info("Total dividends in DB: {}", all.size());
    all.forEach(
        d ->
            log.info(
                "Dividend id={}, accountId={}, stockItemId={}, stockItemName={}",
                d.getId(),
                d.getAccountId(),
                d.getStockItemId(),
                d.getStockItemName()));
    assertThat(all).isNotNull();
  }
}
