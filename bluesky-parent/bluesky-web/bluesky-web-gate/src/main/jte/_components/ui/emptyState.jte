@import io.github.luversof.boot.context.support.MessageUtil
@param String message = ""
@param String hint = ""
@param boolean boxed = false
@param String padding = "py-10"
@param boolean widenRangeAction = false
@param String actionLabel = ""
@param String actionHref = ""

!{ String msg = (message != null && !message.isEmpty()) ? message : MessageUtil.getMessage("common.message.no.data"); }
!{ String widenLabel = MessageUtil.getMessage("stock.empty.cta.widen.range"); }
<div class="${boxed ? "bg-base-200 rounded-box " : ""}flex flex-col items-center justify-center gap-1 ${padding} text-center text-base-content/50">
	<svg xmlns="http://www.w3.org/2000/svg" class="h-8 w-8 opacity-40" fill="none" viewBox="0 0 24 24" stroke="currentColor">
		<path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-4.586a1 1 0 00-.707.293l-1.414 1.414a1 1 0 01-.707.293h-2.172a1 1 0 01-.707-.293l-1.414-1.414A1 1 0 008.586 13H4" />
	</svg>
	<div class="text-sm">${msg}</div>
	@if(hint != null && !hint.isEmpty())
		<div class="text-xs opacity-70">${hint}</div>
	@endif
	<%-- 빈 상태에서의 다음 행동: 기간 넓히기(같은 화면의 '전체' 프리셋 클릭) 또는 지정 링크 --%>
	@if(widenRangeAction)
		<button type="button" class="btn btn-sm btn-outline mt-2" data-empty-widen-range>${widenLabel}</button>
	@elseif(actionHref != null && !actionHref.isEmpty() && actionLabel != null && !actionLabel.isEmpty())
		<a href="${actionHref}" class="btn btn-sm btn-outline mt-2">${actionLabel}</a>
	@endif
</div>
