package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.MonthlyDividendProfile;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.repository.MonthlyDividendProfileRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileReorderRequest;
import net.luversof.api.stock.web.dto.request.MonthlyDividendProfileUpsertRequest;
import net.luversof.api.stock.web.dto.response.MonthlyDividendProfileResponse;

@ExtendWith(MockitoExtension.class)
class MonthlyDividendProfileServiceTest {

  @Mock private MonthlyDividendProfileRepository monthlyDividendProfileRepository;

  @Mock private StockItemRepository stockItemRepository;

  @InjectMocks private MonthlyDividendProfileService monthlyDividendProfileService;

  @Test
  void upsertNewProfileDoesNotPreassignIdBeforeSave() {
    StockItem stockItem = createStockItem("498400");
    UUID generatedId = UUID.randomUUID();

    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol("498400");
    request.setSourceUrl("https://example.com/etf");
    request.setPayoutWindow("MONTH_END");
    request.setDisplayOrder(3);
    request.setActive(true);
    request.setNote("note");
    request.setLastVerifiedDate(LocalDate.of(2026, 5, 22));

    when(stockItemRepository.findBySymbol("498400")).thenReturn(stockItem);
    when(monthlyDividendProfileRepository.findByStockItemId(stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendProfileRepository.save(any(MonthlyDividendProfile.class)))
        .thenAnswer(
            invocation -> {
              MonthlyDividendProfile profile = invocation.getArgument(0);
              assertThat(profile.getId()).isNull();
              assertThat(profile.getCreatedDate()).isNotNull();
              profile.setId(generatedId);
              return profile;
            });

    MonthlyDividendProfileResponse response = monthlyDividendProfileService.upsert(request);

    assertThat(response.id()).isEqualTo(generatedId);
    assertThat(response.stockItemSymbol()).isEqualTo("498400");
    assertThat(response.sourceUrl()).isEqualTo("https://example.com/etf");
    assertThat(response.displayOrder()).isEqualTo(3);
  }

  @Test
  void upsertExistingProfileKeepsDisplayOrderWhenRequestOmitsIt() {
    StockItem stockItem = createStockItem("494300");
    MonthlyDividendProfile existingProfile = new MonthlyDividendProfile();
    existingProfile.setId(UUID.randomUUID());
    existingProfile.setStockItemId(stockItem.getId());
    existingProfile.setDisplayOrder(7);

    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol("494300");
    request.setPayoutWindow("MID_MONTH");
    request.setActive(true);

    when(stockItemRepository.findBySymbol("494300")).thenReturn(stockItem);
    when(monthlyDividendProfileRepository.findByStockItemId(stockItem.getId()))
        .thenReturn(Optional.of(existingProfile));
    when(monthlyDividendProfileRepository.save(any(MonthlyDividendProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MonthlyDividendProfileResponse response = monthlyDividendProfileService.upsert(request);

    assertThat(response.displayOrder()).isEqualTo(7);
  }

  @Test
  void upsertNewProfileAppendsToEndWhenRequestOmitsDisplayOrder() {
    StockItem stockItem = createStockItem("159800");
    MonthlyDividendProfile highestOrderProfile = new MonthlyDividendProfile();
    highestOrderProfile.setDisplayOrder(8);

    MonthlyDividendProfileUpsertRequest request = new MonthlyDividendProfileUpsertRequest();
    request.setSymbol("159800");
    request.setPayoutWindow("MID_MONTH");
    request.setActive(true);

    when(stockItemRepository.findBySymbol("159800")).thenReturn(stockItem);
    when(monthlyDividendProfileRepository.findByStockItemId(stockItem.getId()))
        .thenReturn(Optional.empty());
    when(monthlyDividendProfileRepository.findFirstByOrderByDisplayOrderDescUpdatedDateDesc())
        .thenReturn(Optional.of(highestOrderProfile));
    when(monthlyDividendProfileRepository.save(any(MonthlyDividendProfile.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MonthlyDividendProfileResponse response = monthlyDividendProfileService.upsert(request);

    assertThat(response.displayOrder()).isEqualTo(9);
  }

  @Test
  void reorderAssignsSequentialDisplayOrderFromRequestedSymbolOrder() {
    StockItem plus = createStockItem("0018C0");
    StockItem tiger = createStockItem("329200");
    StockItem rise = createStockItem("494300");

    MonthlyDividendProfile plusProfile = createProfile(plus, 1);
    MonthlyDividendProfile tigerProfile = createProfile(tiger, 2);
    MonthlyDividendProfile riseProfile = createProfile(rise, 3);

    MonthlyDividendProfileReorderRequest request = new MonthlyDividendProfileReorderRequest();
    request.setSymbols(List.of("329200", "0018C0", "494300"));

    when(monthlyDividendProfileRepository.findAllByOrderByDisplayOrderAscUpdatedDateDesc())
        .thenReturn(List.of(plusProfile, tigerProfile, riseProfile));
    when(stockItemRepository.findBySymbol("329200")).thenReturn(tiger);
    when(stockItemRepository.findBySymbol("0018C0")).thenReturn(plus);
    when(stockItemRepository.findBySymbol("494300")).thenReturn(rise);

    monthlyDividendProfileService.reorder(request);

    ArgumentCaptor<Iterable<MonthlyDividendProfile>> captor =
        ArgumentCaptor.forClass(Iterable.class);
    verify(monthlyDividendProfileRepository).saveAll(captor.capture());

    List<MonthlyDividendProfile> savedProfiles = new ArrayList<>();
    captor.getValue().forEach(savedProfiles::add);

    assertThat(savedProfiles)
        .extracting(MonthlyDividendProfile::getStockItemId)
        .containsExactly(tiger.getId(), plus.getId(), rise.getId());
    assertThat(savedProfiles)
        .extracting(MonthlyDividendProfile::getDisplayOrder)
        .containsExactly(1, 2, 3);
  }

  private StockItem createStockItem(String symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setId(UUID.randomUUID());
    stockItem.setSymbol(symbol);
    stockItem.setName(symbol + " ETF");
    stockItem.setMarket("KRX");
    return stockItem;
  }

  private MonthlyDividendProfile createProfile(StockItem stockItem, int displayOrder) {
    MonthlyDividendProfile profile = new MonthlyDividendProfile();
    profile.setId(UUID.randomUUID());
    profile.setStockItemId(stockItem.getId());
    profile.setDisplayOrder(displayOrder);
    return profile;
  }
}
