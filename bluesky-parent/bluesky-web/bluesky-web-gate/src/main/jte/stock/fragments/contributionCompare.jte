@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.service.MonthlyContributionPickSupport
@import net.luversof.web.gate.stock.util.StockPickBasisLabelUtil

<%-- 한 자리의 기준별 1위(사용자 질문 2026-10-06: "기준에 대해 추천을 어떻게 보여줄거야?").
     여러 기준이 같은 종목을 꼽을수록 굳은 추천이다 - 몇 개가 꼽았는지 먼저 적고, 기준마다 1위와 그 근거를 한 줄씩 둔다.
     기준마다 갈리면 하나를 억지로 고르지 않고 갈린다고 말한다. 시뮬레이터 자리 카드와 월배당 ETF 자리 보기가 같이 쓴다. --%>
@param MonthlyContributionPickSupport.ContributionPick pick
@param java.text.DecimalFormat percentFormat

@if(pick.tops() != null && pick.tops().size() > 1)
	<div class="mt-2 border-t border-base-200 pt-2 space-y-1" data-contribution-compare data-agreement="${pick.agreement()}" data-basis-count="${pick.tops().size()}">
		<div class="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 text-xs">
			<span class="font-medium text-base-content/70">${MessageUtil.getMessage("stock.pick.compare.title")}</span>
			@if(pick.agreement() >= pick.tops().size())
				<span class="badge badge-sm badge-success h-auto whitespace-normal" data-compare-verdict="all">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.pick.compare.all"), pick.tops().size())}</span>
			@elseif(pick.agreement() >= 2)
				<span class="badge badge-sm badge-info h-auto whitespace-normal" data-compare-verdict="some">${java.text.MessageFormat.format(MessageUtil.getMessage("stock.pick.compare.some"), pick.tops().size(), pick.agreement())}</span>
			@else
				<span class="text-base-content/60" data-compare-verdict="split">${MessageUtil.getMessage("stock.pick.compare.split")}</span>
			@endif
		</div>
		<ul class="space-y-0.5 text-xs">
			@for(var top : pick.tops())
				!{boolean same = top.candidate().symbol().equals(pick.symbol());}
				!{String valueText = "steady".equals(top.basis().param())
				    ? java.text.MessageFormat.format(StockPickBasisLabelUtil.comparePattern("steady"), top.candidate().payoutCutCount(), top.candidate().payoutCutPairs())
				    : java.text.MessageFormat.format(StockPickBasisLabelUtil.comparePattern(top.basis().param()), percentFormat.format(top.value()));}
				<li class="flex flex-wrap gap-x-2 ${same ? "font-medium text-base-content" : "text-base-content/70"}" data-compare-row="${top.basis().param()}" data-compare-symbol="${top.candidate().symbol()}">
					<span class="break-keep">${StockPickBasisLabelUtil.label(top.basis())}</span>
					<span class="min-w-0 wrap-anywhere">
						@if(same)
							${MessageUtil.getMessage("stock.pick.compare.this")}
						@else
							<a class="link link-hover" href="/stock/monthly-etf?view=contribution&basis=${top.basis().param()}" data-compare-link="${top.basis().param()}">${top.candidate().name() != null && !top.candidate().name().isBlank() ? top.candidate().name() : top.candidate().symbol()}(${top.candidate().symbol()})</a>
						@endif
						<span class="tabular-nums text-base-content/60"> · ${valueText}</span>
					</span>
				</li>
			@endfor
		</ul>
	</div>
@endif
