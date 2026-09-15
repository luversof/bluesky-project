package net.luversof.web.gate.stock.util;

import io.github.luversof.boot.context.support.MessageUtil;

/**
 * 원장 점검 규칙 코드를 사람이 읽는 말로 바꾼다.
 *
 * <p>코드는 <b>다른 서비스</b>(api-stock {@code LedgerIntegrityService})가 정한다. 게이트는 그 코드로 문구 키를 조립해 찾는데,
 * {@link MessageUtil#getMessage(String)} 은 못 찾은 키를 <b>빈 문자열</b>로 돌려준다(예외도 코드도 아니다 &mdash; 한 인자 형태가
 * 기본값으로 {@code ""} 를 넘긴다). 그래서 api-stock 에 규칙이 하나 늘면 관리 화면에 <b>이름 없는 경고</b>가 뜬다 &mdash; "(3)" 처럼
 * 건수만 있고 무엇이 걸렸는지는 어디에도 없다. 여러 사유가 겹친 행은 ", , " 가 된다.
 *
 * <p>실측 2026-09-14 기준으로는 api-stock 의 20 개 코드가 en·ko 양쪽에 다 있다. 지금 깨진 것은 없고, <b>깨졌을 때 조용한 것</b>이 문제다.
 *
 * <p>그래서 모르는 코드는 감추지 않고 코드 그대로 보여 준다 &mdash; 관리 화면에서는 "무엇이 들어 있는지" 가 "예쁘게 보이는 것" 보다 중요하다({@link
 * StockPayoutWindowLabelUtil} 과 같은 규칙).
 */
public final class StockLedgerRuleLabelUtil {

  private static final String PREFIX = "stock.admin.ledger.rule.";

  private StockLedgerRuleLabelUtil() {}

  /** 규칙 문구. 모르는 코드면 코드 그대로. */
  public static String label(String code) {
    if (code == null || code.isBlank()) {
      return "";
    }
    String trimmed = code.trim();
    return MessageUtil.getMessage(PREFIX + trimmed, trimmed);
  }
}
