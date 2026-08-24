package net.luversof.api.stock.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 사람이 넣은 값을 저장 한 번으로 잃지 않는지 본다.
 *
 * <p>실측 사고 2026-08-22: {@code manualPrincipalAmount}(직접 입력한 투자원금)는 이 앱에 쓰기 화면이 없어 직접 POST 로만 넣는데,
 * 계좌를 다른 목적으로 저장하면 {@code jsonConfig} 가 통째로 덮여 사라졌다. 세 계좌에서 그렇게 지워졌고 요약 화면 원금이 10,627,923 원 어긋났다
 * ({@link AccountJsonConfigCarryOverTest} 에서 고쳤다).
 *
 * <p>같은 함정이 다른 곳에도 있는지 2026-08-23 에 훑었고, 아래가 그때 확인한 사실이다. 각각이 무너지면 같은 사고가 난다.
 *
 * <ul>
 *   <li>종목 태그는 {@code @Transient} 이고 별도 테이블이라 종목 저장으로 지워지지 않는다. 시트 가져오기가 전량 재생성하지만 클래스에
 *       {@code @Transactional} 이 있어 중간 실패는 롤백된다.
 *   <li>월배당 프로필 순서 변경은 {@code displayOrder} 만 바꾼다. 예전 값을 읽어 그대로 다시 저장하므로 {@code sourceUrl} · {@code
 *       note} 가 남는다.
 *   <li>월배당 프로필 저장({@code upsert})은 모든 필드를 덮어쓰지만, 게이트가 기존 값을 채워 보낸다.
 * </ul>
 */
class PartialUpdateDataLossGuardTest {

  private static final Path MAIN = Path.of("src/main/java/net/luversof/api/stock");

  private String read(String relative) throws IOException {
    Path path = MAIN.resolve(relative);
    assertThat(path).as("파일이 옮겨졌다: " + path).exists();
    return Files.readString(path, StandardCharsets.UTF_8);
  }

  @Test
  void 계좌_갱신은_보내지_않은_설정을_이어받는다() throws IOException {
    assertThat(read("service/AccountService.java"))
        .as("이 호출이 사라지면 계좌를 저장할 때마다 직접 입력한 값이 지워진다")
        .contains("carryOverJsonConfig(stored, account)");
  }

  @Test
  void 종목_태그는_별도_저장이라_종목_저장으로_지워지지_않는다() throws IOException {
    assertThat(read("domain/StockItem.java"))
        .as("태그가 종목 컬럼이 되면 종목 저장이 태그를 덮어쓴다")
        .contains("@Transient private List<String> tags");
  }

  @Test
  void 태그_전량_재생성은_트랜잭션_안에서_돈다() throws IOException {
    String source = read("service/StockAdminService.java");
    assertThat(source).as("deleteAll 뒤 재삽입 사이에 실패하면 태그가 전부 사라진다").contains("@Transactional");
    assertThat(source.indexOf("@Transactional"))
        .as("클래스 선언보다 앞에 있어야 클래스 전체에 걸린다")
        .isLessThan(source.indexOf("public class StockAdminService"));
  }

  @Test
  void 월배당_프로필_순서변경은_다른_값을_건드리지_않는다() throws IOException {
    String source = read("service/MonthlyDividendProfileService.java");
    int start = source.indexOf("public void reorder(");
    assertThat(start).as("reorder 를 찾지 못했다").isGreaterThan(-1);
    String body = source.substring(start, source.indexOf("public void deleteBySymbol("));

    List<String> overwritten = new ArrayList<>();
    for (String setter :
        List.of("setSourceUrl", "setNote", "setPayoutWindow", "setActive", "setLastVerifiedDate")) {
      if (body.contains(setter)) {
        overwritten.add(setter);
      }
    }
    assertThat(overwritten).as("순서만 바꾸는 호출이 다른 값을 덮어쓰면 사람이 넣은 값이 사라진다").isEmpty();
  }

  /** 파일이 사라지거나 옮겨지면 위 검사들이 조용히 무의미해진다. */
  @Test
  void 검사_대상_파일이_모두_있다() throws IOException {
    try (Stream<Path> files = Files.walk(MAIN)) {
      long count = files.filter(Files::isRegularFile).count();
      assertThat(count).as("소스를 찾지 못했다").isGreaterThan(50);
    }
  }
}
