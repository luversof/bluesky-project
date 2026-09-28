package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileUpsertRequest;

/**
 * 월배당 프로필의 총보수(연, %) · 상장일(2026-09-28, 사용자 승인 DDL). 열은 {@code NUMERIC(6,4)} · {@code DATE}.
 *
 * <p>저장은 다른 필드처럼 전체 덮어쓰기다 &mdash; 게이트가 기존 값을 돌려보낸다({@code PartialUpdateDataLossGuardTest} 참고).
 */
@ExtendWith(MockitoExtension.class)
class MonthlyDividendProfileExpenseListingTest {

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private StockItemRepository stockItemRepository;

  @InjectMocks private MonthlyDividendProfileService monthlyDividendProfileService;

  @Test
  void 총보수와_상장일을_저장하고_돌려준다() {
    StockItem stockItem = stockItem("476800");
    when(stockItemRepository.findBySymbol("476800")).thenReturn(stockItem);
    when(monthlyDividendProfileRepository.findByStockItemId(stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendProfileRepository.save(any(MonthlyDividendProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var request = request("476800");
    request.setTotalExpenseRatioPct(new BigDecimal("0.09"));
    request.setListingDate(LocalDate.of(2024, 3, 5));

    var response = monthlyDividendProfileService.upsert(request);

    // 열 자릿수(소수 넷째 자리)에 맞춰 저장한다.
    assertThat(response.totalExpenseRatioPct()).isEqualByComparingTo("0.09");
    assertThat(response.totalExpenseRatioPct().scale()).isEqualTo(4);
    assertThat(response.listingDate()).isEqualTo(LocalDate.of(2024, 3, 5));
  }

  @Test
  void 모르면_null_로_둔다() {
    StockItem stockItem = stockItem("466940");
    when(stockItemRepository.findBySymbol("466940")).thenReturn(stockItem);
    when(monthlyDividendProfileRepository.findByStockItemId(stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendProfileRepository.save(any(MonthlyDividendProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var response = monthlyDividendProfileService.upsert(request("466940"));

    assertThat(response.totalExpenseRatioPct()).isNull();
    assertThat(response.listingDate()).isNull();
  }

  /** 음수 보수는 없고 100% 이상은 단위 실수(0.09% 를 9 로)다 - DB 오류(500)가 나기 전에 400 으로 막는다. 0 은 받는다(무보수 상품). */
  @Test
  void 총보수_범위_밖은_400_으로_막는다() {
    assertThat(MonthlyDividendProfileService.normalizeTotalExpenseRatioPct(BigDecimal.ZERO))
        .isEqualByComparingTo("0");
    assertThat(
            MonthlyDividendProfileService.normalizeTotalExpenseRatioPct(new BigDecimal("99.9999")))
        .isEqualByComparingTo("99.9999");
    assertThat(
            MonthlyDividendProfileService.normalizeTotalExpenseRatioPct(new BigDecimal("0.123456")))
        .isEqualByComparingTo("0.1235");

    assertThatThrownBy(
            () ->
                MonthlyDividendProfileService.normalizeTotalExpenseRatioPct(
                    new BigDecimal("-0.01")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400");
    assertThatThrownBy(
            () ->
                MonthlyDividendProfileService.normalizeTotalExpenseRatioPct(new BigDecimal("100")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("400");
  }

  private static MonthlyDividendProfileUpsertRequest request(String symbol) {
    var request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol(symbol);
    request.setPayoutWindow("MID_MONTH");
    request.setDisplayOrder(1);
    request.setActive(true);
    return request;
  }

  private static StockItem stockItem(String symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setId(UUID.randomUUID());
    stockItem.setSymbol(symbol);
    stockItem.setName(symbol);
    stockItem.setMarket("KRX");
    return stockItem;
  }
}
