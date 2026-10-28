package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import net.luversof.api.stock.constant.TradeType;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.api.stock.web.dto.response.LedgerImportResult;
import net.luversof.app.google.stock.domain.GoogleSheetDividend;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;
import net.luversof.app.google.stock.service.StockGoogleSheetService;

/**
 * 시트 가져오기는 고칠 수 없는 행만 버리고, 왜 버렸는지 숨기지 않는다(2026-10-27 감사).
 *
 * <p>감사 전: 계좌 칸이 빈 매매 행 하나가 계좌 없는 Trade 로 담겨 saveAll 전체가 NOT NULL 위반으로 실패했고, 배당은 모르는 종목명마다 DIV+난수
 * 심볼 종목을 새로 만들어 붙여 "다 들어갔다" 로 보고했다.
 */
@ExtendWith(MockitoExtension.class)
class LedgerImportDroppedRowsTest {

  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");
  private static final UUID ACCOUNT = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

  @Mock StockGoogleSheetService stockGoogleSheetService;
  @Mock StockItemRepository stockItemRepository;
  @Mock TradeRepository tradeRepository;
  @Mock DividendRepository dividendRepository;
  @Mock AccountService accountService;

  private StockAdminService service() {
    StockAdminService service = new StockAdminService();
    ReflectionTestUtils.setField(service, "stockGoogleSheetService", stockGoogleSheetService);
    ReflectionTestUtils.setField(service, "stockItemRepository", stockItemRepository);
    ReflectionTestUtils.setField(service, "tradeRepository", tradeRepository);
    ReflectionTestUtils.setField(service, "dividendRepository", dividendRepository);
    ReflectionTestUtils.setField(service, "accountService", accountService);
    return service;
  }

  private static StockItem samsung() {
    StockItem samsung = new StockItem();
    samsung.setId(UUID.randomUUID());
    samsung.setName("삼성전자");
    return samsung;
  }

  private static Account account() {
    Account account = new Account();
    account.setId(ACCOUNT);
    account.setUserId(USER);
    account.setName("KB증권 위탁");
    return account;
  }

  private static GoogleSheetTrade trade(String account) {
    GoogleSheetTrade row = new GoogleSheetTrade();
    row.set날짜(Instant.parse("2026-10-20T00:00:00Z"));
    row.set종목("삼성전자");
    row.set구분("매수");
    row.set계좌(account);
    row.set매매가(new BigDecimal("70000"));
    row.set매매_수량(1);
    return row;
  }

  private static GoogleSheetDividend dividend(String stockName) {
    GoogleSheetDividend row = new GoogleSheetDividend();
    row.set지급일("2026-10-15");
    row.set종목(stockName);
    row.set계좌("KB증권 위탁");
    row.set주식수(10);
    row.set분배금액(new BigDecimal("100"));
    row.set배당금(new BigDecimal("1000"));
    return row;
  }

  @Test
  @SuppressWarnings("unchecked")
  void 계좌_칸이_빈_매매_행은_그_행만_버린다() {
    when(stockItemRepository.findAll()).thenReturn(List.of(samsung()));
    when(accountService.findByUserId(USER)).thenReturn(List.of(account()));
    when(stockGoogleSheetService.getGoogleSheetTradeList(USER))
        .thenReturn(new ArrayList<>(List.of(trade("KB증권 위탁"), trade(" "), trade(null))));

    LedgerImportResult result = service().tradeBulkInsert(USER);

    verify(tradeRepository, never()).deleteAll();
    verify(tradeRepository).deleteByAccountId(ACCOUNT);

    ArgumentCaptor<Iterable<Trade>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(tradeRepository).saveAll(saved.capture());
    assertThat(saved.getValue())
        .hasSize(1)
        .allSatisfy(t -> assertThat(t.getAccountId()).isEqualTo(ACCOUNT));
    assertThat(result.droppedCount()).isEqualTo(2);
    assertThat(result.unknownStockNames()).as("계좌 탓에 버린 행은 종목 이름 탓이 아니다").isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void 구분_칸은_다듬어_매수_매도만_받고_나머지는_버린다() {
    when(stockItemRepository.findAll()).thenReturn(List.of(samsung()));
    when(accountService.findByUserId(USER)).thenReturn(List.of(account()));
    GoogleSheetTrade buyWithSpace = trade("KB증권 위탁");
    buyWithSpace.set구분("매수 ");
    GoogleSheetTrade sell = trade("KB증권 위탁");
    sell.set구분("매도");
    GoogleSheetTrade blank = trade("KB증권 위탁");
    blank.set구분("");
    GoogleSheetTrade typo = trade("KB증권 위탁");
    typo.set구분("매수수");
    when(stockGoogleSheetService.getGoogleSheetTradeList(USER))
        .thenReturn(new ArrayList<>(List.of(buyWithSpace, sell, blank, typo)));

    LedgerImportResult result = service().tradeBulkInsert(USER);

    ArgumentCaptor<Iterable<Trade>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(tradeRepository).saveAll(saved.capture());
    assertThat(saved.getValue())
        .extracting(Trade::getType)
        .as("예전엔 '매수 ' · 빈칸 · 오타가 전부 매도로 들어갔다")
        .containsExactly(TradeType.BUY, TradeType.SELL);
    assertThat(result.droppedCount()).isEqualTo(2);
  }

  @Test
  @SuppressWarnings("unchecked")
  void 모르는_종목명의_배당은_만들지_않고_모르는_이름으로_알린다() {
    when(stockItemRepository.findAll()).thenReturn(List.of(samsung()));
    when(accountService.findByUserId(USER)).thenReturn(List.of(account()));
    when(stockGoogleSheetService.getGoogleSheetDividendList(USER))
        .thenReturn(List.of(dividend("삼성전자"), dividend("삼성전자우(오타)")));

    LedgerImportResult result = service().dividendBulkInsert(USER);

    verify(dividendRepository, never()).deleteAll();
    verify(dividendRepository).deleteByAccountId(ACCOUNT);

    verify(stockItemRepository, never()).save(any(StockItem.class));
    ArgumentCaptor<Iterable<Dividend>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(dividendRepository).saveAll(saved.capture());
    assertThat(saved.getValue()).hasSize(1);
    assertThat(result.unknownStockNames()).containsExactly("삼성전자우(오타)");
  }
}
