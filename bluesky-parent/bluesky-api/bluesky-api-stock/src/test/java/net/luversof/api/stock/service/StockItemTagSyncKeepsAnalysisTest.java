package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.StockItemTag;
import net.luversof.api.stock.repository.StockItemTagRepository;
import net.luversof.app.google.stock.domain.GoogleSheetStockItem;

/**
 * 종목 갱신이 시트에 없는 analysis 태그(분석 대상 추가가 붙인 것)를 지우지 않는지 본다.
 *
 * <p>2026-11-27: 태그를 deleteAll 로 지우고 시트로 다시 만들어, 종목 갱신 한 번에 analysis 태그 31 개가 사라질 상태였다.
 */
@ExtendWith(MockitoExtension.class)
class StockItemTagSyncKeepsAnalysisTest {

  @Mock private StockItemTagRepository stockItemTagRepository;

  @InjectMocks private StockAdminService service;

  @SuppressWarnings("unchecked")
  @Test
  void keepsAnalysisTagsAndRebuildsSheetTags() {
    StockItem item = new StockItem();
    item.setId(UUID.randomUUID());
    item.setSymbol("069500");
    item.setName("KODEX 200");
    StockItemTag analysis = tag(item.getId(), "analysis");
    StockItemTag oldSheet = tag(item.getId(), "ETF");
    when(stockItemTagRepository.findAll()).thenReturn(List.of(analysis, oldSheet));

    GoogleSheetStockItem row = new GoogleSheetStockItem();
    row.set종목코드("069500");
    row.set종목이름("KODEX 200");
    row.set태그("ETF, analysis");
    service.syncStockItemTags(List.of(row), List.of(item));

    ArgumentCaptor<Iterable<StockItemTag>> deleted = ArgumentCaptor.forClass(Iterable.class);
    verify(stockItemTagRepository).deleteAll(deleted.capture());
    assertThat(deleted.getValue()).containsExactly(oldSheet);

    // 시트에도 analysis 가 있으면 남은 행과 겹치지 않게(유니크 키) ETF 만 다시 넣는다.
    ArgumentCaptor<Iterable<StockItemTag>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(stockItemTagRepository).saveAll(saved.capture());
    assertThat(saved.getValue()).extracting(StockItemTag::getTag).containsExactly("ETF");
  }

  private static StockItemTag tag(UUID stockItemId, String name) {
    StockItemTag tag = new StockItemTag();
    tag.setId(UUID.randomUUID());
    tag.setStockItemId(stockItemId);
    tag.setTag(name);
    return tag;
  }
}
