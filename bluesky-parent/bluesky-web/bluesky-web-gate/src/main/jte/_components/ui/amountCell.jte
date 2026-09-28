@import java.math.BigDecimal
@import java.text.DecimalFormat

@param BigDecimal value = null
@param boolean signed = false
@param String cssClass = "text-base-content/70"
@param String tdClass = "text-right tabular-nums"
@param boolean zeroAsDash = true
@param boolean bold = false

<%--
    표의 금액 칸 하나.

    표마다 이 규칙을 다시 적다 보니 표기가 갈렸다 - 어떤 표는 0 을 '-' 로, 어떤 표는 ₩0 으로
    적고 있었다(실측 2026-09-07: 연도별 세금·비용만 ₩0 이라, 매매도 배당도 없던 해가 ₩0 으로
    도배됐다). 규칙을 한 곳에 둔다.

      0 -> '-'      0 원과 '그 해엔 아무 일도 없었다' 는 다르게 읽힌다

    그 '-' 는 뜻을 담은 표시이므로 읽혀야 한다. 2026-09-10 까지 text-base-content/30 이었는데
    대비가 라이트 2.00 · 다크 2.46 으로 AA(4.5) 에 한참 못 미쳤다(실측: 매매 19곳 · 자산현황 6곳,
    25곳 모두 셀 안에 이 표시 하나뿐이라 안 보이면 빈 칸으로 읽힌다). /60 으로 올려 라이트 4.94 · 다크 6.00.
    axe 는 이걸 잡지 못했다 - 대비 검사는 캔버스로 색을 해석하는 프로브로 따로 돌린다.
      signed        부호가 뜻을 바꾸는 값(손익)에만 붙인다. 나간 돈에 + 를 붙이면 번 돈처럼 읽힌다
      zeroAsDash    합계처럼 0 자체가 답인 자리에서는 꺼서 ₩0 을 적을 수 있다
--%>
!{
    DecimalFormat amountCellFormat = net.luversof.web.gate.stock.util.StockFormatUtil.wonFormat();
    BigDecimal amountCellValue = value != null ? value : BigDecimal.ZERO;
    boolean amountCellUp = amountCellValue.signum() >= 0;
    boolean amountCellBlank = zeroAsDash && amountCellValue.signum() == 0;
    String amountCellZeroTitle = io.github.luversof.boot.context.support.MessageUtil.getMessage("common.amount.zero.title");
    String amountCellClass = signed ? (amountCellUp ? "text-profit" : "text-loss") : cssClass;
}
<td class="${tdClass}" aria-label="${amountCellBlank && value != null ? amountCellZeroTitle : null}">
    @if(amountCellBlank)
        <%-- 그 "-" 가 0 이라는 뜻인지 값이 없다는 뜻인지 표에서는 알 수 없다 - 실측 2026-09-11:
             배당 상세 목록에서 같은 기호가 두 뜻으로 쓰였다. 여기서는 값이 실제로 있을 때만(=0 원) 까닭을 단다.
             값이 아예 없는 칸(null)은 그대로 둔다 - 0 이라고 말할 근거가 없다. --%>
        <span class="text-base-content/60" title="${value != null ? amountCellZeroTitle : null}">-</span>
    @else
        <%-- 0 에는 부호를 붙이지 않는다 - 방향이 없는 값에 "+" 를 달면 "0 원 벌었다" 처럼 읽힌다
             (실측 2026-09-11: 자산 성장에 "+₩0" 한 곳). StockFormatUtil.signedWon 과 같은 규칙. --%>
        <span class="amount-value ${bold ? "font-semibold " : ""}${amountCellClass}">${signed && amountCellValue.signum() != 0 ? (amountCellUp ? "+" : "-") : ""}&#8361;${amountCellFormat.format(signed ? amountCellValue.abs() : amountCellValue)}</span>
    @endif
</td>
