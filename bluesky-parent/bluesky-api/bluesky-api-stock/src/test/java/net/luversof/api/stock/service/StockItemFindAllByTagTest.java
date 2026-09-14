package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemTag;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.StockItemTagRepository;

/**
 * 태그로 종목을 찾는 조회. 화면의 태그 칩(ETF · 리츠 · 월배당 …)이 이 결과를 쓴다.
 *
 * <p>테스트가 없었다(실측 2026-09-12: api-stock service/util/support 에서 테스트가 한 번도 부르지 않는 public 메서드 14 개 중
 * 하나).
 *
 * <p>규칙 넷: 빈 값이면 <b>저장소를 묻지도 않고</b> 빈 목록 / 앞뒤 공백은 떼고 묻는다 / 같은 종목이 태그 표에 여러 줄 있어도 한 번만 묻는다 / 돌려주는
 * 종목에는 태그가 붙어 있다(칩을 그려야 하므로).
 */
@ExtendWith(MockitoExtension.class)
class StockItemFindAllByTagTest {

  @Mock private StockItemRepository stockItemRepository;

  @Mock private StockItemTagRepository stockItemTagRepository;

  @InjectMocks private StockItemService stockItemService;

  private StockItem item(UUID id, String symbol) {
    StockItem stockItem = new StockItem();
    stockItem.setId(id);
    stockItem.setSymbol(symbol);
    stockItem.setName(symbol + " 이름");
    return stockItem;
  }

  private StockItemTag tag(UUID stockItemId, String value) {
    StockItemTag stockItemTag = new StockItemTag();
    stockItemTag.setStockItemId(stockItemId);
    stockItemTag.setTag(value);
    return stockItemTag;
  }

  @Test
  void 빈_태그는_저장소를_묻지도_않는다() {
    assertThat(stockItemService.findAllByTag(null)).isEmpty();
    assertThat(stockItemService.findAllByTag("")).isEmpty();
    assertThat(stockItemService.findAllByTag("   ")).isEmpty();
    verify(stockItemTagRepository, never()).findByTag(anyString());
    verify(stockItemRepository, never()).findAllById(any());
  }

  @Test
  void 앞뒤_공백은_떼고_묻는다() {
    UUID id = UUID.randomUUID();
    when(stockItemTagRepository.findByTag("ETF")).thenReturn(List.of(tag(id, "ETF")));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(item(id, "005930")));
    when(stockItemTagRepository.findByStockItemIdIn(any())).thenReturn(List.of(tag(id, "ETF")));

    assertThat(stockItemService.findAllByTag("  ETF  ")).hasSize(1);

    ArgumentCaptor<String> asked = ArgumentCaptor.forClass(String.class);
    verify(stockItemTagRepository).findByTag(asked.capture());
    assertThat(asked.getValue()).isEqualTo("ETF");
  }

  /** 태그 표에 같은 종목이 여러 줄이어도 종목 조회는 한 번, 아이디도 한 번만 넘긴다. */
  @Test
  void 같은_종목이_여러_줄이어도_한_번만_묻는다() {
    UUID id = UUID.randomUUID();
    when(stockItemTagRepository.findByTag("리츠"))
        .thenReturn(List.of(tag(id, "리츠"), tag(id, "리츠"), tag(null, "리츠")));
    when(stockItemRepository.findAllById(any())).thenReturn(List.of(item(id, "329200")));
    when(stockItemTagRepository.findByStockItemIdIn(any())).thenReturn(List.of(tag(id, "리츠")));

    assertThat(stockItemService.findAllByTag("리츠")).hasSize(1);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Iterable<UUID>> ids = ArgumentCaptor.forClass(Iterable.class);
    verify(stockItemRepository, times(1)).findAllById(ids.capture());
    assertThat(ids.getValue()).containsExactly(id);
  }

  @Test
  void 해당_태그가_없으면_종목을_묻지_않는다() {
    when(stockItemTagRepository.findByTag("없는태그")).thenReturn(List.of());

    assertThat(stockItemService.findAllByTag("없는태그")).isEmpty();
    verify(stockItemRepository, never()).findAllById(any());
  }

  /** 칩을 그리려면 태그가 함께 와야 한다 - 목록만 주면 화면에서 태그가 사라진다. */
  @Test
  void 돌려주는_종목에는_태그가_붙어_있다() {
    UUID first = UUID.randomUUID();
    UUID second = UUID.randomUUID();
    when(stockItemTagRepository.findByTag("월배당"))
        .thenReturn(List.of(tag(first, "월배당"), tag(second, "월배당")));
    when(stockItemRepository.findAllById(any()))
        .thenReturn(List.of(item(first, "476800"), item(second, "329200")));
    when(stockItemTagRepository.findByStockItemIdIn(any()))
        .thenReturn(List.of(tag(first, "월배당"), tag(first, "리츠"), tag(second, "월배당")));

    List<StockItem> found = stockItemService.findAllByTag("월배당");

    assertThat(found).hasSize(2);
    assertThat(
            found.stream().filter(x -> x.getId().equals(first)).findFirst().orElseThrow().getTags())
        .containsExactlyInAnyOrder("월배당", "리츠");
    assertThat(
            found.stream()
                .filter(x -> x.getId().equals(second))
                .findFirst()
                .orElseThrow()
                .getTags())
        .containsExactly("월배당");

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Set<UUID>> asked = ArgumentCaptor.forClass(Set.class);
    verify(stockItemTagRepository).findByStockItemIdIn(asked.capture());
    assertThat(asked.getValue()).containsExactlyInAnyOrder(first, second);
  }
}
