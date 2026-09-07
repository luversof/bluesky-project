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
      signed        부호가 뜻을 바꾸는 값(손익)에만 붙인다. 나간 돈에 + 를 붙이면 번 돈처럼 읽힌다
      zeroAsDash    합계처럼 0 자체가 답인 자리에서는 꺼서 ₩0 을 적을 수 있다
--%>
!{
    DecimalFormat amountCellFormat = new DecimalFormat("#,##0");
    BigDecimal amountCellValue = value != null ? value : BigDecimal.ZERO;
    boolean amountCellUp = amountCellValue.signum() >= 0;
    boolean amountCellBlank = zeroAsDash && amountCellValue.signum() == 0;
    String amountCellClass = signed ? (amountCellUp ? "text-profit" : "text-loss") : cssClass;
}
<td class="${tdClass}">
    @if(amountCellBlank)
        <span class="text-base-content/30">-</span>
    @else
        <span class="amount-value ${bold ? "font-semibold " : ""}${amountCellClass}">${signed ? (amountCellUp ? "+" : "-") : ""}&#8361;${amountCellFormat.format(signed ? amountCellValue.abs() : amountCellValue)}</span>
    @endif
</td>
