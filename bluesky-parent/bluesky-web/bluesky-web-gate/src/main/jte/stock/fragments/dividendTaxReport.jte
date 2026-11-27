@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.dto.response.DividendTaxReport
@import net.luversof.web.gate.stock.service.DividendTaxReportSupport
@import net.luversof.web.gate.stock.support.StockViewSupport
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 종소세 신고 자료(2026-11-28). 귀속연도 배당을 일반 · 고배당 · 과세이연으로 나누고, 고배당 분리과세를 신청할 때와 안 할 때를 나란히 본다.
     세액은 api-stock 이 계산한다(DividendTaxReportCalculator) - 여기서는 그리기만 한다. 계산할 수 없는 값은 null 이고 "-" 와 까닭을 적는다. --%>
@param DividendTaxReportSupport.View view = null
@param String inputResult = ""

@if(view != null)
	!{
		DividendTaxReport report = view.report();
		java.util.function.Function<BigDecimal, String> won = v -> v == null ? "-" : StockFormatUtil.fullKrw(StockFormatUtil.displayWon(v));
		boolean showWith = report != null && report.withSeparation() != null;
		String recommendationKey = report == null || report.recommendation() == null ? null : switch (report.recommendation()) {
			case "NONE_NEEDED" -> "stock.dividend.tax.recommend.none";
			case "WITH" -> "stock.dividend.tax.recommend.with";
			case "WITHOUT" -> "stock.dividend.tax.recommend.without";
			default -> null;
		};
		String needOther = MessageUtil.getMessage("stock.dividend.tax.need.other.short");
		boolean isaWarning = report != null && report.chosen() != null && report.chosen().isaRestricted();
	}
	<section id="tax-report" class="scroll-mt-24 rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm" data-tax-report>
		<h2 class="text-base font-semibold" data-page-section>${MessageUtil.getMessage("stock.dividend.tax.title")}</h2>
		<p class="mt-1 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.desc")}</p>

		<nav class="mt-3" aria-label="${MessageUtil.getMessage("stock.dividend.tax.year.label")}">
			<div class="tabs tabs-box w-full bg-base-200/70 p-1">
				@for(Integer y : view.years())
					<a href="/stock/dividend?taxYear=${y}#tax-report" aria-current="${y == view.year() ? "page" : null}" class="${y == view.year() ? "tab flex-1 tab-active !bg-primary !text-primary-content font-semibold" : "tab flex-1 text-base-content/60 hover:text-base-content"}">${y}</a>
				@endfor
			</div>
		</nav>

		@if("saved".equals(inputResult))
			<div class="alert alert-success mt-3" role="alert"><span>${MessageUtil.getMessage("stock.dividend.tax.input.saved")}</span></div>
		@elseif("save-failed".equals(inputResult))
			<div class="alert alert-error mt-3" role="alert"><span>${MessageUtil.getMessage("stock.dividend.tax.input.failed")}</span></div>
		@endif

		@if(report == null)
			<div class="alert alert-warning mt-3" role="alert"><span>${MessageUtil.getMessage("stock.dividend.tax.load.failed")}</span></div>
		@else
			@if(report.inProgress())
				<p class="mt-2 text-xs text-base-content/60" data-tax-in-progress>${MessageUtil.getMessage("stock.dividend.tax.in.progress")}</p>
			@endif

			@if(recommendationKey != null)
				<div class="alert ${"NONE_NEEDED".equals(report.recommendation()) ? "alert-success" : "alert-info"} mt-3" role="status" data-tax-recommendation="${report.recommendation()}">
					<span>${MessageUtil.getMessage(recommendationKey)}</span>
				</div>
			@endif
			@if(isaWarning)
				<div class="alert alert-warning mt-2" role="alert" data-tax-isa-warning><span>${MessageUtil.getMessage("stock.dividend.tax.isa.warning")}</span></div>
			@endif
			@if(report.unknownTaxableCount() > 0)
				<p class="mt-2 text-xs" data-tax-unknown><span class="inline-block rounded border border-warning/30 bg-warning/10 px-1.5 py-0.5 text-base-content">${StockViewSupport.msg("stock.dividend.tax.unknown", String.valueOf(report.unknownTaxableCount()))}</span></p>
			@endif

			<%-- 총 배당(세전)이 일반 · 고배당기업 · 과세이연으로 어떻게 나뉘는지(사용자 요청 2026-11-28). 세전 합은 내역 줄을 더한 값, 과세금액은 api 가 준 값. --%>
			<dl class="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4" data-tax-breakdown>
				<div class="min-w-0 rounded-xl border border-primary/30 bg-primary/5 p-3">
					<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.total")}</dt>
					<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-tax-total>${won.apply(report.grossOf(null))}</dd>
					<dd class="mt-1 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.withheld")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.withheld")) <span class="tabular-nums amount-value" data-tax-withheld>${won.apply(report.withheldTax())}</span></dd>
				</div>
				<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
					<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.general")}</dt>
					<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-tax-general-gross>${won.apply(report.grossOf("GENERAL"))}</dd>
					<dd class="mt-1 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.col.taxable")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.taxable")) <span class="tabular-nums amount-value" data-tax-general>${won.apply(report.generalTaxable())}</span></dd>
				</div>
				<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
					<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.high")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.high"))</dt>
					<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-tax-high-gross>${won.apply(report.grossOf("HIGH_DIVIDEND"))}</dd>
					<dd class="mt-1 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.col.taxable")} <span class="tabular-nums amount-value" data-tax-high>${won.apply(report.highDividendTaxable())}</span></dd>
				</div>
				<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
					<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.deferred")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.deferred"))</dt>
					<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-tax-deferred>${won.apply(report.deferredGross())}</dd>
					<dd class="mt-1 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.deferred.excluded")}</dd>
				</div>
			</dl>

			<h3 class="mt-4 text-sm font-semibold">${MessageUtil.getMessage("stock.dividend.tax.scenario.title")}</h3>
			<%-- 두 열이 무엇을 뜻하는지 먼저 말로 적는다(사용자 요청 2026-11-28: 표만으로는 차이를 알 수 없었다). --%>
			<ul class="mt-2 grid gap-2 text-xs text-base-content/80 ${showWith ? "sm:grid-cols-2" : ""}" data-tax-scenario-explain>
				<li class="rounded-lg bg-base-200/60 p-2"><span class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.scenario.without")}</span> - ${MessageUtil.getMessage(showWith ? "stock.dividend.tax.scenario.without.explain" : "stock.dividend.tax.scenario.only.explain")}</li>
				@if(showWith)
					<li class="rounded-lg bg-base-200/60 p-2"><span class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.scenario.with")}</span> - ${MessageUtil.getMessage("stock.dividend.tax.scenario.with.explain")}</li>
				@endif
			</ul>
			<div class="mt-2 overflow-x-auto">
				<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.dividend.tax.scenario.title")}" data-tax-scenarios>
					<thead>
						<tr>
							<th scope="col"><span class="sr-only">${MessageUtil.getMessage("stock.dividend.tax.scenario.item")}</span></th>
							<th scope="col" class="text-right whitespace-nowrap">${MessageUtil.getMessage("stock.dividend.tax.scenario.without")}</th>
							@if(showWith)
								<th scope="col" class="text-right whitespace-nowrap">${MessageUtil.getMessage("stock.dividend.tax.scenario.with")}</th>
							@endif
						</tr>
					</thead>
					<tbody>
						!{ var a = report.withoutSeparation(); var b = report.withSeparation(); }
						<tr data-tax-filing>
							<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.filing")}</th>
							<td class="text-right">${MessageUtil.getMessage(a.comprehensive() ? "stock.dividend.tax.filing.comprehensive" : "stock.dividend.tax.filing.none")}</td>
							@if(showWith)<td class="text-right">${MessageUtil.getMessage(b.comprehensive() ? "stock.dividend.tax.filing.comprehensive.separation" : "stock.dividend.tax.filing.separation")}</td>@endif
						</tr>
						<tr>
							<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.financial")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.financial"), placement = "top")</th>
							<td class="text-right tabular-nums amount-value">${won.apply(a.financialIncome())}</td>
							@if(showWith)<td class="text-right tabular-nums amount-value">${won.apply(b.financialIncome())}</td>@endif
						</tr>
						<tr>
							<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.comprehensive")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.comprehensive"), placement = "top")</th>
							<td class="text-right">${MessageUtil.getMessage(a.comprehensive() ? "stock.dividend.tax.yes" : "stock.dividend.tax.no")}</td>
							@if(showWith)<td class="text-right">${MessageUtil.getMessage(b.comprehensive() ? "stock.dividend.tax.yes" : "stock.dividend.tax.no")}</td>@endif
						</tr>
						<tr>
							<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.excess")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.excess"), placement = "top")</th>
							<td class="text-right tabular-nums amount-value">${won.apply(a.excess())}</td>
							@if(showWith)<td class="text-right tabular-nums amount-value">${won.apply(b.excess())}</td>@endif
						</tr>
						<tr>
							<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.grossup")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.grossup"), placement = "top")</th>
							<td class="text-right tabular-nums amount-value">${won.apply(a.grossUp())}</td>
							@if(showWith)<td class="text-right tabular-nums amount-value">${won.apply(b.grossUp())}</td>@endif
						</tr>
						@if(showWith)
							<tr>
								<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.separate")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.separate"), placement = "top")</th>
								<td class="text-right">-</td>
								<td class="text-right tabular-nums amount-value">${won.apply(b.highDividendSeparateTax())}</td>
							</tr>
						@endif
						<tr>
							<th scope="row" class="font-normal">${MessageUtil.getMessage("stock.dividend.tax.row.additional")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.additional"), placement = "top")</th>
							<td class="text-right tabular-nums amount-value" data-tax-additional-without>${a.additionalNationalTax() == null && a.comprehensive() ? needOther : won.apply(a.additionalNationalTax())}</td>
							@if(showWith)<td class="text-right tabular-nums amount-value" data-tax-additional-with>${b.additionalNationalTax() == null && b.comprehensive() ? needOther : won.apply(b.additionalNationalTax())}</td>@endif
						</tr>
						<tr>
							<th scope="row" class="font-semibold">${MessageUtil.getMessage("stock.dividend.tax.row.additional.total")} @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.additional.total"), placement = "top")</th>
							<td class="text-right tabular-nums font-semibold amount-value">${a.additionalTotalTax() == null && a.comprehensive() ? needOther : won.apply(a.additionalTotalTax())}</td>
							@if(showWith)<td class="text-right tabular-nums font-semibold amount-value">${b.additionalTotalTax() == null && b.comprehensive() ? needOther : won.apply(b.additionalTotalTax())}</td>@endif
						</tr>
					</tbody>
				</table>
			</div>
			@if(!report.ruleSupported())
				<p class="mt-2 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.rule.unsupported")}</p>
			@elseif(report.otherIncomeTaxBase() == null)
				<p class="mt-2 text-xs text-base-content/60" data-tax-need-other-income>${MessageUtil.getMessage("stock.dividend.tax.need.other.income")}</p>
			@endif

			<form method="post" action="/stock/dividend/tax-input" class="mt-4 grid gap-3 sm:grid-cols-3 sm:items-end" data-tax-input-form>
				<input type="hidden" name="year" value="${view.year()}" />
				<label class="form-control min-w-0">
					<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.dividend.tax.input.other.income")}</span> @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.other.income"))</div>
					<input name="otherIncomeTaxBase" type="number" min="0" step="1" inputmode="numeric" value="${report.otherIncomeTaxBase() != null ? report.otherIncomeTaxBase().toPlainString() : ""}" class="input input-bordered w-full min-w-0 input-sm" />
				</label>
				<label class="form-control min-w-0">
					<div class="label"><span class="label-text font-medium">${MessageUtil.getMessage("stock.dividend.tax.input.other.financial")}</span> @template._components.ui.infoTip(desc = MessageUtil.getMessage("stock.dividend.tax.help.other.financial"))</div>
					<input name="otherFinancialIncome" type="number" min="0" step="1" inputmode="numeric" value="${report.otherFinancialIncome() != null ? report.otherFinancialIncome().toPlainString() : ""}" class="input input-bordered w-full min-w-0 input-sm" />
				</label>
				<button type="submit" class="btn btn-primary btn-sm">${MessageUtil.getMessage("stock.dividend.tax.input.submit")}</button>
			</form>
			@if(report.otherFinancialIncome() == null)
				<p class="mt-1 text-xs text-base-content/60" data-tax-interest-excluded>${MessageUtil.getMessage("stock.dividend.tax.interest.excluded")}</p>
			@endif

			@if(report.rows() != null && !report.rows().isEmpty())
				<details class="mt-4">
					<summary class="cursor-pointer text-sm font-medium">${StockViewSupport.msg("stock.dividend.tax.rows.title", String.valueOf(report.rows().size()))}</summary>
					<div class="mt-2 overflow-x-auto">
						<table class="table data-table" aria-label="${MessageUtil.getMessage("stock.dividend.tax.rows.label")}" data-tax-rows>
							<thead>
								<tr>
									<th scope="col">${MessageUtil.getMessage("stock.dividend.tax.col.account")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.dividend.tax.col.stock")}</th>
									<th scope="col">${MessageUtil.getMessage("stock.dividend.tax.col.category")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.dividend.tax.col.count")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.dividend.tax.col.gross")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.dividend.tax.col.taxable")}</th>
									<th scope="col" class="text-right">${MessageUtil.getMessage("stock.dividend.tax.col.tax")}</th>
								</tr>
							</thead>
							<tbody>
								@for(DividendTaxReport.Row row : report.rows())
									!{
										String categoryKey = switch (row.category() == null ? "" : row.category()) {
											case "HIGH_DIVIDEND" -> "stock.dividend.tax.category.high";
											case "DEFERRED" -> "stock.dividend.tax.category.deferred";
											default -> "stock.dividend.tax.category.general";
										};
									}
									<tr data-tax-row-category="${row.category()}">
										<td>${view.accountNames().getOrDefault(row.accountId(), "-")}</td>
										<td>${row.stockItemName() != null ? row.stockItemName() : "-"}</td>
										<td class="whitespace-nowrap">${MessageUtil.getMessage(categoryKey)}</td>
										<td class="text-right tabular-nums">${row.count()}</td>
										<td class="text-right tabular-nums amount-value">${won.apply(row.grossAmount())}</td>
										<td class="text-right tabular-nums amount-value">${won.apply(row.taxableAmount())}</td>
										<td class="text-right tabular-nums amount-value">${won.apply(row.tax())}</td>
									</tr>
								@endfor
							</tbody>
						</table>
					</div>
				</details>
			@endif
			<p class="mt-3 text-xs text-base-content/60">${MessageUtil.getMessage("stock.dividend.tax.note")}</p>
		@endif
	</section>
@endif
