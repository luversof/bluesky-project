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

  /**
   * 소수점이 있는 금액(주당 분배금 · 주당 과세표준액처럼 원 단위보다 작은 값)의 정확한 표기.
   *
   * <p>월배당 ETF 목록(2026-09-21)이 쓴다 &mdash; 주당 32.25 원을 원 단위로 반올림하면 비교가 무너진다. 통화 단위는 여기서 로케일에 맞춰 붙인다.
   *
   * @param scale 소수 자릿수(0 이면 정수 표기)
   */
  public static String fullKrw(java.math.BigDecimal value, int scale) {
    return fullKrw(value, scale, org.springframework.context.i18n.LocaleContextHolder.getLocale());
  }

  /** 한국어는 {@code 32.25원}, 그 외에는 {@code KRW 32.25}. 값이 없으면 0 으로 본다. */
  public static String fullKrw(java.math.BigDecimal value, int scale, java.util.Locale locale) {
    java.math.BigDecimal safe =
        (value != null ? value : java.math.BigDecimal.ZERO)
            .setScale(Math.max(scale, 0), java.math.RoundingMode.HALF_UP);
    java.util.Locale target = locale != null ? locale : java.util.Locale.KOREA;
    String amount = String.format(target, "%,." + Math.max(scale, 0) + "f", safe);
    if (!java.util.Locale.KOREAN.getLanguage().equals(target.getLanguage())) {
      return "KRW " + amount;
    }
    return amount + "원";
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

  /**
   * 비율(%) 표기. 반올림한 뒤 음의 영을 0 으로 고쳐 "-0.0%" 가 나가지 않게 한다.
   *
   * <p>실측 2026-09-10: {@code String.format("%+.1f%%", -0.04)} 는 "-0.0%" 다. 2년 평가액 시계열 3,978 구간 중
   * 16개(하루 구간 위주)가 그 범위에 들어 자산 성장 기간 수익률·일간 변동에 실제로 찍힐 수 있다. 반올림은 Formatter 와 같은 HALF_UP.
   */
  /**
   * 한 표 안의 비중을 함께 반올림해, 표시값의 합이 전체(보통 100%)와 맞게 한다.
   *
   * <p>행마다 따로 반올림하면 표시값의 합이 어긋난다 - 실측 2026-09-11: 자산 현황 '종목별 현황' 9행이 99.9%, '계좌 보유 종목 상세' 두 표가
   * 100.1% 였다(합계행은 100.0%). 각 행의 오차는 0.05%p 미만이라 값이 틀린 것은 아니고, 더한 결과만 어긋난다.
   *
   * <p>최대잔여법: 먼저 모두 내림한 뒤, 남은 몫을 버림 잔차가 큰 행부터 한 칸씩 나눠 준다. 행 하나가 원값에서 벗어나는 폭은 표시 자릿수 한 칸(0.1%p)을 넘지
   * 않는다.
   *
   * <p>입력 합이 100 과 크게 다르면(필터로 일부만 보는 표 등) 손대지 않고 행마다 반올림한 값을 그대로 돌려준다.
   */
  public static java.util.List<String> balancedPct(
      java.util.List<java.math.BigDecimal> values, int scale) {
    java.util.List<String> fallback = new java.util.ArrayList<>();
    if (values == null || values.isEmpty()) {
      return fallback;
    }
    java.math.BigDecimal step = java.math.BigDecimal.ONE.movePointLeft(scale);
    java.math.BigDecimal sum = java.math.BigDecimal.ZERO;
    for (java.math.BigDecimal value : values) {
      sum = sum.add(value == null ? java.math.BigDecimal.ZERO : value);
    }
    for (java.math.BigDecimal value : values) {
      fallback.add(pct(value == null ? 0d : value.doubleValue(), scale));
    }
    java.math.BigDecimal hundred = java.math.BigDecimal.valueOf(100);
    if (sum.subtract(hundred).abs().compareTo(step) > 0) {
      return fallback;
    }

    int size = values.size();
    java.math.BigDecimal[] floors = new java.math.BigDecimal[size];
    java.math.BigDecimal[] remainders = new java.math.BigDecimal[size];
    java.math.BigDecimal floorSum = java.math.BigDecimal.ZERO;
    for (int i = 0; i < size; i++) {
      java.math.BigDecimal value =
          values.get(i) == null ? java.math.BigDecimal.ZERO : values.get(i);
      java.math.BigDecimal floor = value.setScale(scale, java.math.RoundingMode.FLOOR);
      floors[i] = floor;
      remainders[i] = value.subtract(floor);
      floorSum = floorSum.add(floor);
    }
    int spare =
        hundred.subtract(floorSum).divide(step, 0, java.math.RoundingMode.HALF_UP).intValue();
    if (spare < 0 || spare > size) {
      return fallback;
    }
    Integer[] order = new Integer[size];
    for (int i = 0; i < size; i++) {
      order[i] = i;
    }
    java.util.Arrays.sort(order, (a, b) -> remainders[b].compareTo(remainders[a]));
    boolean[] bump = new boolean[size];
    for (int i = 0; i < spare; i++) {
      bump[order[i]] = true;
    }
    java.util.List<String> out = new java.util.ArrayList<>();
    for (int i = 0; i < size; i++) {
      java.math.BigDecimal shown = bump[i] ? floors[i].add(step) : floors[i];
      out.add(pct(shown.doubleValue(), scale));
    }
    return out;
  }

  /**
   * 부호 붙은 원 단위 금액. <b>0 에는 부호를 붙이지 않는다.</b>
   *
   * <p>{@code String.format("%+,d", 0)} 은 {@code +0} 이다. 부호는 방향을 말하는 표시인데 0 에는 방향이 없다 &mdash; "+0"
   * 은 "0 원 벌었다" 처럼 읽힌다. 실측 2026-09-11(10 화면): 부호 붙은 0 이 8 곳이었고, 그중 7 곳이 거래 이력 없는 종목 상세의 카드(합산 손익 ·
   * 평가 변동 · 실현 손익 · 기간 배당)였다 &mdash; 그 종목은 아무 일도 없었던 것이지 0 원을 번 것이 아니다.
   *
   * <p>같은 규칙을 {@code amountCell} 이 먼저 정했다: 0 과 "아무 일도 없었다" 는 다르게 읽힌다.
   */
  /**
   * 축약 표기에 부호를 붙인다. 영에는 붙이지 않는다({@link #signedWon} 과 같은 규칙).
   *
   * <p>{@code compactKrw} 는 음수에만 "-" 를 달고 양수는 맨 숫자로 낸다. 그래서 손익 카드는 방향을 <b>글자색</b>으로만 말하고 있었다
   * &mdash; 실측 2026-09-12(고대비 모드): {@code .text-profit}/{@code .text-loss} 의 색이 모두 검정 한 가지로 합쳐져, 그
   * 상태에서 "9억 9,117만" 은 벌었는지 잃었는지 알 수 없다.
   *
   * <p>손익이 아닌 값(총 자산·배당·매수 금액)에는 쓰지 않는다 &mdash; 나간 돈에 "+" 를 붙이면 번 돈처럼 읽힌다.
   */
  public static String signedCompactKrw(long value) {
    if (value <= 0) {
      return compactKrw(value);
    }
    return "+" + compactKrw(value);
  }

  public static String signedWon(long value) {
    if (value == 0) {
      return "0";
    }
    return String.format("%+,d", value);
  }

  public static String pct(double value, int scale) {
    return String.format("%." + scale + "f%%", roundForDisplay(value, scale));
  }

  /**
   * {@link #pct} 의 부호 표기판. 영에는 부호를 붙이지 않는다.
   *
   * <p>영 판정은 반올림 뒤에 한다 - -0.04 는 "-0.0%" 도 "+0.0%" 도 아니고 "0.0%" 다.
   *
   * <p>2026-09-11 까지 영도 "+" 를 달았다(원래 목적은 "-0.0%" 를 막는 것이었다). 그런데 부호는 방향을 말하는 표시이고 0 에는 방향이 없다 - 거래
   * 이력이 없는 종목 상세가 "평가 손익 0 +0.0%" 처럼 금액은 부호 없이, 비율만 "+" 를 달고 나갔다. {@link #signedWon} 과 같은 규칙으로 맞춘다.
   */
  public static String signedPct(double value, int scale) {
    double rounded = roundForDisplay(value, scale);
    if (rounded == 0) {
      return String.format("%." + scale + "f%%", 0.0);
    }
    return String.format("%+." + scale + "f%%", rounded);
  }

  /**
   * 표시 자릿수로 먼저 반올림한다. BigDecimal 에는 음의 영이 없어 -0.04 는 0.0(부호 없음)이 되고, 그 값을 Formatter 에 넘기면 "-0.0%"
   * 대신 "0.0%"/"+0.0%" 가 나온다. NaN/Infinity 는 그대로 둔다.
   */
  static double roundForDisplay(double value, int scale) {
    if (!Double.isFinite(value)) return value;
    return new java.math.BigDecimal(value)
        .setScale(scale, java.math.RoundingMode.HALF_UP)
        .doubleValue();
  }
}
