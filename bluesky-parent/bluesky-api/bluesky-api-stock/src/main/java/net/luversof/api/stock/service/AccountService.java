package net.luversof.api.stock.service;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.repository.AccountRepository;
import net.luversof.api.stock.repository.DividendRepository;
import net.luversof.api.stock.repository.TradeRepository;

@Service
public class AccountService {

  @Autowired private AccountRepository accountRepository;

  @Autowired private DividendRepository dividendRepository;

  @Autowired private TradeRepository tradeRepository;

  @Autowired
  public void setAccountRepository(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  public void setDividendRepository(DividendRepository dividendRepository) {
    this.dividendRepository = dividendRepository;
  }

  public void setTradeRepository(TradeRepository tradeRepository) {
    this.tradeRepository = tradeRepository;
  }

  /**
   * 계좌를 만든다.
   *
   * <p>이름과 소유자는 반드시 있어야 한다. 예전에는 검사 없이 {@code appendJsonConfig} 가 {@code
   * account.getName().contains(...)} 를 부르는 바람에, 이름 없는 요청이 <b>500 NullPointerException</b> 으로
   * 끝났다(실측: {@code POST /api/account} 에 {@code {"userId": ...}} 만 보내면 500). 이 서비스는 인증 없이 노출돼 있어 잘못된
   * 요청이 그대로 서버 오류 알림이 된다.
   *
   * <p>소유자가 없는 계좌는 저장돼도 어느 사용자 조회에도 걸리지 않는 고아 데이터가 된다.
   */
  public Account createAccount(Account account) {
    if (account == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account is required");
    }
    if (!StringUtils.hasText(account.getName())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "account name is required");
    }
    if (account.getUserId() == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
    }

    // id 가 실려 오면 저장은 '갱신' 이 된다(Spring Data JDBC 의 save 규칙). 그대로 두면 두 가지가 샌다.
    //
    //  1) 남의 계좌 id 를 자기 userId 로 보내면 그 계좌가 통째로 넘어온다. 계좌에 매달린 거래·배당도
    //     따라간다(실측: 합성 사용자 A 가 만든 계좌를 B 가 같은 id 로 저장하자 A 0개 / B 1개가 됐다).
    //  2) createdDate 를 빼고 보내면 NOT NULL 제약에 걸려 500 이 난다(실측: PSQLException 이 그대로
    //     서버 오류로 나갔다). 갱신하는 쪽이 생성 시각을 알 이유가 없다.
    //
    // 그래서 갱신일 때는 저장된 행을 먼저 읽어, 소유자가 같은지 확인하고 생성 시각을 이어받는다.
    if (account.getId() != null) {
      var stored =
          accountRepository
              .findById(account.getId())
              .orElseThrow(
                  () ->
                      new ResponseStatusException(HttpStatus.BAD_REQUEST, "account is not found"));
      if (!account.getUserId().equals(stored.getUserId())) {
        StockErrorCode.INVALID_USER_ID.throwException(account.getUserId(), account.getId());
      }
      account.setCreatedDate(stored.getCreatedDate());
      carryOverJsonConfig(stored, account);
    }

    appendJsonConfig(account);
    return accountRepository.save(account);
  }

  /**
   * 계좌 이름에서 파생되는 설정을 채운다.
   *
   * <p>{@code isTaxDeferred} 는 <b>이름에서만</b> 나온다 - 다른 쓰기 경로가 없고, 게이트는 읽기만 한다(배당 화면이 이 값이 참이면 "세후 =
   * 세전" 으로 그린다).
   *
   * <p>그래서 붙이기만 해서는 안 된다. 비과세 계좌의 이름을 바꿔 저장하면 옛 값이 그대로 남아, 과세 계좌인데도 화면이 세금을 떼지 않은 금액을 실수령액으로 보여준다.
   * 이름이 조건을 만족하지 않으면 지운다 - 파생값은 근거와 항상 같아야 한다.
   *
   * <p>{@code manualPrincipalAmount} 같은 다른 키는 건드리지 않는다.
   */
  /**
   * 갱신하는 쪽이 보내지 않은 설정 키는 저장된 값을 이어받는다.
   *
   * <p>{@code createdDate} 를 이어받는 것과 같은 이유다 &mdash; 계좌 이름 하나 고치려는 호출자가 그 계좌에 어떤 설정이 붙어 있는지 알 이유가
   * 없다. 그런데 예전에는 {@code jsonConfig} 를 통째로 덮어써서, 보내지 않은 키가 조용히 사라졌다.
   *
   * <p>실측 2026-08-23: {@code manualPrincipalAmount}(직접 입력한 투자원금)는 이 앱에 <b>쓰기 경로가 아예 없다</b>. 오직 직접
   * POST 로만 넣는데, 다른 목적으로 계좌를 한 번 저장하면 사라진다. 실제로 세 계좌(ISA · 연금저축1 · 연금저축2)에서 그렇게 지워졌고, 그 뒤 요약 화면의
   * 원금이 보유원가 폴백으로 바뀌어 <b>1.7% 과대</b>가 됐다.
   *
   * <p>이름에서 파생되는 {@code isTaxDeferred} 는 따로 걸러 내지 않는다. 이 병합 <b>뒤에</b> {@link #appendJsonConfig} 가
   * 이름을 보고 항상 다시 넣거나 지우므로, 여기서 이어받든 말든 결과가 같다(제외 목록을 뒀다가 변이 테스트로 그 분기가 아무 일도 하지 않는 것을 확인하고 지웠다).
   *
   * <p>대신 키를 <b>지우려면</b> 값을 명시적으로 비워 보내야 한다. 빠뜨려서 지워지는 것보다 이쪽이 안전하다.
   */
  private void carryOverJsonConfig(Account stored, Account account) {
    if (stored.getJsonConfig() == null || stored.getJsonConfig().isEmpty()) {
      return;
    }
    var merged =
        account.getJsonConfig() == null
            ? new HashMap<String, Object>()
            : new HashMap<>(account.getJsonConfig());
    stored.getJsonConfig().forEach((key, value) -> merged.putIfAbsent(key, value));
    account.setJsonConfig(merged.isEmpty() ? null : merged);
  }

  private void appendJsonConfig(Account account) {
    var jsonConfig =
        account.getJsonConfig() == null ? new HashMap<String, Object>() : account.getJsonConfig();
    if (account.getName().contains("ISA") || account.getName().contains("연금")) {
      jsonConfig.put("isTaxDeferred", true);
    } else {
      jsonConfig.remove("isTaxDeferred");
    }

    account.setJsonConfig(jsonConfig.isEmpty() ? null : jsonConfig);
  }

  public Optional<Account> findById(UUID id) {
    return accountRepository.findById(id);
  }

  public List<Account> findByIdIn(List<UUID> idList) {
    return accountRepository.findByIdIn(idList);
  }

  public List<Account> findByUserId(UUID userId) {
    return accountRepository.findByUserId(userId);
  }

  /**
   * UserId 기준 데이터 일괄 삭제
   *
   * @param userId
   */
  @Transactional
  public void deleteAllByUserId(UUID userId) {
    var accountList = accountRepository.findByUserId(userId);
    accountList.forEach(
        account -> {
          dividendRepository.deleteByAccountId(account.getId());
          tradeRepository.deleteByAccountId(account.getId());
        });
    accountRepository.deleteAll(accountList);
  }
}
