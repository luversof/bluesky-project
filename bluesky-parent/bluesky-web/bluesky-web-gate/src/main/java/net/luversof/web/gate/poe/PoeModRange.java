package net.luversof.web.gate.poe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모드 티어의 최소롤·최대롤 문장을 합쳐 범위 문장으로 만든다. 두 문장은 숫자만 다르고 나머지 텍스트는 같다는 전제(같은 스탯 서술) 하에, 등장 순서대로 숫자를 짝지어
 * 다르면 {@code min–max}, 같으면 그 값 하나로 표기한다. 예: "생명력 최대치 +130" + "생명력 최대치 +144" → "생명력 최대치 +130–144".
 */
public final class PoeModRange {

  private PoeModRange() {}

  private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

  public static String range(String min, String max) {
    if (max == null) {
      return "";
    }
    if (min == null || min.equals(max)) {
      return max;
    }
    Matcher mn = NUMBER.matcher(min);
    // max 문장을 뼈대로, 등장하는 각 숫자를 min 의 대응 숫자와 합친다.
    StringBuilder sb = new StringBuilder();
    Matcher mx = NUMBER.matcher(max);
    int last = 0;
    while (mx.find()) {
      sb.append(max, last, mx.start());
      String maxNum = mx.group();
      String minNum = mn.find() ? mn.group() : null;
      if (minNum != null && !minNum.equals(maxNum)) {
        sb.append("(").append(minNum).append("–").append(maxNum).append(")");
      } else {
        sb.append(maxNum);
      }
      last = mx.end();
    }
    sb.append(max.substring(last));
    return sb.toString();
  }
}
