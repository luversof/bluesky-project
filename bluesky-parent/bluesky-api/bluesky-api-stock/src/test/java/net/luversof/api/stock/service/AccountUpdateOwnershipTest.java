package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.repository.AccountRepository;

/**
 * 계좌 저장이 '갱신' 일 때의 규칙을 고정한다.
 *
 * <p>{@code POST /api/account} 는 id 가 실려 오면 Spring Data JDBC 의 {@code save} 규칙에 따라 갱신이 된다. 검사 없이 두면
 * 두 가지가 샌다.
 *
 * <ul>
 *   <li><b>소유권 탈취</b> &mdash; 남의 계좌 id 를 자기 userId 로 보내면 그 계좌가 통째로 넘어온다. 계좌에 매달린 거래·배당도 따라간다. 실측(합성
 *       사용자): A 가 만든 계좌를 B 가 같은 id 로 저장하자 <b>A 0개 / B 1개</b> 가 됐다.
 *   <li><b>생성 시각 유실</b> &mdash; {@code createdDate} 를 빼고 보내면 NOT NULL 제약에 걸려 500 이 났다(PSQLException
 *       이 그대로 서버 오류로 나갔다). 갱신하는 쪽이 생성 시각을 알 이유가 없다.
 * </ul>
 */
class AccountUpdateOwnershipTest {

  private static final UUID OWNER = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();
  private static final Instant CREATED = Instant.parse("2026-01-02T03:04:05Z");

  private final AccountRepository accountRepository = Mockito.mock(AccountRepository.class);
  private final AccountService accountService = new AccountService();

  AccountUpdateOwnershipTest() {
    ReflectionTestUtils.setField(accountService, "accountRepository", accountRepository);
    Mockito.when(accountRepository.save(Mockito.any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  private Account stored(UUID id) {
    Account account = new Account();
    account.setId(id);
    account.setUserId(OWNER);
    account.setName("주인 계좌");
    account.setCreatedDate(CREATED);
    return account;
  }

  private Account request(UUID id, UUID userId, String name) {
    Account account = new Account();
    account.setId(id);
    account.setUserId(userId);
    account.setName(name);
    return account;
  }

  @Test
  void 남의_계좌_id_로_저장하면_거부한다() {
    UUID id = UUID.randomUUID();
    Mockito.when(accountRepository.findById(id)).thenReturn(Optional.of(stored(id)));

    assertThatThrownBy(() -> accountService.createAccount(request(id, OTHER, "가져가기")))
        .as("막지 않으면 계좌와 거기 매달린 거래·배당이 통째로 넘어간다")
        .isInstanceOf(RuntimeException.class);

    Mockito.verify(accountRepository, Mockito.never()).save(Mockito.any());
  }

  @Test
  void 본인_계좌는_생성시각을_이어받아_갱신한다() {
    UUID id = UUID.randomUUID();
    Mockito.when(accountRepository.findById(id)).thenReturn(Optional.of(stored(id)));

    Account saved = accountService.createAccount(request(id, OWNER, "주인 계좌"));

    assertThat(saved.getCreatedDate())
        .as("갱신하는 쪽이 생성 시각을 알 이유가 없다. 안 이어받으면 NOT NULL 제약으로 500 이 난다")
        .isEqualTo(CREATED);
  }

  @Test
  void 없는_id_로_갱신하면_400_이다() {
    UUID id = UUID.randomUUID();
    Mockito.when(accountRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> accountService.createAccount(request(id, OWNER, "없는 계좌")))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("not found");
  }

  /** id 가 없으면 예전처럼 그냥 새로 만든다(시트 가져오기가 이 경로를 쓴다). */
  @Test
  void id_가_없으면_조회하지_않고_새로_만든다() {
    Account saved = accountService.createAccount(request(null, OWNER, "새 계좌"));

    assertThat(saved.getId()).isNull();
    Mockito.verify(accountRepository, Mockito.never()).findById(Mockito.any());
    Mockito.verify(accountRepository).save(Mockito.any());
  }

  /** 이름에서 파생되는 과세이연 표시는 갱신에서도 유지된다. */
  @Test
  void 갱신에서도_과세이연이_붙는다() {
    UUID id = UUID.randomUUID();
    Mockito.when(accountRepository.findById(id)).thenReturn(Optional.of(stored(id)));

    Account saved = accountService.createAccount(request(id, OWNER, "한국투자증권 ISA"));

    assertThat(saved.getJsonConfig()).containsEntry("isTaxDeferred", true);
  }
}
