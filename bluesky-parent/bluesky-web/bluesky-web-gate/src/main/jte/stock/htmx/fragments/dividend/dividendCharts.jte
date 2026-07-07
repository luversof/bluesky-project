@import io.github.luversof.boot.context.support.MessageUtil

!{
    String weightAnalysisLabel = MessageUtil.getMessage("stock.chart.title.weight.analysis");
    String byStockLabel = MessageUtil.getMessage("stock.label.by.stock");
    String byAccountLabel = MessageUtil.getMessage("stock.label.by.account");
    String monthlyDividendsLabel = MessageUtil.getMessage("stock.dividend.chart.title.monthly.simple");
}

    <!-- Row 2: Charts -->
    <div class="flex flex-wrap gap-4 mb-4">

        <!-- Left: 비중 분석 -->
        @template._components.ui.card(cssClass = "flex-[2] min-w-[200px]", body = @`
                <div class="text-sm font-bold mb-3">${weightAnalysisLabel}</div>
                <div class="flex gap-3">
                    <div class="flex-shrink-0 w-[200px] h-[200px]">
                        <canvas id="donutChart" class="amount-chart"></canvas>
                    </div>
                    <div class="flex flex-col w-0 flex-1 h-[200px] overflow-hidden">
                        <div class="tabs tabs-box tabs-xs mb-1 w-fit flex-shrink-0">
                            <a class="tab tab-active" id="donutTabStock" data-dividend-donut-switch="stock">${byStockLabel}</a>
                            <a class="tab" id="donutTabAccount" data-dividend-donut-switch="account">${byAccountLabel}</a>
                        </div>
                        <div id="donutLegend" class="flex-1 min-h-0 overflow-y-auto text-[0.72rem]"></div>
                    </div>
                </div>
        `)

        <!-- Right: Monthly Bar Chart -->
        @template._components.ui.card(cssClass = "flex-[3] min-w-[280px] overflow-visible", body = @`
                <h3 class="font-bold text-sm mb-3">${monthlyDividendsLabel}</h3>
                <div class="relative h-[280px]">
                    <canvas id="monthlyBarChart" class="amount-chart"></canvas>
                </div>
        `)

    </div>

<script>
    // 이 프래그먼트는 htmx 스왑마다 재렌더될 수 있으므로 document 위임 + 전역 1회 가드로 바인딩한다.
    if (!window.__dividendDonutSwitchBound) {
        window.__dividendDonutSwitchBound = true;
        document.addEventListener('click', function(event) {
            var tab = event.target && event.target.closest ? event.target.closest('[data-dividend-donut-switch]') : null;
            if (!tab || typeof window.switchDonut !== 'function') {
                return;
            }
            window.switchDonut(tab.dataset.dividendDonutSwitch);
        });
    }
</script>
