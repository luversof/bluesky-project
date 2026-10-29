@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.dto.view.FinancialIncomeThresholdView
@import net.luversof.web.gate.stock.util.StockFormatUtil
@import net.luversof.web.gate.stock.support.StockViewSupport

<%-- 올해 금융소득 종합과세 기준(2026-10-20 자율 개선) - 과세 계좌의 과세표준이 2,000 만 원에 얼마나 다가갔는지.
     배당 내역의 기간 합계는 고른 기간만 말하고 과세이연 계좌까지 섞여, 한 해 기준을 볼 곳이 없었다.
     기간 · 필터와 무관하다(올해 1 월 1 일 ~ 오늘). 조회가 실패하면 view 가 null 이라 구역째 숨긴다 - 0 으로 보이면 "여유 2,000 만 원" 으로 읽힌다.
     숫자는 계산 흐름대로 지금까지 · 더 받을 예상 · 연말 예상(a · b · a+b). --%>
@param FinancialIncomeThresholdView view = null

@if(view != null)
	!{
		String ytdText = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.ytdTaxable()));
		String restText = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.projectedRest()));
		String projectedText = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.projectedYearEnd()));
		String thresholdText = StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.threshold()));
	}
	<section class="rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm" data-financial-income>
		<h2 class="text-base font-bold" data-page-section>${MessageUtil.getMessage("stock.dividend.financial.income.title")}</h2>
		<p class="mt-1 text-xs text-base-content/60">${StockViewSupport.msg("stock.dividend.financial.income.desc", String.valueOf(view.year()))}</p>

		<dl class="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3">
			<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
				<dt class="text-xs text-base-content/60">${StockViewSupport.msg("stock.dividend.financial.income.ytd", view.asOf().toString())}</dt>
				<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-financial-income-ytd>${ytdText}</dd>
			</div>
			<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
				<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.financial.income.rest")}</dt>
				<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-financial-income-rest>${restText}</dd>
				<%-- 종목마다 나눠 센다(2026-10-20 검토): 달마다 나오는 종목은 최근 흐름, 분기 · 연 배당은 작년 이맘때 횟수 x 최근 금액. --%>
				<dd class="mt-0.5 text-[11px] text-base-content/60 wrap-anywhere" data-financial-income-rest-basis>${StockViewSupport.msg("stock.dividend.financial.income.rest.basis", String.valueOf(view.recentWindowDays()), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.recentDailyAverage())), String.valueOf(view.remainingDays()), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.monthlyRest())))}</dd>
				@if(view.periodicCount() > 0)
					<dd class="text-[11px] text-base-content/60 wrap-anywhere" data-financial-income-rest-periodic>${StockViewSupport.msg("stock.dividend.financial.income.rest.periodic", String.valueOf(view.periodicCount()), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.periodicRest())))}</dd>
				@endif
			</div>
			<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
				<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.financial.income.projected")}</dt>
				<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-financial-income-projected>${projectedText}</dd>
				<dd class="mt-0.5 text-[11px] text-base-content/60" data-financial-income-projected-pct>${StockViewSupport.msg("stock.dividend.financial.income.pct.of.threshold", view.projectedPct().toPlainString())}</dd>
			</div>
		</dl>

		<div class="mt-3">
			<div class="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1 text-sm">
				<span><span class="text-base-content/60">${MessageUtil.getMessage("stock.dividend.financial.income.threshold")}</span> <span class="font-mono amount-value">${thresholdText}</span> <span class="text-base-content/60">·</span> <span class="font-mono font-semibold" data-financial-income-used-pct>${view.usedPct().toPlainString()}%</span></span>
				@if(view.exceeded())
					<span class="inline-block rounded border border-warning/30 bg-warning/10 px-1.5 py-0.5 text-base-content" data-financial-income-over>${StockViewSupport.msg("stock.dividend.financial.income.over", StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.over())))}</span>
				@else
					<span class="text-base-content/70" data-financial-income-remaining>${MessageUtil.getMessage("stock.dividend.financial.income.remaining")} <span class="font-mono amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.remaining()))}</span></span>
				@endif
			</div>
			<%-- daisyUI progress 는 빌드된 main.css 에 없다(2026-10-20 확인) - 있는 클래스로 막대를 그린다.
			     연말 예상은 그 뒤에 옅게 잇는다(2026-10-28) - 지금까지 4.6% 막대만 있고 예상 16.6% 는 글자로만 있었다. --%>
			<div class="mt-2 flex h-2 w-full overflow-hidden rounded-full bg-base-200" role="progressbar" aria-valuemin="0" aria-valuemax="100" aria-valuenow="${view.usedBarPct()}" aria-label="${MessageUtil.getMessage("stock.dividend.financial.income.title")}" data-financial-income-bar data-bar-tone="${view.exceeded() || view.projectedToExceed() ? "warning" : "primary"}"><div class="h-2 ${view.exceeded() || view.projectedToExceed() ? "bg-warning" : "bg-primary"}" style="width: ${view.usedBarPct()}%"></div>@if(view.projectedBarPct() > view.usedBarPct())<div class="h-2 ${view.exceeded() || view.projectedToExceed() ? "bg-warning/35" : "bg-primary/35"}" style="width: ${view.projectedBarPct() - view.usedBarPct()}%" title="${MessageUtil.getMessage("stock.dividend.financial.income.projected")}" data-financial-income-projected-bar="${view.projectedBarPct()}"></div>@endif</div>
			@if(view.projectedToExceed())
				<p class="mt-1 text-xs" data-financial-income-projected-exceed><span class="inline-block rounded border border-warning/30 bg-warning/10 px-1.5 py-0.5 text-base-content">${MessageUtil.getMessage("stock.dividend.financial.income.projected.exceed")}</span></p>
			@endif
		</div>

		<ul class="mt-3 space-y-1 text-xs text-base-content/60">
			@if(view.lastYearTaxable() != null && view.lastYearTaxable().signum() > 0)
				<li data-financial-income-last-year>${StockViewSupport.msg("stock.dividend.financial.income.last.year", String.valueOf(view.year() - 1))} <span class="font-mono amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(view.lastYearTaxable()))}</span></li>
			@endif
			@if(view.unknownTaxableCount() > 0)
				<li data-financial-income-unknown><span class="inline-block rounded border border-warning/30 bg-warning/10 px-1.5 py-0.5 text-base-content">${StockViewSupport.msg("stock.dividend.financial.income.unknown", String.valueOf(view.unknownTaxableCount()))}</span></li>
			@endif
			<li data-financial-income-note>${MessageUtil.getMessage("stock.dividend.financial.income.note")}</li>
		</ul>
	</section>
@endif
