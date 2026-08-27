package net.luversof.web.gate.stock.util;

/** 화면 표시용 숫자 포맷 헬퍼. 요약 카드의 큰 금액을 억/만 단위로 압축해 가독성을 높인다(정확값은 툴팁으로 노출). */
public final class StockFormatUtil {

  private StockFormatUtil() {}

  /**
   * 원 단위 금액을 한국식 억/만 압축 표기로 변환한다. 만 미만 잔여는 버린다(요약용). 예) 123,456,789 → "1억 2,345만", 23,100,000 →
   * "2,310만", 5,300 → "5,300", -2,310,000 → "-231만".
   */
  public static String compactKrw(long value) {
    return compactKrw(value, org.springframework.context.i18n.LocaleContextHolder.getLocale());
  }

  /**
   * 로케일에 맞춘 압축 표기. 한국어는 억/만, 그 외에는 국제 표기(B/M/K)를 쓴다.
   *
   * <p>예전에는 로케일과 무관하게 억/만 을 붙여, 영어 화면에도 "n억 n,nnn만" 이 그대로 나왔다(실측: 영어 화면 15곳).
   *
   * <p>자릿수 구분과 소수점도 넘겨받은 로케일로 찍는다. 예전에는 단위 문자열만 로케일을 보고 숫자 포맷은 JVM 기본 로케일을 써서, 인자로 로케일을 줘도 출력이 서버
   * 설정에 좌우됐다(실측: 기본 로케일을 fr-FR 로 두면 이 클래스 테스트 7 개 중 6 개가 깨진다 - 자릿수 구분이 쉼표에서 공백으로, 소수점이 마침표에서 쉼표로
   * 바뀐다). 배포 JVM 이 ko-KR 이라 지금 눈에 보이는 증상은 없지만, 로케일 인자를 받아놓고 무시하는 것은 계약 위반이라 고쳤다.
   */
  public static String compactKrw(long value, java.util.Locale locale) {
    if (value == 0) {
      return "0";
    }

    java.util.Locale target = locale != null ? locale : java.util.Locale.KOREA;
    if (!java.util.Locale.KOREAN.getLanguage().equals(target.getLanguage())) {
      return compactWestern(value, target);
    }

    String sign = value < 0 ? "-" : "";
    long abs = Math.abs(value);
    long eok = abs / 100_000_000L;
    long man = (abs % 100_000_000L) / 10_000L;

    if (eok > 0) {
      StringBuilder sb =
          new StringBuilder(sign).append(String.format(target, "%,d", eok)).append("억");
      if (man > 0) {
        sb.append(" ").append(String.format(target, "%,d", man)).append("만");
      }
      return sb.toString();
    }
    if (man > 0) {
      return sign + String.format(target, "%,d", man) + "만";
    }
    // 1만 미만은 원 단위 그대로 표기
    return sign + String.format(target, "%,d", abs);
  }

  /**
   * 툴팁에 쓰는 정확한 금액 표기. 로케일에 맞춰 통화 단위를 붙인다.
   *
   * <p>템플릿 28 곳이 {@code String.format("%,d원", ...)} 로 "원" 을 직접 붙이고 있어, 영어 화면에서도 툴팁만 한국식 "원" 표기로
   * 나왔다(실측: 종목상세 7 개·계좌상세 5 개 등). 압축 표기(compactKrw)는 이미 로케일을 보므로 같은 규칙으로 맞춘다.
   */
  public static String fullKrw(long value) {
    return fullKrw(value, org.springframework.context.i18n.LocaleContextHolder.getLocale());
  }

  /** 한국어는 {@code 1,234원}, 그 외에는 {@code KRW 1,234}. */
  public static String fullKrw(long value, java.util.Locale locale) {
    java.util.Locale target = locale != null ? locale : java.util.Locale.KOREA;
    String amount = String.format(target, "%,d", value);
    if (!java.util.Locale.KOREAN.getLanguage().equals(target.getLanguage())) {
      return "KRW " + amount;
    }
    return amount + "원";
  }

  /** 국제 표기. 10억 이상은 B, 100만 이상은 M, 1천 이상은 K, 그 미만은 그대로. */
  /**
   * 국제 표기(K/M/B).
   *
   * <p>단위는 <b>반올림한 뒤</b>의 크기로 고른다. 예전에는 원래 값으로 단위를 고른 다음 반올림해서, 반올림이 1,000 에 닿아도 단위가 그대로였다 &mdash;
   * 실측: 999,999 → "1,000K"(1M 이어야 한다), 999,950,000 과 999,999,999 → "1,000M"(1B). 압축 표기는 자릿수를 줄이려고
   * 쓰는 것인데 "1,000K" 는 그 목적을 정확히 어긴다. 같은 화면에 "1M" · "1B" 가 함께 나오므로 눈에도 어긋난다.
   */
  private static String compactWestern(long value, java.util.Locale locale) {
    String sign = value < 0 ? "-" : "";
    long abs = Math.abs(value);
    if (abs < 1_000L) {
      return sign + String.format(locale, "%,d", abs);
    }

    String[] suffixes = {"K", "M", "B"};
    double[] scales = {1_000.0, 1_000_000.0, 1_000_000_000.0};
    int unit = abs >= 1_000_000_000L ? 2 : abs >= 1_000_000L ? 1 : 0;
    double rounded = roundToTenth(abs / scales[unit]);
    // 반올림이 1,000 에 닿으면 다음 단위로 올린다(B 위는 없으므로 그대로 둔다).
    if (rounded >= 1_000.0 && unit < suffixes.length - 1) {
      unit++;
      rounded = roundToTenth(abs / scales[unit]);
    }
    return sign + trimZero(rounded, locale) + suffixes[unit];
  }

  private static double roundToTenth(double value) {
    return Math.round(value * 10.0) / 10.0;
  }

  /**
   * 소수 첫째 자리까지, 끝자리가 0 이면 정수로 표기(1.0B -> 1B).
   *
   * <p>예전에는 포맷한 <b>문자열</b>이 ".0" 으로 끝나는지 봤다. 소수점 기호가 쉼표인 로케일(fr 등)에서는 "1,0" 이라 그 검사가 절대 맞지 않아
   * "1,0B" 가 그대로 나갔다. 문자열이 아니라 수로 판단한다.
   */
  private static String trimZero(double v, java.util.Locale locale) {
    double rounded = roundToTenth(v);
    if (rounded == Math.rint(rounded)) {
      return String.format(locale, "%,d", (long) rounded);
    }
    return String.format(locale, "%.1f", rounded);
  }

  /**
   * 화면에 원 단위로 찍는 값. 표의 각 행과 소계가 같은 규칙을 써야 열을 더한 값이 소계와 맞는다.
   *
   * <p>예전에는 행이 각각 {@code longValue()}(버림)로 그려지는데 소계만 BigDecimal 합계를 한 번 버렸다. 그래서 보이는 숫자를 더하면 소계와
   * 달랐다 &mdash; 실측 2026-08-23 월배당 8 종목에서 행 합과 소계가 <b>2 원</b> 차이 났다. 버림이라 행마다 최대 1 원씩 모자라고 종목 수만큼
   * 벌어진다. 지금은 행·소계 모두 반올림이라 정확히 맞는다.
   */
  public static long displayWon(java.math.BigDecimal amount) {
    return amount == null ? 0L : amount.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
  }
}
