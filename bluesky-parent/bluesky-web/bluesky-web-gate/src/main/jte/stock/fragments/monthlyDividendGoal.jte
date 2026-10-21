@import io.github.luversof.boot.context.support.MessageUtil
@import java.math.BigDecimal
@import net.luversof.web.gate.stock.service.MonthlyDividendGoalCalculator
@import net.luversof.web.gate.stock.util.StockFormatUtil

<%-- 목표 월배당까지(2026-10-20 자율 개선) - 월 얼마를 받으려면 얼마를 더 사야 하고, 매달 적립 + 분배금 재투자면 몇 달 걸리는지.
     기준은 등록된 전 종목(표 거르기와 무관)이라 결과 구역 밖에 두고 이 구역만 따로 바꿔 끼운다 - 칩 · 정렬을 바꿔도 이 칸은 그대로고,
     이 칸을 계산해도 아래 표 조건은 그대로다. 주소는 건드리지 않는다(replace-url=false) - 새로 고침하면 표 조건이 남고 목표 칸은 빈다.
     계산은 MonthlyDividendGoalCalculator. --%>
@param MonthlyDividendGoalCalculator.Basis basis = null
@param MonthlyDividendGoalCalculator.Goal goal = null
@param BigDecimal goalMonthly = null
@param BigDecimal goalContribution = null

<section id="monthlyDividendGoal" class="rounded-2xl border border-base-300 bg-base-100 p-4 shadow-sm" hx-target="#monthlyDividendGoal" hx-select="#monthlyDividendGoal" hx-swap="outerHTML show:none" hx-push-url="false" hx-replace-url="false" data-monthly-goal>
	<h2 class="text-base font-bold" data-page-section>${MessageUtil.getMessage("stock.simulator.monthly.goal.title")}</h2>
	@if(basis == null || basis.rowCount() == 0 || basis.marketValue().signum() <= 0)
		<p class="mt-1 text-sm text-base-content/60" data-monthly-goal-empty>${MessageUtil.getMessage("stock.simulator.monthly.goal.empty")}</p>
	@else
		<p class="mt-1 text-xs text-base-content/60 wrap-anywhere" data-monthly-goal-basis>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.goal.basis"), String.valueOf(basis.rowCount()))} <span class="font-mono text-base-content/80 amount-value" data-monthly-goal-current>${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(basis.monthlyDividend()))}</span> · ${MessageUtil.getMessage("stock.simulator.monthly.goal.market.value")} <span class="font-mono amount-value">${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(basis.marketValue()))}</span> · ${MessageUtil.getMessage("stock.simulator.monthly.goal.yield")} <span class="font-mono" data-monthly-goal-yield>${basis.annualYieldPct().toPlainString()}%</span></p>
		<form method="get" action="/stock/simulator" hx-boost="true" class="mt-3 flex flex-wrap items-end gap-3" data-swap-anchor-key="monthly-goal" data-monthly-goal-form>
			<input type="hidden" name="tab" value="monthly-dividend" />
			<label class="form-control min-w-0">
				<span class="label-text text-xs">${MessageUtil.getMessage("stock.simulator.monthly.goal.field.goal")}</span>
				<input type="text" inputmode="numeric" name="goalMonthly" value="${goalMonthly != null ? goalMonthly.toPlainString() : ""}" class="input input-bordered input-sm w-40 font-mono" placeholder="3000000" autocomplete="off" />
			</label>
			<label class="form-control min-w-0">
				<span class="label-text text-xs">${MessageUtil.getMessage("stock.simulator.monthly.goal.field.contribution")}</span>
				<input type="text" inputmode="numeric" name="goalContribution" value="${goalContribution != null ? goalContribution.toPlainString() : ""}" class="input input-bordered input-sm w-40 font-mono" placeholder="0" autocomplete="off" />
			</label>
			<button type="submit" class="btn btn-sm btn-primary">${MessageUtil.getMessage("stock.simulator.monthly.goal.submit")}</button>
		</form>
		@if(goal != null)
			@if(goal.reached())
				<p class="mt-3 text-sm" data-monthly-goal-reached>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.goal.reached"), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(goal.goalMonthly())))}</p>
			@else
				<%-- 계산 흐름대로: 모자란 월배당 → 한 번에 더 살 금액 → 매달 적립하면 걸리는 기간. --%>
				<dl class="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3" data-monthly-goal-result>
					<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
						<dt class="text-xs text-base-content/60">${MessageUtil.getMessage("stock.simulator.monthly.goal.shortfall")}</dt>
						<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-monthly-goal-shortfall>${StockFormatUtil.fullKrw(StockFormatUtil.displayWon(goal.shortfall()))}</dd>
					</div>
					<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
						<dt class="text-xs text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.goal.additional"), basis.annualYieldPct().toPlainString())}</dt>
						<dd class="mt-1 font-mono text-lg font-semibold amount-value" data-monthly-goal-additional>${goal.additionalCapital() != null ? StockFormatUtil.fullKrw(StockFormatUtil.displayWon(goal.additionalCapital())) : "-"}</dd>
					</div>
					<div class="min-w-0 rounded-xl bg-base-200/60 p-3">
						<dt class="text-xs text-base-content/60">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.goal.months"), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(goal.monthlyContribution())))}</dt>
						@if(goal.monthsToGoal() != null)
							<dd class="mt-1 font-mono text-lg font-semibold" data-monthly-goal-months="${goal.monthsToGoal()}">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.goal.duration"), String.valueOf(goal.years()), String.valueOf(goal.remainderMonths()))}</dd>
							<dd class="mt-0.5 text-[11px] text-base-content/60 wrap-anywhere" data-monthly-goal-at>${java.text.MessageFormat.format(MessageUtil.getMessage("stock.simulator.monthly.goal.at"), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(goal.capitalAtGoal())), StockFormatUtil.fullKrw(StockFormatUtil.displayWon(goal.contributedAtGoal())))}</dd>
						@else
							<dd class="mt-1 text-sm" data-monthly-goal-months="none">${MessageUtil.getMessage("stock.simulator.monthly.goal.never")}</dd>
						@endif
					</div>
				</dl>
			@endif
			<p class="mt-2 text-xs text-base-content/60" data-monthly-goal-note>${MessageUtil.getMessage("stock.simulator.monthly.goal.note")}</p>
		@endif
	@endif
</section>
