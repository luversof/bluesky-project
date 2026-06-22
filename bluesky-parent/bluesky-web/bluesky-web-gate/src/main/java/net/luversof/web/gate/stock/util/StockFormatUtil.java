package net.luversof.web.gate.stock.util;

/** 화면 표시용 숫자 포맷 헬퍼. 요약 카드의 큰 금액을 억/만 단위로 압축해 가독성을 높인다(정확값은 툴팁으로 노출). */
public final class StockFormatUtil {

  private StockFormatUtil() {}

  /**
   * 원 단위 금액을 한국식 억/만 압축 표기로 변환한다. 만 미만 잔여는 버린다(요약용). 예) 123,456,789 → "1억 2,345만", 23,100,000 →
   * "2,310만", 5,300 → "5,300", -2,310,000 → "-231만".
   */
  public static String compactKrw(long value) {
    if (value == 0) {
      return "0";
    }

    String sign = value < 0 ? "-" : "";
    long abs = Math.abs(value);
    long eok = abs / 100_000_000L;
    long man = (abs % 100_000_000L) / 10_000L;

    if (eok > 0) {
      StringBuilder sb = new StringBuilder(sign).append(String.format("%,d", eok)).append("억");
      if (man > 0) {
        sb.append(" ").append(String.format("%,d", man)).append("만");
      }
      return sb.toString();
    }
    if (man > 0) {
      return sign + String.format("%,d", man) + "만";
    }
    // 1만 미만은 원 단위 그대로 표기
    return sign + String.format("%,d", abs);
  }
}
