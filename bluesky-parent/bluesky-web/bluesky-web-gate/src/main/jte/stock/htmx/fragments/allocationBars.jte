@import net.luversof.web.gate.stock.util.StockFormatUtil
@import java.util.List
@import io.github.luversof.boot.context.support.MessageUtil
@import net.luversof.web.gate.stock.controller.StockSummaryHtmxController.AllocationBarRow
@import net.luversof.web.gate.stock.util.StockDetailLinkUtil

@param List<AllocationBarRow> allocationRows = java.util.List.of()
@param AllocationBarRow othersRow = null

!{
	String titleLabel = MessageUtil.getMessage("stock.dashboard.allocation");
	String assetStatusLabel = MessageUtil.getMessage("layout.menu.stock.analytics");
}
<%-- outerHTML 스왑 대상이므로 데이터 유무와 무관하게 항상 최상위 요소(카드)를 출력한다 --%>
@template._components.ui.card(cssClass = "h-full", body = @`
	<div class="flex justify-between items-center mb-2">
		<h2 class="card-title text-base m-0">${titleLabel}</h2>
		<a href="/stock/analytics" class="link link-hover text-xs">${assetStatusLabel} <span aria-hidden="true">→</span></a>
	</div>
	@if(!allocationRows.isEmpty())
	<%-- 막대마다 따로 반올림하면 표시값의 합이 100.0% 가 아니게 된다(실측 2026-09-11: 대시보드 비중 합 99.9%).
	     자산 현황 표·차트 범례와 같은 최대잔여법으로 한 번에 배분한다. 기타 줄도 같은 묶음이다. --%>
	!{
		java.util.List<java.math.BigDecimal> allocationWeights = new java.util.ArrayList<>();
		for (AllocationBarRow weightRow : allocationRows) {
			allocationWeights.add(java.math.BigDecimal.valueOf(weightRow.weightPercent()));
		}
		if (othersRow != null) {
			allocationWeights.add(java.math.BigDecimal.valueOf(othersRow.weightPercent()));
		}
		java.util.List<String> allocationShown = StockFormatUtil.balancedPct(allocationWeights, 1);
	}
	<%-- 막대 너비는 옆 글자와 같은 값을 써야 한다. 2026-09-12 까지 막대는 원값 반올림, 글자는 balancedPct(합을 100.0% 로
		 맞추는 최대잔여법)를 써서 한 행이 어긋났다 - 실측: RISE 200위클리커버드콜 이 막대 4.5% · 글자 4.6% 였다
		 (원값 4.543%, 6행 원값 합이 99.9 라 잔차가 가장 큰 이 행이 0.1 올라갔다). 눈에 띄는 폭은 아니지만 같은 줄이 두 수를 말한다.
		 글자 쪽을 원값으로 되돌리면 합 100.0% 성질이 깨지므로 막대가 글자를 따른다. --%>
	!{String[] allocationBarColors = {"rgba(99,102,241,0.8)", "rgba(34,197,94,0.8)", "rgba(251,191,36,0.8)", "rgba(239,68,68,0.8)", "rgba(59,130,246,0.8)"};}
	<div class="space-y-1">
		<%-- 아주 좁은 화면에서는 막대를 접는다. 종목명(96px) · 비중(44px) · 금액(86px)이 모두 고정폭이라
		     막대만 줄어드는데, 실측 2026-09-15(320px)에서 4px 까지 눌려 비중을 전혀 못 보여줬다.
		     값은 옆의 백분율이 그대로 말하므로 정보는 잃지 않는다. --%>
		@for(int allocationRowIndex = 0; allocationRowIndex < allocationRows.size(); allocationRowIndex++)
			!{AllocationBarRow row = allocationRows.get(allocationRowIndex);}
			!{String barColor = allocationBarColors[allocationRowIndex % allocationBarColors.length];}
			<div class="flex flex-wrap items-center justify-between gap-2 text-sm py-1.5 border-b border-base-200 last:border-0">
				@if(row.stockItemId() != null)
				<a href="${StockDetailLinkUtil.item(row.stockItemId().toString())}" class="link link-hover truncate w-24 sm:w-28 shrink-0" title="${row.stockItemName()}">${row.stockItemName()}</a>
				@else
				<span class="truncate w-24 sm:w-28 shrink-0" title="${row.stockItemName()}">${row.stockItemName()}</span>
				@endif
				<div class="flex-1 min-w-0 h-2 rounded-full bg-base-200 max-[400px]:hidden">
					<div class="h-2 rounded-full" style="width: ${allocationShown.get(allocationRowIndex).replace("%", "")}%; background: ${barColor}"></div>
				</div>
				<span class="text-xs text-base-content/60 tabular-nums w-11 text-right shrink-0">${allocationShown.get(allocationRowIndex)}</span>
				<span class="font-mono font-bold text-xs amount-value whitespace-nowrap text-right shrink-0" title="${StockFormatUtil.fullKrw(row.amount())}">${String.format("%,d", row.amount())}@template._components.ui.srExact(text = StockFormatUtil.fullKrw(row.amount()), visible = String.format("%,d", row.amount()))</span>
			</div>
		@endfor
		@if(othersRow != null)
			<div class="flex flex-wrap items-center justify-between gap-2 text-sm py-1.5 border-b border-base-200 last:border-0">
				<span class="truncate w-24 sm:w-28 shrink-0 text-base-content/60" title="${othersRow.stockItemName()}">${othersRow.stockItemName()}</span>
				<div class="flex-1 min-w-0 h-2 rounded-full bg-base-200 max-[400px]:hidden">
					<div class="h-2 rounded-full" style="width: ${allocationShown.get(allocationRows.size()).replace("%", "")}%; background: rgba(156,163,175,0.8)"></div>
				</div>
				<span class="text-xs text-base-content/60 tabular-nums w-11 text-right shrink-0">${allocationShown.get(allocationRows.size())}</span>
				<span class="font-mono font-bold text-xs amount-value whitespace-nowrap text-right shrink-0" title="${StockFormatUtil.fullKrw(othersRow.amount())}">${String.format("%,d", othersRow.amount())}@template._components.ui.srExact(text = StockFormatUtil.fullKrw(othersRow.amount()), visible = String.format("%,d", othersRow.amount()))</span>
			</div>
		@endif
	</div>
	@else
	@template._components.ui.emptyState(padding = "py-6")
	@endif
`)
