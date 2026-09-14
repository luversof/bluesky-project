package net.luversof.web.gate.stock.util;

/**
 * 문구 가운데 금액이 끼어 있을 때 앞/뒤 글자를 갈라 낸다.
 *
 * <p>지표 카드의 보조줄({@code statCard} 의 {@code sub})은 통째로 글자라 <b>금액 숨김이 걸리지 않는다</b>. 컴포넌트는 그래서 {@code
 * subBefore} + {@code subAmount}(= {@code amount-value} 로 감싼다) + {@code subAfter} 를 따로 받는다.
 *
 * <p>자리표시자 위치는 로케일마다 다르다 &mdash; {@code stock.trade.realized.basis.gap} 은 한국어가 "이 계좌 기준 {0}"(뒤) 이고
 * 영어가 "{0} on this account's own basis"(앞)다. 그래서 앞/뒤 글자를 박아 두면 한쪽이 깨진다. 패턴을 쪼개 쓴다.
 *
 * <p>실측 2026-09-12(금액 숨김 켜고 10 화면 · 금액 텍스트 540 개): 계좌 상세의 실현손익 카드 보조줄 "이 계좌 기준 +2,063,739" 한 곳만
 * 흐려지지 않았다. 같은 문구를 쓰는 매매 화면의 계좌별 표 3 줄은 정상이었다.
 */
public final class StockMessageSplitUtil {

  private static final String PLACEHOLDER = "{0}";

  private StockMessageSplitUtil() {}

  /** 자리표시자 앞 글자. 자리표시자가 없으면 문구 전체를 앞으로 본다. */
  public static String beforePlaceholder(String pattern) {
    if (pattern == null) {
      return "";
    }
    int at = pattern.indexOf(PLACEHOLDER);
    return at < 0 ? pattern : pattern.substring(0, at);
  }

  /** 자리표시자 뒤 글자. 자리표시자가 없으면 빈 문자열이다. */
  public static String afterPlaceholder(String pattern) {
    if (pattern == null) {
      return "";
    }
    int at = pattern.indexOf(PLACEHOLDER);
    return at < 0 ? "" : pattern.substring(at + PLACEHOLDER.length());
  }
}
