package net.luversof.web.gate.stock.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * 화면 금액 표기를 고정한다.
 *
 * <p><b>로케일은 반드시 인자로 넘긴다.</b> 인자 없는 {@code compactKrw(long)} 오버로드는 {@code
 * LocaleContextHolder.getLocale()} 을 읽고, 그 값은 컨텍스트가 없으면 <b>실행 환경의 기본 로케일</b>이다. 예전 테스트가 그 오버로드를 써서,
 * 한국어 머신에서만 통과하고 영어 로케일에서는 두 테스트가 모두 깨졌다(실측: {@code -231만} 이 나와야 할 자리에 {@code -2.3M}). 테스트가 돌아가는
 * 기계에 따라 결과가 달라지면 회귀를 잡는 도구로 쓸 수 없다.
 */
class StockFormatUtilTest {

  private static final Locale KO = Locale.KOREAN;
  private static final Locale EN = Locale.ENGLISH;

  @Test
  void 한국어는_억만_단위로_압축한다() {
    assertThat(StockFormatUtil.compactKrw(0, KO)).isEqualTo("0");
    assertThat(StockFormatUtil.compactKrw(5_300, KO)).isEqualTo("5,300");
    assertThat(StockFormatUtil.compactKrw(10_000, KO)).isEqualTo("1만");
    assertThat(StockFormatUtil.compactKrw(23_100_000, KO)).isEqualTo("2,310만");
    assertThat(StockFormatUtil.compactKrw(123_456_789, KO)).isEqualTo("1억 2,345만");
    assertThat(StockFormatUtil.compactKrw(100_000_000, KO)).isEqualTo("1억"); // 만 자리 0이면 생략
    assertThat(StockFormatUtil.compactKrw(1_200_000_000, KO)).isEqualTo("12억");
  }

  @Test
  void 한국어_음수도_같은_규칙을_따른다() {
    assertThat(StockFormatUtil.compactKrw(-2_310_000, KO)).isEqualTo("-231만");
    assertThat(StockFormatUtil.compactKrw(-123_456_789, KO)).isEqualTo("-1억 2,345만");
  }

  /**
   * 영어 화면은 B/M/K 로 쓴다.
   *
   * <p>이 갈래는 "영어 화면에도 억/만 이 그대로 나왔다(실측 15곳)" 는 문제를 고치려고 넣은 것인데, 정작 테스트가 하나도 없었다.
   */
  @Test
  void 영어는_BMK_로_압축한다() {
    assertThat(StockFormatUtil.compactKrw(0, EN)).isEqualTo("0");
    assertThat(StockFormatUtil.compactKrw(999, EN)).isEqualTo("999");
    assertThat(StockFormatUtil.compactKrw(5_300, EN)).isEqualTo("5.3K");
    assertThat(StockFormatUtil.compactKrw(23_100_000, EN)).isEqualTo("23.1M");
    assertThat(StockFormatUtil.compactKrw(1_200_000_000, EN)).isEqualTo("1.2B");
    assertThat(StockFormatUtil.compactKrw(-2_310_000, EN)).isEqualTo("-2.3M");
  }

  /** 끝자리가 0 이면 소수점을 떼어 1.0B 대신 1B 로 쓴다. */
  @Test
  void 영어_표기는_끝의_0을_떼어낸다() {
    assertThat(StockFormatUtil.compactKrw(1_000_000_000, EN)).isEqualTo("1B");
    assertThat(StockFormatUtil.compactKrw(2_000_000, EN)).isEqualTo("2M");
    assertThat(StockFormatUtil.compactKrw(1_000, EN)).isEqualTo("1K");
  }

  /** 툴팁의 정확 금액. 한국어는 뒤에 "원", 그 외에는 앞에 "KRW". */
  @Test
  void 정확_금액은_로케일에_따라_통화를_붙인다() {
    assertThat(StockFormatUtil.fullKrw(1_248_142_500L, KO)).isEqualTo("1,248,142,500원");
    assertThat(StockFormatUtil.fullKrw(1_248_142_500L, EN)).isEqualTo("KRW 1,248,142,500");
    assertThat(StockFormatUtil.fullKrw(0, KO)).isEqualTo("0원");
    assertThat(StockFormatUtil.fullKrw(-5_300, KO)).isEqualTo("-5,300원");
    assertThat(StockFormatUtil.fullKrw(-5_300, EN)).isEqualTo("KRW -5,300");
  }

  /**
   * 테스트가 로케일 인자 없는 오버로드를 쓰지 않는지 스스로 확인한다.
   *
   * <p>{@code compactKrw(long)} / {@code fullKrw(long)} 은 실행 환경 기본 로케일을 따르므로, 테스트에서 쓰면 기계에 따라 결과가
   * 달라진다. 실제로 그래서 이 파일의 예전 테스트 2 개가 영어 로케일에서 전부 깨졌다.
   */
  @Test
  void 테스트는_로케일_인자를_반드시_넘긴다() throws java.io.IOException {
    java.nio.file.Path self =
        java.nio.file.Path.of(
            "src/test/java/net/luversof/web/gate/stock/util/StockFormatUtilTest.java");
    assertThat(self).as("파일이 옮겨졌다: " + self).exists();
    java.util.regex.Pattern singleArg =
        java.util.regex.Pattern.compile(
            "StockFormatUtil\\.(?:compactKrw|fullKrw)\\(\\s*[^,()]+\\s*\\)");
    java.util.regex.Matcher matcher =
        singleArg.matcher(
            java.nio.file.Files.readString(self, java.nio.charset.StandardCharsets.UTF_8));
    java.util.List<String> found = new java.util.ArrayList<>();
    while (matcher.find()) {
      found.add(matcher.group());
    }
    assertThat(found).as("로케일 인자 없는 호출은 실행 환경 기본 로케일을 따라 기계마다 결과가 달라진다").isEmpty();
  }

  /** 로케일이 없으면 한국어로 본다(기존 동작). */
  @Test
  void 로케일이_null이면_한국어로_본다() {
    assertThat(StockFormatUtil.compactKrw(23_100_000, null)).isEqualTo("2,310만");
    assertThat(StockFormatUtil.fullKrw(5_300, null)).isEqualTo("5,300원");
  }

  /**
   * 서버의 기본 로케일이 무엇이든 같은 결과가 나오는지.
   *
   * <p>이 클래스는 로케일을 인자로 받는데, 예전에는 단위 문자열("억"/"만"/"KRW")만 그 로케일을 보고 숫자 자체는 {@code
   * String.format("%,d", ...)} 로 <b>JVM 기본 로케일</b>을 썼다. 그래서 인자로 en 을 줘도 서버 설정에 따라 출력이 달라졌다 (실측: 기본
   * 로케일을 fr-FR 로 두면 이 클래스 테스트 7 개 중 6 개가 깨졌다 &mdash; "1,248,142,500원" -> "1 248 142 500원", "5.3K"
   * -> "5,3K"). 특히 {@code trimZero} 는 포맷한 문자열이 ".0" 으로 끝나는지 봤기 때문에 소수점이 쉼표인 로케일에서는 "1,0B" 가 그대로
   * 나갔다.
   *
   * <p>배포 JVM 이 ko-KR 이라 지금 눈에 보이는 증상은 없다. 그래도 로케일 인자를 받아놓고 무시하면 서버 설정 하나로 화면 숫자가 바뀌므로 고정한다.
   */
  @Test
  void 서버_기본_로케일이_달라도_결과가_같다() {
    java.util.Locale original = java.util.Locale.getDefault();
    try {
      for (java.util.Locale ambient :
          java.util.List.of(
              java.util.Locale.KOREA,
              java.util.Locale.US,
              java.util.Locale.FRANCE,
              java.util.Locale.GERMANY)) {
        java.util.Locale.setDefault(ambient);
        assertThat(StockFormatUtil.compactKrw(1_493_281_835L, java.util.Locale.KOREA))
            .as("기본 로케일 " + ambient)
            .isEqualTo("14억 9,328만");
        assertThat(StockFormatUtil.compactKrw(5_300L, java.util.Locale.KOREA))
            .as("기본 로케일 " + ambient)
            .isEqualTo("5,300");
        assertThat(StockFormatUtil.compactKrw(5_300L, java.util.Locale.US))
            .as("기본 로케일 " + ambient)
            .isEqualTo("5.3K");
        assertThat(StockFormatUtil.compactKrw(1_000_000_000L, java.util.Locale.US))
            .as("끝의 0 을 떼는 판단이 소수점 기호에 걸리면 안 된다: " + ambient)
            .isEqualTo("1B");
        // 넘겨받은 로케일의 소수점이 쉼표면 ".0" 문자열 검사가 절대 맞지 않아 "1,0B" 가 그대로 나간다.
        // US 만 보면 이 결함을 놓친다(실제로 놓쳐서 주입이 통과했다).
        assertThat(StockFormatUtil.compactKrw(1_000_000_000L, java.util.Locale.FRANCE))
            .as("소수점이 쉼표인 로케일에서 끝의 0 이 남았다: " + ambient)
            .isEqualTo("1B");
        assertThat(StockFormatUtil.compactKrw(5_300L, java.util.Locale.FRANCE))
            .as("소수 표기는 넘겨받은 로케일을 따른다: " + ambient)
            .isEqualTo("5,3K");
        assertThat(StockFormatUtil.fullKrw(1_248_142_500L, java.util.Locale.KOREA))
            .as("기본 로케일 " + ambient)
            .isEqualTo("1,248,142,500원");
        assertThat(StockFormatUtil.fullKrw(1_248_142_500L, java.util.Locale.US))
            .as("기본 로케일 " + ambient)
            .isEqualTo("KRW 1,248,142,500");
      }
    } finally {
      java.util.Locale.setDefault(original);
    }
  }

  /**
   * 통화 단위를 화면 코드가 직접 붙이지 않는지.
   *
   * <p>{@code fullKrw} 는 로케일에 맞춰 {@code 1,234원} / {@code KRW 1,234} 를 낸다. 예전에는 템플릿 28 곳이 {@code
   * String.format("%,d원", ...)} 로 "원" 을 직접 붙여, 영어 화면 툴팁만 한국어 단위로 나왔다. 그 수정이 되돌아가지 않도록 고정한다 (실측
   * 2026-08-23: 표시 코드의 직접 표기 0 건).
   *
   * <p>{@code ₩} 는 별개다 &mdash; 템플릿 99 곳이 금액 앞에 붙이고 있고, 그래서 같은 화면에서 인라인 금액은 "₩1,234", 툴팁은 "1,234원"
   * (영어면 "KRW 1,234") 으로 표기가 갈린다. 어느 표기로 통일할지는 화면 결정이라 여기서 강제하지 않고, 늘어나지 않는지만 센다.
   */
  @Test
  void 표시_코드가_통화_단위를_직접_붙이지_않는다() throws java.io.IOException {
    java.util.List<String> offenders = new java.util.ArrayList<>();
    int scanned = 0;
    int wonSignCount = 0;
    for (java.nio.file.Path root :
        java.util.List.of(
            java.nio.file.Path.of("src/main/jte/stock"),
            java.nio.file.Path.of("src/main/java/net/luversof/web/gate/stock"))) {
      try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.walk(root)) {
        for (java.nio.file.Path file :
            files
                .filter(java.nio.file.Files::isRegularFile)
                .filter(f -> f.toString().endsWith(".jte") || f.toString().endsWith(".java"))
                .toList()) {
          String name = file.getFileName().toString();
          // 규칙을 정하는 클래스 자신과, 가져온 텍스트에서 단위를 떼어내는 파서는 대상이 아니다.
          if (name.equals("StockFormatUtil.java")
              || name.equals("MonthlyDividendPayoutImportParser.java")) {
            continue;
          }
          scanned++;
          String source =
              java.nio.file.Files.readString(file, java.nio.charset.StandardCharsets.UTF_8);
          wonSignCount += source.length() - source.replace("\u20A9", "").length();
          if (source.contains("d원") || source.contains("KRW ")) {
            offenders.add(name);
          }
        }
      }
    }

    assertThat(scanned).as("소스를 하나도 읽지 못했다").isGreaterThan(50);
    assertThat(offenders)
        .as("통화 단위는 StockFormatUtil 이 로케일에 맞춰 붙인다 - 직접 붙이면 영어 화면에 한국어 단위가 나간다")
        .isEmpty();
    // 실측 2026-08-23: 99 곳. 줄이는 것은 화면 결정이므로 늘어나는 것만 막는다.
    assertThat(wonSignCount)
        .as("원화 기호를 직접 붙이는 곳이 늘었다 - 툴팁(fullKrw)과 표기가 더 갈린다")
        .isLessThanOrEqualTo(99);
  }

  /**
   * 반올림이 1,000 에 닿으면 단위를 올린다.
   *
   * <p>실측 2026-08-23: 예전에는 단위를 <b>원래 값</b>으로 고른 뒤 반올림해서 999,999 가 "1,000K", 999,950,000 과
   * 999,999,999 가 "1,000M" 으로 나왔다. 압축 표기는 자릿수를 줄이려고 쓰는 것인데 "1,000K" 는 그 목적을 정확히 어기고, 같은 화면의 "1M" ·
   * "1B" 와도 어긋난다.
   */
  @Test
  void 국제표기는_반올림이_자릿수를_넘기면_단위를_올린다() {
    assertThat(StockFormatUtil.compactKrw(999_999L, java.util.Locale.US)).isEqualTo("1M");
    assertThat(StockFormatUtil.compactKrw(999_950_000L, java.util.Locale.US)).isEqualTo("1B");
    assertThat(StockFormatUtil.compactKrw(999_999_999L, java.util.Locale.US)).isEqualTo("1B");
    assertThat(StockFormatUtil.compactKrw(-999_999_999L, java.util.Locale.US)).isEqualTo("-1B");
  }

  /** 넘기지 않는 값은 그대로여야 한다. 승급 규칙이 과하게 적용되면 안 된다. */
  @Test
  void 반올림이_넘기지_않으면_단위가_그대로다() {
    assertThat(StockFormatUtil.compactKrw(999_499L, java.util.Locale.US)).isEqualTo("999.5K");
    assertThat(StockFormatUtil.compactKrw(999_949_999L, java.util.Locale.US)).isEqualTo("999.9M");
    assertThat(StockFormatUtil.compactKrw(1_000L, java.util.Locale.US)).isEqualTo("1K");
    assertThat(StockFormatUtil.compactKrw(999L, java.util.Locale.US)).isEqualTo("999");
  }

  /** 한국어 표기는 정수 절삭이라 이 승급과 무관하다. 함께 고정한다. */
  @Test
  void 한국어_표기는_경계에서_바뀌지_않는다() {
    assertThat(StockFormatUtil.compactKrw(999_999L, java.util.Locale.KOREA)).isEqualTo("99만");
    assertThat(StockFormatUtil.compactKrw(999_999_999L, java.util.Locale.KOREA))
        .isEqualTo("9억 9,999만");
    assertThat(StockFormatUtil.compactKrw(100_000_000L, java.util.Locale.KOREA)).isEqualTo("1억");
  }

  /**
   * 기준표({@code compact-number-en.txt})가 서버 구현과 여전히 같은지.
   *
   * <p>그 표는 화면 쪽 규칙(stock-charts.ts 의 compactNumber)과 node 테스트가 대조하는 값이다. 서버가 바뀌었는데 표가 낡으면 화면 검사만
   * 통과하고 실제로는 갈린 상태가 된다. 양쪽에서 같은 표를 붙잡는다.
   */
  @Test
  void 영어_압축표기_기준표가_구현과_같다() throws java.io.IOException {
    java.nio.file.Path table = java.nio.file.Path.of("src/test/resources/compact-number-en.txt");
    assertThat(table).as("기준표가 없다").exists();

    java.util.List<String> mismatches = new java.util.ArrayList<>();
    int checked = 0;
    for (String line :
        java.nio.file.Files.readAllLines(table, java.nio.charset.StandardCharsets.UTF_8)) {
      String trimmed = line.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      String[] parts = trimmed.split("[|]");
      checked++;
      String actual = StockFormatUtil.compactKrw(Long.parseLong(parts[0]), java.util.Locale.US);
      if (!actual.equals(parts[1])) {
        mismatches.add(parts[0] + ": 표 " + parts[1] + " / 구현 " + actual);
      }
    }
    assertThat(checked).as("기준표가 비었다").isGreaterThanOrEqualTo(16);
    assertThat(mismatches).as("기준표가 낡았다. 다시 뽑아 넣을 것").isEmpty();
  }
}
