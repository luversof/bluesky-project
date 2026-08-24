package net.luversof.web.gate.stock.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import net.luversof.web.gate.stock.domain.Account;
import net.luversof.web.gate.stock.dto.request.TradeProfitRequest;

/**
 * 계좌 필터를 보내기 전에 이 사용자 계좌로 좁히는 규칙을 고정한다.
 *
 * <p>두 가지가 함께 걸려 있다.
 *
 * <ul>
 *   <li>남의 계좌 id 가 섞여 오면 백엔드가 요청을 거절해 <b>화면이 통째로 오류</b>가 됐다.
 *   <li>좁힌 결과가 빈 목록이면 그대로 보낼 수 없다 &mdash; 파라미터가 아예 빠져 '필터 없음'(= 전체)이 되어 오히려 <b>전부 보인다</b>(실측 사례: 없는
 *       태그로 걸렀는데 전체 106행이 그대로 나왔다).
 * </ul>
 *
 * <p>예전에는 같은 코드가 자산성장·포트폴리오(2곳)에 복사돼 있었다.
 */
class OwnedAccountNarrowingTest {

  private static final UUID OWNED = UUID.randomUUID();
  private static final UUID OTHER = UUID.randomUUID();

  private static final class Probe extends StockBaseHtmxController {
    Probe() {
      super(null, null, null, null, null, null);
    }
  }

  private final Probe probe = new Probe();

  private CompletableFuture<List<Account>> accounts() {
    return CompletableFuture.completedFuture(
        List.of(new Account(OWNED, null, "한국투자증권 위탁", null, null)));
  }

  /** 필터가 없으면 좁힐 것이 없고, 계좌 조회를 기다리지도 않는다. */
  @Test
  void 필터가_없으면_계좌_조회를_기다리지_않는다() {
    var request = new TradeProfitRequest();
    CompletableFuture<List<Account>> never = new CompletableFuture<>();

    assertThat(probe.narrowToOwnedAccounts(request, never))
        .as("필터가 없는데 기다리면 화면이 그만큼 느려진다")
        .isFalse();
    assertThat(request.getAccountIdList()).isNull();
  }

  @Test
  void 내_계좌만_남긴다() {
    var request = new TradeProfitRequest();
    request.setAccountIdList(List.of(OWNED, OTHER));

    boolean empty = probe.narrowToOwnedAccounts(request, accounts());

    assertThat(empty).isFalse();
    assertThat(request.getAccountIdList())
        .as("남의 계좌 id 를 그대로 보내면 백엔드가 요청을 거절해 화면이 오류가 된다")
        .containsExactly(OWNED);
  }

  /** 남는 계좌가 없으면 '전체'가 아니라 '없음'이어야 한다. */
  @Test
  void 남는_계좌가_없으면_조회를_건너뛰라고_알린다() {
    var request = new TradeProfitRequest();
    request.setAccountIdList(List.of(OTHER));

    boolean empty = probe.narrowToOwnedAccounts(request, accounts());

    assertThat(empty).as("이 값이 거짓이면 파라미터가 빠져 '필터 없음 = 전체'가 된다").isTrue();
    assertThat(request.getAccountIdList()).isEmpty();
  }

  /** 좁히는 규칙이 한 곳에만 있는지. 복사되면 그 화면만 다르게 동작한다. */
  @Test
  void 좁히는_규칙은_한_곳에만_있다() throws IOException {
    Path root = Path.of("src/main/java/net/luversof/web/gate/stock");
    int copies = 0;
    try (Stream<Path> files = Files.walk(root)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        copies += countOccurrences(source, "boolean hasAccountFilter");
      }
    }
    assertThat(copies).as("계좌 좁히기는 narrowToOwnedAccounts 에만 있어야 한다").isEqualTo(1);
  }

  private int countOccurrences(String source, String needle) {
    int found = 0;
    int at = source.indexOf(needle);
    while (at >= 0) {
      found++;
      at = source.indexOf(needle, at + needle.length());
    }
    return found;
  }
}
