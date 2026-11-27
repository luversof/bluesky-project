@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.DividendTaxReport
@import net.luversof.web.gate.stock.support.StockViewSupport
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 종소세 신고 탭 비교표 한 열(시나리오)의 계산 과정(사용자 요청 2026-11-28: 금액이 어디서 나왔는지 알 수 있게).
     값은 api-stock 이 낸 Scenario.detail 을 그대로 옮긴다 - 여기서 다시 계산하는 것은 ①~⑤ 의 합(0 으로 자르기 전 값) 하나뿐이다. --%>
@param DividendTaxReport report
@param DividendTaxReport.Scenario scenario
@param boolean separated = false
@param boolean hasHigh = false

!{
	java.util.function.Function<BigDecimal, String> won = v -> v == null ? "-" : StockFormatUtil.fullKrw(StockFormatUtil.displayWon(v));
	DividendTaxReport.Detail d = scenario.detail();
	BigDecimal general = report.generalTaxable();
	BigDecimal high = report.highDividendTaxable();
	BigDecimal other = report.otherFinancialIncome() == null ? BigDecimal.ZERO : report.otherFinancialIncome();
	String financialLine = separated
			? StockViewSupport.msg("stock.dividend.tax.calc.financial.with", won.apply(general), won.apply(other), won.apply(scenario.financialIncome()), won.apply(high))
			: hasHigh
					? StockViewSupport.msg("stock.dividend.tax.calc.financial.without", won.apply(general), won.apply(high), won.apply(other), won.apply(scenario.financialIncome()))
					: StockViewSupport.msg("stock.dividend.tax.calc.financial.only", won.apply(general), won.apply(other), won.apply(scenario.financialIncome()));
	boolean hasComprehensiveTax = d != null && d.combinedTax() != null;
	BigDecimal rawExtra = hasComprehensiveTax
			? d.thresholdTax().add(d.combinedTax().tax()).subtract(scenario.grossUp()).subtract(d.otherTax().tax()).subtract(d.financialWithheld())
			: null;
	boolean hasSlices = d != null && d.separatedSlices() != null && !d.separatedSlices().isEmpty();
}
<ol class="space-y-2 text-xs leading-relaxed" data-tax-calc="${separated ? "with" : "without"}">
	<li class="tabular-nums">${financialLine}</li>
	@if(!scenario.comprehensive())
		<li>${StockViewSupport.msg("stock.dividend.tax.calc.below", won.apply(scenario.financialIncome()))}</li>
	@else
		<li class="tabular-nums">${StockViewSupport.msg("stock.dividend.tax.calc.excess", won.apply(scenario.financialIncome()), won.apply(scenario.excess()))}</li>
		@if(d != null)
			!{
				BigDecimal ineligible = scenario.financialIncome().subtract(d.grossUpEligible());
				BigDecimal ineligibleDividend = ineligible.subtract(other);
				BigDecimal room = net.luversof.web.gate.stock.dto.view.FinancialIncomeThresholdView.THRESHOLD.subtract(ineligible).max(BigDecimal.ZERO);
			}
			<li data-tax-calc-grossup>
				<p class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.calc.grossup.title")}</p>
				<ul class="mt-1 space-y-0.5 tabular-nums">
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.grossup.eligible", won.apply(d.grossUpEligible()))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.grossup.ineligible", won.apply(ineligibleDividend), won.apply(other), won.apply(ineligible))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.grossup.room", won.apply(ineligible), won.apply(room))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.grossup.base", won.apply(d.grossUpEligible()), won.apply(room), won.apply(d.grossUpBase()))}</li>
					<li class="font-semibold">${StockViewSupport.msg("stock.dividend.tax.calc.grossup", won.apply(d.grossUpBase()), d.grossUpRate() == null ? "-" : d.grossUpRate().movePointRight(2).stripTrailingZeros().toPlainString(), won.apply(scenario.grossUp()))}</li>
				</ul>
				<p class="mt-0.5 text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.calc.grossup.note")}</p>
			</li>
		@endif
		<li>
			<p class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.calc.comprehensive.title")}</p>
			@if(hasComprehensiveTax)
				<ul class="mt-1 space-y-0.5 tabular-nums">
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.threshold", won.apply(d.thresholdTax()))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.combined", won.apply(report.otherIncomeTaxBase()), won.apply(scenario.excess()), won.apply(scenario.grossUp()), won.apply(d.combinedTax().base()), String.valueOf(d.combinedTax().rate()), won.apply(d.combinedTax().deduction()), won.apply(d.combinedTax().tax()))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.credit", won.apply(scenario.grossUp()))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.other", won.apply(d.otherTax().base()), String.valueOf(d.otherTax().rate()), won.apply(d.otherTax().deduction()), won.apply(d.otherTax().tax()))}</li>
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.withheld", won.apply(scenario.financialIncome()), won.apply(d.financialWithheld()))}</li>
					<li class="font-semibold">${StockViewSupport.msg("stock.dividend.tax.calc.financial.extra", won.apply(rawExtra), won.apply(d.financialExtra()))}</li>
				</ul>
				@if(rawExtra.signum() < 0)
					<p class="mt-0.5 text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.calc.financial.extra.floor")}</p>
				@endif
			@else
				<p class="mt-0.5 text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.calc.need.other")}</p>
			@endif
		</li>
	@endif
	@if(hasSlices)
		<li data-tax-calc-separate>
			<p class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.calc.separate.title")}</p>
			<ul class="mt-1 space-y-0.5 tabular-nums">
				@for(DividendTaxReport.Slice slice : d.separatedSlices())
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.slice", won.apply(slice.amount()), String.valueOf(slice.rate()), won.apply(slice.tax()))}</li>
				@endfor
				<li>${StockViewSupport.msg("stock.dividend.tax.calc.separate.sum", won.apply(scenario.highDividendSeparateTax()))}</li>
				<li>${StockViewSupport.msg("stock.dividend.tax.calc.separate.withheld", won.apply(high), won.apply(d.separatedWithheld()))}</li>
				<li class="font-semibold">${StockViewSupport.msg("stock.dividend.tax.calc.separate.extra", won.apply(scenario.highDividendSeparateTax()), won.apply(d.separatedWithheld()), won.apply(d.separatedExtra()))}</li>
			</ul>
		</li>
	@endif
	@if(scenario.additionalNationalTax() != null && d != null)
		<li>
			<p class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.calc.total.title")}</p>
			<ul class="mt-1 space-y-0.5 tabular-nums">
				@if(d.separatedExtra() != null)
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.total.national", won.apply(d.financialExtra()), won.apply(d.separatedExtra()), won.apply(scenario.additionalNationalTax()))}</li>
				@else
					<li>${StockViewSupport.msg("stock.dividend.tax.calc.total.national.only", won.apply(scenario.additionalNationalTax()))}</li>
				@endif
				<li>${StockViewSupport.msg("stock.dividend.tax.calc.total.local", won.apply(scenario.additionalNationalTax()), won.apply(d.localTax()))}</li>
				<li class="font-semibold">${StockViewSupport.msg("stock.dividend.tax.calc.total.sum", won.apply(scenario.additionalNationalTax()), won.apply(d.localTax()), won.apply(scenario.additionalTotalTax()))}</li>
			</ul>
		</li>
	@endif
</ol>
