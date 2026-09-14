package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendProfileResponse;

/**
 * 월배당 프로필 조회. 관리 화면의 프로필 목록과 시뮬레이터의 종목 순서가 이 결과를 쓴다.
 *
 * <p>테스트가 없었다(실측 2026-09-12: api-stock 에서 테스트가 한 번도 부르지 않는 public 메서드 14 개 중 하나). 고정하는 것은 넷이다.
 *
 * <ol>
 *   <li>종목을 지정하면 그 종목 것만 준다 - 모르는 종목코드는 <b>400 으로 끊는다</b>. 조용히 널로 떨어지면 "그 종목만" 을 물었는데 전체 목록이 오게 된다.
 *   <li>활성만 보기는 다른 질의를 쓴다.
 *   <li>종목 정보는 <b>한 번에</b> 받아 붙인다(행마다 조회하면 행 수만큼 SELECT 가 나간다).
 *   <li>활성 여부가 비어 있으면 꺼진 것으로 읽는다.
 * </ol>
 *
 * <p>실측 2026-09-12(운영): 전체 8행, `symbol=476800` 1행, 앞뒤 공백을 넣어도 1행, 없는 코드는 400.
 */
@ExtendWith(MockitoExtension.class)
class MonthlyDividendProfileFindTest {

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private StockItemRepository stockItemRepository;

  @InjectMocks private MonthlyDividendProfileService monthlyDividendProfileService;

  private StockItem stockItem(UUID id, String symbol) {
    StockItem item = new StockItem();
    item.setId(id);
    item.setSymbol(symbol);
    item.setName(symbol + " 이름");
    return item;
  }

  private MonthlyDividendProfile profile(UUID stockItemId, Integer displayOrder, Boolean active) {
    MonthlyDividendProfile profile = new MonthlyDividendProfile();
    profile.setId(UUID.randomUUID());
    profile.setStockItemId(stockItemId);
    profile.setSourceUrl("https://example.test");
    profile.setPayoutWindow("MID_MONTH");
    profile.setDisplayOrder(displayOrder);
    profile.setActive(active);
    profile.setNote("");
    profile.setLastVerifiedDate(LocalDate.of(2026, 9, 1));
    profile.setUpdatedDate(Instant.parse("2026-09-02T00:00:00Z"));
    return profile;
  }

  private MonthlyDividendProfileRequest request(String symbol, Boolean activeOnly) {
    MonthlyDividendProfileRequest request = new MonthlyDividendProfileRequest();
    request.setSymbol(symbol);
    request.setActiveOnly(activeOnly);
    return request;
  }

  @Test
  void 종목을_지정하면_그_종목_것만_준다() {
    UUID itemId = UUID.randomUUID();
    when(stockItemRepository.findBySymbol("476800")).thenReturn(stockItem(itemId, "476800"));
    when(monthlyDividendProfileRepository.findByStockItemId(itemId))
        .thenReturn(java.util.Optional.of(profile(itemId, 1, true)));
    when(stockItemRepository.findById(itemId))
        .thenReturn(java.util.Optional.of(stockItem(itemId, "476800")));

    List<MonthlyDividendProfileResponse> found =
        monthlyDividendProfileService.findProfiles(request("  476800  ", null));

    assertThat(found).hasSize(1);
    assertThat(found.get(0).stockItemSymbol()).as("종목 정보를 채워 준다").isEqualTo("476800");
    assertThat(found.get(0).stockItemName()).isEqualTo("476800 이름");
    verify(monthlyDividendProfileRepository, never())
        .findAllByOrderByDisplayOrderAscUpdatedDateDesc();
  }

  /** 모르는 종목코드에 전체 목록으로 답하면 "그 종목만" 을 물은 화면이 남의 줄을 그린다. */
  @Test
  void 모르는_종목코드는_끊는다() {
    when(stockItemRepository.findBySymbol("NOSUCH")).thenReturn(null);

    assertThatThrownBy(() -> monthlyDividendProfileService.findProfiles(request("NOSUCH", null)))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Unknown stock symbol");
    verify(monthlyDividendProfileRepository, never())
        .findAllByOrderByDisplayOrderAscUpdatedDateDesc();
    verify(monthlyDividendProfileRepository, never()).findByStockItemId(any());
  }

  @Test
  void 활성만_보기는_다른_질의를_쓴다() {
    UUID itemId = UUID.randomUUID();
    when(monthlyDividendProfileRepository.findByActiveOrderByDisplayOrderAscUpdatedDateDesc(true))
        .thenReturn(List.of(profile(itemId, 1, true)));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(stockItem(itemId, "476800")));

    assertThat(monthlyDividendProfileService.findProfiles(request(null, true))).hasSize(1);
    verify(monthlyDividendProfileRepository, never())
        .findAllByOrderByDisplayOrderAscUpdatedDateDesc();
  }

  /** 행마다 조회하면 행 수만큼 SELECT 가 나간다 - 한 번에 받아 붙인다. */
  @Test
  void 종목_정보는_한_번에_받아_붙인다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(first, 1, true), profile(second, 2, true)));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(List.of(stockItem(first, "476800"), stockItem(second, "329200")));

    List<MonthlyDividendProfileResponse> found =
        monthlyDividendProfileService.findProfiles(request(null, null));

    assertThat(found)
        .extracting(MonthlyDividendProfileResponse::stockItemSymbol)
        .containsExactly("476800", "329200");
    verify(stockItemRepository, times(1)).findAllById(any());
    verify(stockItemRepository, never()).findById(any());
  }

  @Test
  void 활성_여부가_비면_꺼진_것으로_읽는다() {
    UUID itemId = UUID.randomUUID();
    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(profile(itemId, 1, null)));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(stockItem(itemId, "476800")));

    assertThat(monthlyDividendProfileService.findProfiles(request(null, null)).get(0).active())
        .isFalse();
  }
}
