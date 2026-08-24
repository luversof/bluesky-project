package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.repository.AccountRepository;

/**
 * 계좌를 갱신할 때, 호출자가 보내지 않은 설정 키가 사라지지 않는지 본다.
 *
 * <p>실측 사고 2026-08-22: {@code manualPrincipalAmount}(직접 입력한 투자원금)는 이 앱에 <b>쓰기 경로가 없다</b>. 오직 직접
 * POST 로만 넣는데, 다른 목적으로 계좌를 한 번 저장하면 {@code jsonConfig} 가 통째로 덮여 사라졌다. 세 계좌(ISA · 연금저축1 · 연금저축2)가
 * 그렇게 지워졌고, 그 뒤 요약 화면의 원금이 보유원가 폴백으로 바뀌어 632,223,826 원이 됐다(원래 621,595,903 원, 10,627,923 원 과대).
 *
 * <p>{@code createdDate} 를 저장된 행에서 이어받는 것과 같은 이유다 &mdash; 계좌 이름 하나 고치려는 호출자가 그 계좌에 어떤 설정이 붙어 있는지 알
 * 이유가 없다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountJsonConfigCarryOverTest {

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ACCOUNT_ID = UUID.randomUUID();

  @Mock private AccountRepository accountRepository;

  private AccountService service() {
    AccountService service = new AccountService();
    service.setAccountRepository(accountRepository);
    return service;
  }

  private Account account(String name, Map<String, Object> jsonConfig) {
    Account account = new Account();
    account.setId(ACCOUNT_ID);
    account.setUserId(USER_ID);
    account.setName(name);
    account.setJsonConfig(jsonConfig == null ? null : new HashMap<>(jsonConfig));
    return account;
  }

  private Account saved(Account incoming, Account stored) {
    when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(stored));
    when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));
    return service().createAccount(incoming);
  }

  @Test
  void 보내지_않은_설정_키는_저장된_값을_이어받는다() {
    Account stored =
        account(
            "한국투자증권 ISA",
            Map.of("isTaxDeferred", true, "manualPrincipalAmount", new BigDecimal("60000000")));
    // 이름만 고치려는 호출. jsonConfig 는 아예 보내지 않았다.
    Account incoming = account("한국투자증권 ISA", null);

    Account result = saved(incoming, stored);

    assertThat(result.getJsonConfig())
        .as("보내지 않았다고 지워지면, 되살릴 방법이 없는 값이 사라진다")
        .containsEntry("manualPrincipalAmount", new BigDecimal("60000000"));
  }

  @Test
  void 보낸_값이_저장된_값을_이긴다() {
    Account stored =
        account("한국투자증권 ISA", Map.of("manualPrincipalAmount", new BigDecimal("60000000")));
    Account incoming =
        account("한국투자증권 ISA", Map.of("manualPrincipalAmount", new BigDecimal("70000000")));

    assertThat(saved(incoming, stored).getJsonConfig())
        .containsEntry("manualPrincipalAmount", new BigDecimal("70000000"));
  }

  /** 이름에서 파생되는 키는 이어받지 않는다. 이어받으면 이름을 바꿔도 옛 값이 남는다. */
  @Test
  void 과세이연은_이름에서_다시_만들어진다() {
    Account stored = account("한국투자증권 ISA", Map.of("isTaxDeferred", true));
    // 과세 계좌로 이름을 바꿔 저장한다.
    Account incoming = account("한국투자증권 위탁", null);

    assertThat(saved(incoming, stored).getJsonConfig())
        .as("이름이 조건을 만족하지 않으면 과세이연은 지워져야 한다")
        .isNull();
  }

  @Test
  void 새_계좌는_이어받을_것이_없다() {
    Account incoming = account("한국투자증권 위탁", null);
    incoming.setId(null);
    when(accountRepository.save(any(Account.class))).thenAnswer(call -> call.getArgument(0));

    assertThat(service().createAccount(incoming).getJsonConfig()).isNull();
  }
}
