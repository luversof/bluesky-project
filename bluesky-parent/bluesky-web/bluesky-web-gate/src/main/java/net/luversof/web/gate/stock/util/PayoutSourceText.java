package net.luversof.web.gate.stock.util;

import java.math.BigDecimal;

import org.springframework.util.StringUtils;

/**
 * 월배당 분배금 출처 파서들(KODEX · PLUS · RISE · SOL · TIGER · TIME)이 함께 쓰는 글자 다듬기.
 *
 * <p>2026-10-22 까지 파서마다 같은 머리 줄 상수 · 날짜 맞추기 · 숫자 평문화 · 펀드 코드 뽑기를 한 벌씩 들고 있었다(머리 줄 여섯 벌 - 하나만 바뀌면
 * 가져오기 파서가 그 출처만 못 읽는다).
 */
final class PayoutSourceText {

  /** 가져오기 파서가 읽는 머리 줄. 출처마다 이 순서(지급기준일 · 실지급일 · 분배금액 · 주당과세표준액)로 내보낸다. */
  static final String BULK_INPUT_HEADER = "지급기준일\t실지급일\t분배금액(원)\t주당과세표준액(원)";

  private PayoutSourceText() {}

  /**
   * 2026-09-15T00:00:00 · 2026.09.15 · 20260915 · 2026-09-15 를 모두 yyyy-MM-dd 로. 숫자가 8 자리 미만이면 받은
   * 그대로(앞뒤 공백만 뺀다).
   */
  static String isoDate(String value) {
    String digits = value != null ? value.replaceAll("[^0-9]", "") : "";
    if (digits.length() < 8) {
      return value != null ? value.trim() : "";
    }
    return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
  }

  /**
   * 20260915 처럼 날짜만 오는 출처(KODEX · PLUS)용: 숫자가 <b>정확히</b> 8 자리일 때만 yyyy-MM-dd 로, 아니면 받은 그대로(앞뒤 공백만
   * 뺀다). 시각이 붙은 값을 날짜로 자르지 않는다 - 그런 값은 이 출처에서 온 적이 없고, 오면 가져오기 파서가 형식 오류로 알린다.
   */
  static String isoDateOfCompact(String value) {
    String digits = value != null ? value.replaceAll("[^0-9]", "") : "";
    if (digits.length() != 8) {
      return value != null ? value.trim() : "";
    }
    return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
  }

  /** 540.0 을 540 으로(지수 표기도 막는다) - 가져오기 파서가 숫자 문자열을 그대로 읽는다. */
  static String plain(BigDecimal value) {
    return value.stripTrailingZeros().toPlainString();
  }

  /** 상세 주소 경로에서 펀드 코드(영숫자로만 된 마지막 조각)를 뽑는다. 없으면 빈 글자. */
  static String lastCodeSegment(String path) {
    if (!StringUtils.hasText(path)) {
      return "";
    }
    String[] parts = path.split("/");
    for (int index = parts.length - 1; index >= 0; index--) {
      String candidate = parts[index].trim();
      if (StringUtils.hasText(candidate)
          && candidate.chars().allMatch(Character::isLetterOrDigit)) {
        return candidate;
      }
    }
    return "";
  }
}
