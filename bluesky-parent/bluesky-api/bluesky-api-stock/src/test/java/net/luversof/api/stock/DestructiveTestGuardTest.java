package net.luversof.api.stock;

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
 * 실사용자 데이터를 바꾸는 테스트가 자동 실행에 남아 있지 않은지 본다.
 *
 * <p>이 저장소의 일부 테스트는 검증이 아니라 <b>개발용 도구</b>다. 실제 DB 에 붙어 원장을 지우거나 다시 넣는다. 프로필 없이 돌리면 설정 오류로 죽어서 지금까지
 * 드러나지 않았는데, 프로필을 주는 순간 그대로 실행된다.
 *
 * <p>실측 사고(2026-08-22): {@code -Dspring.profiles.active=localdev} 로 {@code AccountTest} 를 돌리자
 * {@code deleteAllByUserId} 가 계좌 7 -> 0, 거래 250 -> 0, 배당 193 -> 0 으로 지웠다. 원장은 시트 재가져오기로 되돌렸지만 (거래
 * 250 / 배당 193 복원 확인) 계좌 설정 {@code manualPrincipalAmount} 는 갱신 API 가 없어 복구하지 못했다.
 *
 * <p>그래서 "실사용자 id 로 쓰기를 하는 테스트" 는 반드시 {@code @Disabled} 여야 한다.
 */
class DestructiveTestGuardTest {

  private static final Path TEST_ROOT = Path.of("src/test/java/net/luversof/api/stock");

  /** 실제 저장소·서비스에 쓰기를 거는 호출. */
  private static final Pattern WRITE_CALL =
      Pattern.compile(
          "\\.(delete\\w*|save\\w*|createAccount|createStockItem|\\w*[bB]ulkInsert\\w*"
              + "|importMonthly\\w*|updatePriceHistory)\\s*\\(");

  /** 실사용자를 가리키는 표식. */
  private static final Pattern REAL_USER = Pattern.compile("TestConstant\\.USER_ID");

  private List<String> unguarded() throws IOException {
    List<String> found = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEST_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (!REAL_USER.matcher(source).find()) {
          continue;
        }
        // 목(mock)만 쓰는 단위 테스트는 대상이 아니다.
        if (source.contains("@ExtendWith(MockitoExtension.class)")) {
          continue;
        }
        boolean classDisabled =
            source.contains("@Disabled")
                && source.indexOf("@Disabled") < source.indexOf("class " + fileName(file));
        Matcher matcher = WRITE_CALL.matcher(source);
        while (matcher.find()) {
          if (classDisabled) {
            break;
          }
          // 메서드 앞의 애노테이션 블록을 통째로 본다. @Disabled 는 @Test 앞에 올 수도 뒤에 올
          // 수도 있으므로 @Test 부터만 되짚으면 앞에 붙은 것을 놓친다.
          int methodStart = source.lastIndexOf("  @Test", matcher.start());
          if (methodStart < 0) {
            continue;
          }
          int blockStart = source.lastIndexOf("\\n\\n", methodStart);
          String header = source.substring(Math.max(blockStart, 0), matcher.start());
          if (!header.contains("@Disabled")) {
            found.add(fileName(file) + " -> " + matcher.group(1));
          }
        }
      }
    }
    return found;
  }

  private static String fileName(Path file) {
    return file.getFileName().toString().replace(".java", "");
  }

  @Test
  void 실사용자_데이터를_바꾸는_테스트는_비활성화되어_있다() throws IOException {
    assertThat(unguarded())
        .as(
            "실사용자 id 로 쓰기를 하는 테스트는 @Disabled 여야 한다."
                + " 자동 실행에서 돌면 원장이 지워진다(실측: 계좌 7->0, 거래 250->0, 배당 193->0)")
        .isEmpty();
  }

  /** 가드가 실제로 무언가를 보고 있는지. 대상이 0 개면 검사가 무력하다. */
  @Test
  void 검사_대상이_실제로_존재한다() throws IOException {
    List<String> realUserTests = new ArrayList<>();
    try (Stream<Path> files = Files.walk(TEST_ROOT)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String source = Files.readString(file, StandardCharsets.UTF_8);
        if (REAL_USER.matcher(source).find()) {
          realUserTests.add(fileName(file));
        }
      }
    }
    assertThat(realUserTests)
        .as("TestConstant.USER_ID 를 쓰는 테스트가 하나도 없다면 이 가드는 아무것도 보지 않는다")
        .isNotEmpty();
  }
}
