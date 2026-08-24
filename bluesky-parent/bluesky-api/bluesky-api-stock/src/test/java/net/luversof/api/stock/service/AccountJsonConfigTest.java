package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * 계좌 설정({@code jsonConfig})을 다루는 규칙을 고정한다.
 *
 * <p>이 맵에는 화면 수치를 바꾸는 값이 들어간다 &mdash; {@code isTaxDeferred}(과세이연 계좌는 세전을 세후로 본다)와 {@code
 * manualPrincipalAmount}(수익률 기준을 계산 원가 대신 실제 납입액으로 바꾼다. 실측: 연금저축1 이 -9.85% -> +13.80%).
 *
 * <p>두 값의 성격이 다르다는 것이 핵심이다.
 *
 * <ul>
 *   <li>{@code isTaxDeferred} 는 계좌 <b>이름</b>에서 파생된다(ISA/연금 포함). 그래서 계좌를 다시 만들어도 저절로 살아난다.
 *   <li>{@code manualPrincipalAmount} 는 사람이 넣는 값이라 <b>어디에서도 다시 만들어지지 않는다</b>. 계좌가 지워지면 그대로 사라진다 (실측
 *       사고 2026-08-22: 계좌 삭제 후 시트 재가져오기로 원장은 복구했지만 이 값 3건은 복구되지 않았다).
 * </ul>
 */
class AccountJsonConfigTest {

  private static final Path SERVICE =
      Path.of("src/main/java/net/luversof/api/stock/service/AccountService.java");

  private String source() throws IOException {
    assertThat(SERVICE).as("파일이 옮겨졌다: " + SERVICE).exists();
    return Files.readString(SERVICE, StandardCharsets.UTF_8);
  }

  @Test
  void 과세이연은_계좌명에서_파생된다() throws IOException {
    assertThat(source()).as("이 규칙이 사라지면 계좌를 다시 만들 때 과세이연이 살아나지 않는다").contains("isTaxDeferred");
  }

  /**
   * 저장은 넘어온 설정을 지우지 않고 이어붙여야 한다.
   *
   * <p>{@code appendJsonConfig} 가 새 맵으로 갈아치우면 {@code manualPrincipalAmount} 처럼 사람이 넣은 값이 계좌를 저장할
   * 때마다 사라진다.
   */
  @Test
  void 기존_설정을_지우지_않고_이어붙인다() throws IOException {
    String source = source();
    assertThat(source)
        .as("기존 jsonConfig 를 이어받지 않으면 사람이 넣은 값이 저장할 때마다 사라진다")
        .contains("account.getJsonConfig() == null ? new HashMap<String, Object>()");
  }

  /**
   * 계좌 설정을 바꿀 수 있는 경로가 실제로 있는지.
   *
   * <p>실측: 갱신 전용 엔드포인트는 없지만 {@code POST /api/account} 가 {@code save()} 라, <b>id 와 createdDate 를 함께
   * 보내면 제자리 갱신</b>이 된다(합성 계좌로 확인: 계좌 수 그대로 1개, jsonConfig 만 바뀜). createdDate 를 빼면 NOT NULL 제약으로 500
   * 이 난다. 이 경로가 막히면 사람이 넣은 설정을 되돌릴 방법이 사라진다.
   */
  @Test
  void 계좌_저장은_생성과_갱신을_겸한다() throws IOException {
    assertThat(source())
        .as("save() 가 아니라 insert 전용이 되면 설정을 되돌릴 방법이 없어진다")
        .contains("accountRepository.save(account)");
  }

  /**
   * 파생값은 근거와 항상 같아야 한다.
   *
   * <p>{@code isTaxDeferred} 를 붙이기만 하면, 비과세 계좌의 이름을 바꿔 저장했을 때 옛 값이 남는다. 그 계좌는 이제 과세인데도 배당 화면이 "세후 =
   * 세전" 으로 그려 실수령액이 실제보다 크게 나온다. 이 값의 쓰기 경로는 여기 하나뿐이라(게이트는 읽기만 한다) 여기서 지우지 않으면 되돌릴 방법이 없다.
   *
   * <p>소스 문자열이 아니라 실제 저장 동작으로 확인한다.
   */
  @Test
  void 이름에서_과세이연_조건이_사라지면_설정도_사라진다() {
    var service = serviceWithStubRepository();

    var account = new net.luversof.api.stock.domain.Account();
    account.setUserId(java.util.UUID.randomUUID());
    account.setName("한국투자증권 ISA");
    var saved = service.createAccount(account);
    assertThat(saved.getJsonConfig()).containsEntry("isTaxDeferred", true);

    var renamed = new net.luversof.api.stock.domain.Account();
    renamed.setUserId(account.getUserId());
    renamed.setName("한국투자증권 일반");
    renamed.setJsonConfig(new java.util.HashMap<>(saved.getJsonConfig()));
    var after = service.createAccount(renamed);

    // 남는 키가 없으면 설정 자체가 null 이 된다(빈 맵을 남겨 둘 이유가 없다).
    assertThat(after.getJsonConfig() == null || !after.getJsonConfig().containsKey("isTaxDeferred"))
        .as("이름이 조건을 만족하지 않는데 과세이연이 남아 있으면 세금을 뗀 배당이 세전으로 보인다")
        .isTrue();
  }

  /** 사람이 넣은 값은 이름과 무관하므로 건드리지 않는다. */
  @Test
  void 사람이_넣은_설정은_이름을_바꿔도_남는다() {
    var service = serviceWithStubRepository();

    var account = new net.luversof.api.stock.domain.Account();
    account.setUserId(java.util.UUID.randomUUID());
    account.setName("한국투자증권 일반");
    var config = new java.util.HashMap<String, Object>();
    config.put("manualPrincipalAmount", 60000000);
    account.setJsonConfig(config);

    var saved = service.createAccount(account);

    assertThat(saved.getJsonConfig()).containsEntry("manualPrincipalAmount", 60000000);
    assertThat(saved.getJsonConfig()).doesNotContainKey("isTaxDeferred");
  }

  /** 저장만 흉내 내는 최소 리포지토리(스프링 없이 동작을 본다). */
  private AccountService serviceWithStubRepository() {
    var service = new AccountService();
    service.setAccountRepository(
        (net.luversof.api.stock.repository.AccountRepository)
            java.lang.reflect.Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class<?>[] {net.luversof.api.stock.repository.AccountRepository.class},
                (proxy, method, args) ->
                    switch (method.getName()) {
                      case "save" -> args[0];
                      case "findById" -> java.util.Optional.empty();
                      case "toString" -> "stub";
                      case "hashCode" -> 0;
                      case "equals" -> proxy == args[0];
                      default -> null;
                    }));
    return service;
  }
}
