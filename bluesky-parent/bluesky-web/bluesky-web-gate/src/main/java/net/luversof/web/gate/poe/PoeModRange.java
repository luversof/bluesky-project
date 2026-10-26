package net.luversof.web.gate.poe;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모드 등급의 최소롤·최대롤 문장을 합쳐 범위 문장으로 만든다. 두 문장은 숫자만 다르고 나머지 텍스트는 같다는 전제(같은 스탯 서술) 하에, 등장 순서대로 숫자를 짝지어
 * 다르면 {@code min–max}, 같으면 그 값 하나로 표기한다. 예: "생명력 최대치 +130" + "생명력 최대치 +144" → "생명력 최대치 +130–144".
 */
public final class PoeModRange {

  private PoeModRange() {}

  private static final Pattern NUMBER = Pattern.compile("-?\\d+(?:\\.\\d+)?");

  public static String range(String min, String max) {
    return range(min, max, false);
  }

  /**
   * 옵션 표 표시용(10-23 Q85) — '감소' 옵션은 최소롤 문장 숫자가 더 커서 "(20–10)% 감소" 로 나왔다(게임 · PoB 관례 "(10-20)%
   * reduced"). 두 숫자가 모두 0 이상이고 앞이 크면 작은 값을 앞에. 0 을 가로지르는 범위는 그대로. 제작 에뮬레이터의 굴림(씨앗 → 값)은 {@link
   * #range(String, String)} 그대로라 저장된 씨앗 값이 바뀌지 않는다.
   */
  public static String rangeAscending(String min, String max) {
    return range(min, max, true);
  }

  private static String range(String min, String max, boolean ascending) {
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
        String lo = minNum;
        String hi = maxNum;
        if (ascending
            && !lo.startsWith("-")
            && !hi.startsWith("-")
            && Double.parseDouble(lo) > Double.parseDouble(hi)) {
          lo = maxNum;
          hi = minNum;
        }
        sb.append("(").append(lo).append("–").append(hi).append(")");
      } else {
        sb.append(maxNum);
      }
      last = mx.end();
    }
    sb.append(max.substring(last));
    return sb.toString();
  }
}
