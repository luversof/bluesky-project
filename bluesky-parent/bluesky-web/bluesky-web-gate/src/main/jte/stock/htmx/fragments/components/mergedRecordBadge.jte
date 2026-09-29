@import io.github.luversof.boot.context.support.MessageUtil

@param int recordCount
@param int accountCount

<%-- Activity is a merged row: (date, type, item, tradeType) is grouped across accounts.
	The account chips explain a merge only while one account contributed one record.
	When the same account has two or more records on that day the chips stay the same,
	so the row silently stands for more than it shows - measured 2026-09-11 (all range):
	310 timeline rows carried 452 account chips but 460 original records (8 hidden).
	Only then do we add the count; otherwise the chips already say it and a badge is noise. --%>
!{boolean merged = recordCount > 1 && recordCount > accountCount;}
<%-- opacity-70 을 달면 안 된다: 이 배지는 text-base-content/60 안에 들어가는 자리가 있어(대시보드 최근 활동)
	투명도가 곱해진다 - 실측 2026-09-11 axe: fg #999b9f / bg #f9f9f9 = 2.64:1 로 serious 2건. --%>
@if(merged)
	!{String mergedCount = java.text.MessageFormat.format(MessageUtil.getMessage("common.count"), recordCount);}
	!{String mergedNote = java.text.MessageFormat.format(MessageUtil.getMessage("stock.activity.merged.records"), recordCount);}
	<%-- 누르는 자리 24px(WCAG 2.5.8): 배지는 17px 라 대시보드 최근 활동(1280px)에서 옆 링크와 간격 예외도 못 받았다(실측 2026-09-24).
		감싸는 칸만 24px 로 - 배지 모양은 그대로. tap-target-24(::after 확장)는 daisyUI 툴팁의 ::after(말풍선 화살표)와 겹쳐 못 쓴다. --%>
	<div class="tooltip tooltip-bottom inline-flex min-h-6 items-center" role="note" tabindex="0" aria-label="${mergedNote}" data-tip="${mergedNote}" data-merged-records="${recordCount}">
		<span class="badge badge-xs badge-outline cursor-help">${mergedCount}</span>
	</div>
@endif
