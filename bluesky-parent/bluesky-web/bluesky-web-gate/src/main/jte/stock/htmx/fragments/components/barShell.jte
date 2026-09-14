@import io.github.luversof.boot.context.support.MessageUtil

@param String title = null
@param String canvasId = "tradeMonthlyChart"

!{
	String resolvedTitle = title != null ? title : MessageUtil.getMessage("stock.trade.chart.title.monthly.trade.amount");
}

@template._components.ui.card(cssClass = "flex-[3] min-w-[280px] overflow-visible", body = @`
		<h2 class="font-bold text-sm mb-3" data-page-section>${resolvedTitle}</h2>
		<%-- 실현손익이 아래 칸으로 들어가면서 220px 로는 그 칸이 41px 밖에 안 남았다(실측 2026-09-14).
		     위 칸의 막대 높이를 지키면서 아래 칸에 눈금 세 개가 들어갈 만큼만 늘린다. --%>
		<div class="relative h-[280px]">
			<canvas id="${canvasId}" role="img" aria-label="${resolvedTitle}" class="amount-chart"></canvas>
		</div>
`)
