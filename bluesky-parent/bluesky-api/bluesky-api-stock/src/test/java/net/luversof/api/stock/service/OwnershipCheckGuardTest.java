package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 사용자 데이터를 쓰는 서비스가 소유자를 확인하는지 본다.
 *
 * <p>이 계층은 인증 없이 노출돼 있어 소유권 검사가 여기 없으면 아무 데도 없다. 실측 사고 형태(합성 사용자 2명): {@code POST /api/account} 가
 * id 를 받으면 저장이 갱신이 되는데 소유자를 보지 않아, B 가 A 의 계좌 id 로 저장하자 <b>A 0개 / B 1개</b> 로 계좌가 통째로 넘어갔다. 계좌에 매달린
 * 거래·배당도 함께 따라간다.
 *
 * <p>규칙: 저장된 행을 id 로 찾아 갱신하는 서비스 메서드는, 요청의 {@code userId} 와 저장된 행의 소유자가 같은지 확인해야 한다.
 */
class OwnershipCheckGuardTest {

  private static final Path SERVICE_DIR = Path.of("src/main/java/net/luversof/api/stock/service");

  private static final Path DOMAIN_DIR = Path.of("src/main/java/net/luversof/api/stock/domain");

  /** id 로 저장된 행을 꺼내는 호출(갱신 경로의 표식). */
  private static final Pattern FIND_BY_ID =
      Pattern.compile("\\w+Repository[\\s.\\n]*\\.findById\\(");

  /** 소유권을 확인하는 표현. */
  private static final List<String> OWNERSHIP_CHECKS =
      List.of(
          "INVALID_USER_ID",
          "getUserId().equals",
          "equals(stored.getUserId())",
          "findByUserIdAnd",
          "userId.equals");

  private String read(Path path) throws IOException {
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  /**
   * <b>소유자가 있는</b> 저장소를 id 로 꺼내는 호출이 있는지.
   *
   * <p>예전에는 {@code 아무 Repository.findById(} 면 위반으로 봤다. 그런데 종목처럼 사용자에 매이지 않는 참조 데이터도 같은 모양이라, 그 파일이
   * 다른 이유로 {@code userId} 를 한 번 언급하는 순간 지적이 났다(실측 2026-08-24: 원장 실적 과세비율을 읽는 메서드가 {@code
   * MonthlyDividendPayoutService} 에 들어오자, 그 파일의 유일한 {@code findById} 가 {@code
   * stockItemRepository.findById} 인데도 걸렸다).
   *
   * <p>후보 목록에서 빼지는 않는다 &mdash; 빼면 살아있음 검사가 훑는 범위가 함께 줄어든다. 위반 판정에서만 가린다.
   *
   * <p>도메인에 {@code userId} 가 있는 저장소만 센다. 소유자가 없는 행은 남의 것으로 넘어갈 수가 없다.
   */
  private boolean hasOwnedFindById(String source) throws IOException {
    Matcher matcher = FIND_BY_ID.matcher(source);
    while (matcher.find()) {
      String call = matcher.group();
      int at = call.indexOf("Repository");
      String repository = call.substring(0, at);
      String domain = repository.substring(repository.lastIndexOf('.') + 1).trim();
      domain = Character.toUpperCase(domain.charAt(0)) + domain.substring(1);
      Path domainFile = DOMAIN_DIR.resolve(domain + ".java");
      if (!Files.exists(domainFile) || read(domainFile).contains("userId")) {
        // 도메인을 못 찾으면 안전한 쪽(소유자가 있다)으로 본다.
        return true;
      }
    }
    return false;
  }

  /** 갱신 경로가 있어 소유권 검사가 필요한 서비스. */
  private List<String> candidates() throws IOException {
    List<String> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(SERVICE_DIR)) {
      for (Path file : files.filter(p -> p.toString().endsWith("Service.java")).sorted().toList()) {
        String source = read(file);
        // 저장하지 않는 서비스는 대상이 아니다.
        if (!source.contains("Repository.save(") && !source.contains(".save(")) {
          continue;
        }
        // 사용자 소유 데이터가 아닌 것(전역 참조 데이터)은 소유자가 없다.
        if (!source.contains("userId")) {
          continue;
        }
        Matcher matcher = FIND_BY_ID.matcher(source);
        if (!matcher.find()) {
          continue;
        }
        found.add(file.getFileName().toString());
      }
    }
    return found;
  }

  /** 갱신 경로가 있는 서비스에서 소유권 검사가 보이지 않는 것. */
  private List<String> unguarded() throws IOException {
    List<String> found = new ArrayList<>();
    for (String name : candidates()) {
      String source = read(SERVICE_DIR.resolve(name));
      if (!hasOwnedFindById(source)) {
        // 꺼내는 행에 소유자가 없으면(종목 같은 전역 참조) 남의 것으로 넘어갈 수가 없다.
        continue;
      }
      if (OWNERSHIP_CHECKS.stream().noneMatch(source::contains)) {
        found.add(name);
      }
    }
    return found;
  }

  @Test
  void 갱신_경로가_있는_서비스는_소유자를_확인한다() throws IOException {
    assertThat(unguarded())
        .as("id 로 찾아 갱신하면서 소유자를 보지 않으면 남의 데이터를 가져올 수 있다" + " (실측: 계좌가 통째로 다른 사용자에게 넘어갔다)")
        .isEmpty();
  }

  /**
   * 검사가 실제로 대상을 훑고 있는지.
   *
   * <p>위 검사는 "찾은 것 중 가드가 없는 것" 이 비어 있으면 통과한다. 그래서 <b>하나도 못 찾으면</b> 아무것도 지키지 않으면서 초록불이 된다. 실측
   * 2026-08-24: {@code FIND_BY_ID} 정규식을 무력화하자 후보가 0 개가 됐는데도 이 클래스가 2/2 통과했다.
   *
   * <p>개수만 세면 이름이 바뀌어도 통과하므로, 소유자를 가진 것으로 알려진 두 서비스가 <b>실제로 후보에 들어오는지</b>까지 본다.
   */
  @Test
  void 검사가_갱신_경로를_실제로_찾는다() throws IOException {
    List<String> candidates = candidates();
    assertThat(candidates)
        .as("갱신 경로가 있는 서비스를 하나도 찾지 못했다 - 탐지식이 낡았다(검사가 무력해진다)")
        .contains("AccountService.java", "MonthlyDividendSnapshotService.java");
  }

  /** 계좌 저장에 소유권 검사와 생성시각 승계가 실제로 들어 있는지 못 박는다. */
  @Test
  void 계좌_저장은_소유자와_생성시각을_확인한다() throws IOException {
    String source = read(SERVICE_DIR.resolve("AccountService.java"));
    assertThat(source)
        .as("id 가 실려 오면 저장은 갱신이다. 소유자를 보지 않으면 계좌가 넘어간다")
        .contains("account.getId() != null")
        .contains("stored.getUserId()")
        .contains("account.setCreatedDate(stored.getCreatedDate())");
  }
}
