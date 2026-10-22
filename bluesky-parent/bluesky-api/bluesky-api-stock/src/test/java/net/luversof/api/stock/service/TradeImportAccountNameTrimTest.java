package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
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

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.domain.Trade;
import net.luversof.api.stock.repository.StockItemRepository;
import net.luversof.api.stock.repository.TradeRepository;
import net.luversof.app.google.stock.domain.GoogleSheetTrade;
import net.luversof.app.google.stock.service.StockGoogleSheetService;

/**
 * 시트에서 거래를 가져올 때 계좌 이름의 앞뒤 공백은 같은 계좌로 본다.
 *
 * <p>배당 가져오기는 이름을 다듬어 맞추는데 거래 가져오기만 그대로 썼다(2026-10-21 정리 조사). 그러면 시트 한 칸에 공백이 붙는 순간 같은 계좌가 하나 더
 * 만들어지고 그 행의 거래가 새 계좌로 간다.
 */
@ExtendWith(MockitoExtension.class)
class TradeImportAccountNameTrimTest {

  private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Mock StockGoogleSheetService stockGoogleSheetService;
  @Mock StockItemRepository stockItemRepository;
  @Mock TradeRepository tradeRepository;
  @Mock AccountService accountService;

  private static GoogleSheetTrade row(String account) {
    GoogleSheetTrade row = new GoogleSheetTrade();
    row.set날짜(Instant.parse("2026-10-20T00:00:00Z"));
    row.set종목("삼성전자");
    row.set구분("매수");
    row.set계좌(account);
    row.set매매가(new BigDecimal("70000"));
    row.set매매_수량(1);
    return row;
  }

  @Test
  @SuppressWarnings("unchecked")
  void 공백만_다른_계좌_이름은_한_계좌다() {
    StockItem samsung = new StockItem();
    samsung.setId(UUID.randomUUID());
    samsung.setName("삼성전자");
    when(stockItemRepository.findAll()).thenReturn(List.of(samsung));
    when(stockGoogleSheetService.getGoogleSheetTradeList(USER))
        .thenReturn(new ArrayList<>(List.of(row("KB증권 위탁"), row(" KB증권 위탁 "))));
    when(accountService.findByUserId(USER)).thenReturn(List.of());
    UUID created = UUID.randomUUID();
    when(accountService.createAccount(any(Account.class)))
        .thenAnswer(
            invocation -> {
              Account account = invocation.getArgument(0);
              account.setId(created);
              return account;
            });

    StockAdminService service = new StockAdminService();
    ReflectionTestUtils.setField(service, "stockGoogleSheetService", stockGoogleSheetService);
    ReflectionTestUtils.setField(service, "stockItemRepository", stockItemRepository);
    ReflectionTestUtils.setField(service, "tradeRepository", tradeRepository);
    ReflectionTestUtils.setField(service, "accountService", accountService);

    service.tradeBulkInsert(USER);

    ArgumentCaptor<Account> createdAccount = ArgumentCaptor.forClass(Account.class);
    verify(accountService, times(1)).createAccount(createdAccount.capture());
    assertThat(createdAccount.getValue().getName()).isEqualTo("KB증권 위탁");

    ArgumentCaptor<Iterable<Trade>> saved = ArgumentCaptor.forClass(Iterable.class);
    verify(tradeRepository).saveAll(saved.capture());
    assertThat(saved.getValue())
        .hasSize(2)
        .allSatisfy(t -> assertThat(t.getAccountId()).isEqualTo(created));
  }
}
